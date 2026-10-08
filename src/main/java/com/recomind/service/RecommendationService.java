package com.recomind.service;

import com.recomind.dao.InteractionDao;
import com.recomind.dao.InteractionDaoImpl;
import com.recomind.dao.ItemDao;
import com.recomind.dao.ItemDaoImpl;
import com.recomind.dao.PreferenceDao;
import com.recomind.dao.PreferenceDaoImpl;
import com.recomind.dao.SettingsDao;
import com.recomind.dao.SettingsDaoImpl;
import com.recomind.dao.UserDao;
import com.recomind.dao.UserDaoImpl;
import com.recomind.exception.DAOException;
import com.recomind.model.Interaction;
import com.recomind.model.User;
import com.recomind.model.UserPreference;
import com.recomind.recommender.HybridRecommender;
import com.recomind.recommender.Recommendation;
import com.recomind.recommender.RecommendationData;
import com.recomind.recommender.RecommendationSettings;
import com.recomind.recommender.UserPrefs;
import com.recomind.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves recommendations from a thread-safe cache. A ScheduledExecutorService recomputes the cache
 * in the background every N minutes (N is an admin setting); an ExecutorService stores interactions
 * asynchronously and refreshes the affected user.
 */
public class RecommendationService {
    private static final Logger LOG = LoggerFactory.getLogger(RecommendationService.class);
    private static final int WRITER_STOP_SECONDS = 3;

    private final ItemDao itemDao = new ItemDaoImpl();
    private final UserDao userDao = new UserDaoImpl();
    private final PreferenceDao preferenceDao = new PreferenceDaoImpl();
    private final InteractionDao interactionDao = new InteractionDaoImpl();
    private final SettingsDao settingsDao = new SettingsDaoImpl();
    private final HybridRecommender hybrid = new HybridRecommender();

    private final ConcurrentHashMap<Long, List<Recommendation>> cache = new ConcurrentHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(daemon("reco-writer"));
    private volatile ScheduledExecutorService scheduler;
    private volatile RecommendationData data;
    private volatile RecommendationSettings settings = new RecommendationSettings();

    public List<Recommendation> recommend(long userId) {
        ensureLoaded();
        return cache.computeIfAbsent(userId, this::compute);
    }

    public RecommendationSettings getSettings() {
        ensureLoaded();
        return settings;
    }

    /** Reloads settings and data, then recomputes the cache for every active user. */
    public synchronized void refresh() {
        reload();
        cache.clear();
        for (User user : userDao.findAll()) {
            if (user.isActive()) {
                cache.put(user.getId(), compute(user.getId()));
            }
        }
        LOG.info("Recommendation cache refreshed for {} users", cache.size());
    }

    /** Stores the interaction on the writer thread, then recomputes that user's recommendations. */
    public Future<?> recordInteraction(Interaction interaction) {
        return writer.submit(() -> {
            interactionDao.insert(interaction);
            refreshUser(interaction.getUserId());
        });
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(daemon("reco-refresh"));
        try {
            refresh();
        } catch (RuntimeException e) {
            LOG.error("Initial recommendation refresh failed", e);
        }
        scheduleNext();
    }

    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        writer.shutdown();
        try {
            writer.awaitTermination(WRITER_STOP_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.info("Recommendation service stopped");
    }

    private void scheduleNext() {
        ScheduledExecutorService s = scheduler;
        if (s == null || s.isShutdown()) {
            return;
        }
        try {
            s.schedule(() -> {
                try {
                    refresh();
                } catch (RuntimeException e) {
                    LOG.error("Scheduled refresh failed", e);
                } finally {
                    scheduleNext();
                }
            }, settings.getRecomputeIntervalMinutes(), TimeUnit.MINUTES);
        } catch (RejectedExecutionException e) {
            LOG.debug("Scheduler already stopped");
        }
    }

    private void ensureLoaded() {
        if (data == null) {
            synchronized (this) {
                if (data == null) {
                    reload();
                }
            }
        }
    }

    private void reload() {
        this.settings = RecommendationSettings.fromMap(settingsDao.getAll());
        this.data = loadData();
    }

    private synchronized void refreshUser(long userId) {
        this.data = loadData();
        cache.put(userId, compute(userId));
    }

    private List<Recommendation> compute(long userId) {
        return hybrid.recommend(userId, data, settings);
    }

    private RecommendationData loadData() {
        Map<Long, UserPrefs> prefs = new HashMap<>();
        for (UserPreference p : preferenceDao.findAll()) {
            prefs.put(p.getUserId(), UserPrefs.fromJson(p.getPreferenceJson()));
        }
        return new RecommendationData(itemDao.findAll(), interactionDao.findAll(), prefs,
                loadCategoryNames(), Instant.now());
    }

    private Map<Long, String> loadCategoryNames() {
        Map<Long, String> names = new HashMap<>();
        try (Connection conn = DBUtil.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ID, NAME FROM CATEGORIES");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.put(rs.getLong(1), rs.getString(2));
            }
            return names;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    private static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }
}
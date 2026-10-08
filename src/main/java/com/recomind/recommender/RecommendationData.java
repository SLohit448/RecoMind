package com.recomind.recommender;

import com.recomind.model.Interaction;
import com.recomind.model.Item;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of everything the strategies need: items, interactions, preferences,
 * the user-item matrix and the term IDF values. Built once per refresh.
 */
public final class RecommendationData {
    private final Map<Long, Item> items = new LinkedHashMap<>();
    private final List<Interaction> interactions;
    private final Map<Long, UserPrefs> prefs;
    private final Map<Long, String> categoryNames;
    private final Instant now;
    private final Map<Long, Map<Long, Double>> userItems = new HashMap<>();
    private final Map<Long, Map<Long, Double>> itemUsers = new HashMap<>();
    private final Map<String, Double> idf = new HashMap<>();

    public RecommendationData(Collection<Item> itemList, Collection<Interaction> interactionList,
                              Map<Long, UserPrefs> prefs, Map<Long, String> categoryNames, Instant now) {
        List<Item> sorted = new ArrayList<>(itemList);
        sorted.sort(Comparator.comparingLong(Item::getId));
        for (Item item : sorted) {
            items.put(item.getId(), item);
        }
        this.interactions = new ArrayList<>(interactionList);
        this.prefs = prefs == null ? new HashMap<>() : prefs;
        this.categoryNames = categoryNames == null ? new HashMap<>() : categoryNames;
        this.now = now;
        buildMatrix();
        buildIdf();
    }

    private void buildMatrix() {
        for (Interaction i : interactions) {
            if (!items.containsKey(i.getItemId())) {
                continue;
            }
            userItems.computeIfAbsent(i.getUserId(), k -> new HashMap<>())
                    .merge(i.getItemId(), InteractionWeights.weightOf(i), Double::sum);
        }
        for (Map.Entry<Long, Map<Long, Double>> user : userItems.entrySet()) {
            for (Map.Entry<Long, Double> pair : user.getValue().entrySet()) {
                double w = Math.max(-InteractionWeights.MAX_PAIR_WEIGHT,
                        Math.min(InteractionWeights.MAX_PAIR_WEIGHT, pair.getValue()));
                pair.setValue(w);
                itemUsers.computeIfAbsent(pair.getKey(), k -> new HashMap<>()).put(user.getKey(), w);
            }
        }
    }

    private void buildIdf() {
        Map<String, Integer> docFrequency = new HashMap<>();
        for (Item item : items.values()) {
            for (String term : termsOf(item)) {
                docFrequency.merge(term, 1, Integer::sum);
            }
        }
        int n = items.size();
        for (Map.Entry<String, Integer> e : docFrequency.entrySet()) {
            idf.put(e.getKey(), Math.log((1.0 + n) / (1.0 + e.getValue())) + 1.0);
        }
    }

    /** Feature terms of an item: its category ("c:id") and each tag ("t:tag"). */
    public static Set<String> termsOf(Item item) {
        Set<String> terms = new LinkedHashSet<>();
        terms.add("c:" + item.getCategoryId());
        if (item.getTags() != null) {
            for (String raw : item.getTags().split(",")) {
                String tag = raw.trim().toLowerCase(Locale.ROOT);
                if (!tag.isEmpty()) {
                    terms.add("t:" + tag);
                }
            }
        }
        return terms;
    }

    public Map<Long, Item> items() { return Collections.unmodifiableMap(items); }
    public Item item(long id) { return items.get(id); }
    public List<Interaction> interactions() { return Collections.unmodifiableList(interactions); }
    public Instant now() { return now; }
    public Map<Long, Map<Long, Double>> itemUsers() { return itemUsers; }
    public double idf(String term) { return idf.getOrDefault(term, 1.0); }
    public String categoryName(long id) { return categoryNames.get(id); }

    public Map<Long, Double> userItems(long userId) {
        return userItems.getOrDefault(userId, Collections.emptyMap());
    }

    public UserPrefs prefs(long userId) {
        return prefs.getOrDefault(userId, new UserPrefs());
    }

    public boolean hasHistory(long userId) {
        return !userItems(userId).isEmpty();
    }
}
package com.recomind.recommender;

import com.recomind.model.Item;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Combines the three strategies with the admin-configured weights, removes already-seen items,
 * applies price preferences and keeps the top N using a PriorityQueue (min-heap).
 */
public class HybridRecommender {
    private final ContentBasedStrategy content = new ContentBasedStrategy();
    private final CollaborativeFilteringStrategy collaborative = new CollaborativeFilteringStrategy();
    private final PopularityStrategy popularity = new PopularityStrategy();

    public List<Recommendation> recommend(long userId, RecommendationData data, RecommendationSettings settings) {
        double wc = settings.getContentWeight();
        double wk = settings.getCollaborativeWeight();
        double wp = settings.getPopularityWeight();
        String algorithm = settings.getAlgorithm();
        if (RecommendationSettings.ALGO_CONTENT.equals(algorithm)) {
            wc = 1; wk = 0; wp = 0;
        } else if (RecommendationSettings.ALGO_COLLABORATIVE.equals(algorithm)) {
            wc = 0; wk = 1; wp = 0;
        } else if (RecommendationSettings.ALGO_POPULARITY.equals(algorithm)) {
            wc = 0; wk = 0; wp = 1;
        }
        // Cold start: no history and no stated preferences -> popularity only
        if (!data.hasHistory(userId) && data.prefs(userId).isEmpty()) {
            wc = 0; wk = 0; wp = 1;
        }

        Map<Long, Double> total = new HashMap<>();
        Map<Long, Recommendation> best = new HashMap<>();
        Map<Long, Double> bestContribution = new HashMap<>();
        accumulate(content, wc, userId, data, settings, total, best, bestContribution);
        accumulate(collaborative, wk, userId, data, settings, total, best, bestContribution);
        accumulate(popularity, wp, userId, data, settings, total, best, bestContribution);
        if (total.isEmpty() && wp <= 0) {
            accumulate(popularity, 1.0, userId, data, settings, total, best, bestContribution);
        }

        UserPrefs prefs = data.prefs(userId);
        Map<Long, Double> seen = data.userItems(userId);
        int n = settings.getRecommendationCount();
        Comparator<Recommendation> worstFirst = (a, b) -> {
            int c = Double.compare(a.getScore(), b.getScore());
            return c != 0 ? c : Long.compare(b.getItemId(), a.getItemId());
        };
        PriorityQueue<Recommendation> heap = new PriorityQueue<>(worstFirst);
        for (Map.Entry<Long, Double> e : total.entrySet()) {
            Item item = data.item(e.getKey());
            if (item == null || seen.containsKey(e.getKey())) {
                continue;
            }
            if (prefs.getMinPrice() != null && item.getPrice() < prefs.getMinPrice()) {
                continue;
            }
            if (prefs.getMaxPrice() != null && item.getPrice() > prefs.getMaxPrice()) {
                continue;
            }
            Recommendation source = best.get(e.getKey());
            heap.offer(new Recommendation(e.getKey(), e.getValue(), source.getAlgorithm(), source.getExplanation()));
            if (heap.size() > n) {
                heap.poll();
            }
        }
        List<Recommendation> result = new ArrayList<>(heap);
        result.sort(worstFirst.reversed());
        return result;
    }

    private void accumulate(RecommendationStrategy strategy, double weight, long userId,
                            RecommendationData data, RecommendationSettings settings,
                            Map<Long, Double> total, Map<Long, Recommendation> best,
                            Map<Long, Double> bestContribution) {
        if (weight <= 0) {
            return;
        }
        for (Recommendation r : strategy.score(userId, data, settings).values()) {
            double contribution = weight * r.getScore();
            total.merge(r.getItemId(), contribution, Double::sum);
            if (contribution > bestContribution.getOrDefault(r.getItemId(), 0.0)) {
                bestContribution.put(r.getItemId(), contribution);
                best.put(r.getItemId(), r);
            }
        }
    }
}
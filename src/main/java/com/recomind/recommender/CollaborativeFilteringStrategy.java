package com.recomind.recommender;

import com.recomind.model.Item;
import java.util.HashMap;
import java.util.Map;

/** Item-item collaborative filtering: cosine similarity on the user-item interaction matrix. */
public class CollaborativeFilteringStrategy implements RecommendationStrategy {
    private static final double LIKED_WEIGHT = 3.0;

    @Override
    public String name() {
        return "COLLABORATIVE";
    }

    @Override
    public Map<Long, Recommendation> score(long userId, RecommendationData data, RecommendationSettings settings) {
        Map<Long, Double> mine = data.userItems(userId);
        if (mine.isEmpty() || mine.size() < settings.getMinInteractions()) {
            return new HashMap<>();
        }
        Map<Long, Map<Long, Double>> itemUsers = data.itemUsers();
        Map<Long, Recommendation> raw = new HashMap<>();
        for (Item candidate : data.items().values()) {
            long j = candidate.getId();
            if (mine.containsKey(j)) {
                continue;
            }
            Map<Long, Double> candidateVector = itemUsers.get(j);
            if (candidateVector == null) {
                continue;
            }
            double total = 0;
            long bestItem = -1;
            double bestContribution = 0;
            for (Map.Entry<Long, Double> e : mine.entrySet()) {
                if (e.getValue() <= 0) {
                    continue;
                }
                Map<Long, Double> likedVector = itemUsers.get(e.getKey());
                if (likedVector == null) {
                    continue;
                }
                double similarity = CosineSimilarity.cosine(likedVector, candidateVector);
                if (similarity <= 0 || similarity < settings.getSimilarityThreshold()) {
                    continue;
                }
                double contribution = e.getValue() * similarity;
                total += contribution;
                if (contribution > bestContribution) {
                    bestContribution = contribution;
                    bestItem = e.getKey();
                }
            }
            if (total > 0) {
                raw.put(j, new Recommendation(j, total, name(), explain(bestItem, mine, data)));
            }
        }
        return Scores.normalizeByMax(raw);
    }

    private String explain(long baseItemId, Map<Long, Double> mine, RecommendationData data) {
        Item base = data.item(baseItemId);
        if (base == null) {
            return "People with similar taste liked this";
        }
        String verb = mine.getOrDefault(baseItemId, 0.0) >= LIKED_WEIGHT ? "liked" : "viewed";
        return "People who " + verb + " " + base.getTitle() + " also liked this";
    }
}
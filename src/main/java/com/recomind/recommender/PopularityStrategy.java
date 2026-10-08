package com.recomind.recommender;

import com.recomind.model.Interaction;
import java.util.HashMap;
import java.util.Map;

/** Time-decayed popularity (30-day half-life). Needs no user history, so it covers cold start. */
public class PopularityStrategy implements RecommendationStrategy {
    public static final double HALF_LIFE_DAYS = 30.0;
    private static final double MILLIS_PER_DAY = 86_400_000.0;

    @Override
    public String name() {
        return "POPULARITY";
    }

    @Override
    public Map<Long, Recommendation> score(long userId, RecommendationData data, RecommendationSettings settings) {
        Map<Long, Double> popularity = new HashMap<>();
        long nowMillis = data.now().toEpochMilli();
        for (Interaction i : data.interactions()) {
            double weight = InteractionWeights.weightOf(i);
            if (weight <= 0 || data.item(i.getItemId()) == null) {
                continue;
            }
            double ageDays = i.getCreatedAt() == null
                    ? 0.0
                    : Math.max(0.0, (nowMillis - i.getCreatedAt().getTime()) / MILLIS_PER_DAY);
            double decay = Math.pow(0.5, ageDays / HALF_LIFE_DAYS);
            popularity.merge(i.getItemId(), weight * decay, Double::sum);
        }
        Map<Long, Recommendation> raw = new HashMap<>();
        for (Map.Entry<Long, Double> e : popularity.entrySet()) {
            String category = data.categoryName(data.item(e.getKey()).getCategoryId());
            String why = category == null ? "Trending right now" : "Popular in " + category;
            raw.put(e.getKey(), new Recommendation(e.getKey(), e.getValue(), name(), why));
        }
        return Scores.normalizeByMax(raw);
    }
}
package com.recomind.recommender;

import java.util.Map;

/** Strategy pattern: every algorithm scores candidate items for a user with values in 0..1. */
public interface RecommendationStrategy {
    String name();

    Map<Long, Recommendation> score(long userId, RecommendationData data, RecommendationSettings settings);
}
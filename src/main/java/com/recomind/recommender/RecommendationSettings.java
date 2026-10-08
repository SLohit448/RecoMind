package com.recomind.recommender;

import com.recomind.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Admin-configurable algorithm parameters, loaded from and saved to SYSTEM_SETTINGS. */
public class RecommendationSettings {
    public static final String KEY_WEIGHT_CONTENT = "weight.content";
    public static final String KEY_WEIGHT_COLLABORATIVE = "weight.collaborative";
    public static final String KEY_WEIGHT_POPULARITY = "weight.popularity";
    public static final String KEY_COUNT = "recommendation.count";
    public static final String KEY_THRESHOLD = "similarity.threshold";
    public static final String KEY_MIN_INTERACTIONS = "min.interactions";
    public static final String KEY_INTERVAL = "recompute.interval.minutes";
    public static final String KEY_ALGORITHM = "algorithm.active";

    public static final String ALGO_HYBRID = "HYBRID";
    public static final String ALGO_CONTENT = "CONTENT";
    public static final String ALGO_COLLABORATIVE = "COLLABORATIVE";
    public static final String ALGO_POPULARITY = "POPULARITY";
    private static final Set<String> ALGORITHMS =
            Set.of(ALGO_HYBRID, ALGO_CONTENT, ALGO_COLLABORATIVE, ALGO_POPULARITY);
    private static final double WEIGHT_TOLERANCE = 0.001;

    private double contentWeight = 0.4;
    private double collaborativeWeight = 0.4;
    private double popularityWeight = 0.2;
    private int recommendationCount = 10;
    private double similarityThreshold = 0.1;
    private int minInteractions = 3;
    private int recomputeIntervalMinutes = 30;
    private String algorithm = ALGO_HYBRID;

    public static RecommendationSettings fromMap(Map<String, String> values) {
        RecommendationSettings s = new RecommendationSettings();
        s.contentWeight = parseDouble(values, KEY_WEIGHT_CONTENT, s.contentWeight);
        s.collaborativeWeight = parseDouble(values, KEY_WEIGHT_COLLABORATIVE, s.collaborativeWeight);
        s.popularityWeight = parseDouble(values, KEY_WEIGHT_POPULARITY, s.popularityWeight);
        s.recommendationCount = parseInt(values, KEY_COUNT, s.recommendationCount);
        s.similarityThreshold = parseDouble(values, KEY_THRESHOLD, s.similarityThreshold);
        s.minInteractions = parseInt(values, KEY_MIN_INTERACTIONS, s.minInteractions);
        s.recomputeIntervalMinutes = parseInt(values, KEY_INTERVAL, s.recomputeIntervalMinutes);
        String algo = values.get(KEY_ALGORITHM);
        if (algo != null && !algo.isBlank()) {
            s.algorithm = algo.trim().toUpperCase(Locale.ROOT);
        }
        return s;
    }

    public Map<String, String> toMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(KEY_WEIGHT_CONTENT, Double.toString(contentWeight));
        m.put(KEY_WEIGHT_COLLABORATIVE, Double.toString(collaborativeWeight));
        m.put(KEY_WEIGHT_POPULARITY, Double.toString(popularityWeight));
        m.put(KEY_COUNT, Integer.toString(recommendationCount));
        m.put(KEY_THRESHOLD, Double.toString(similarityThreshold));
        m.put(KEY_MIN_INTERACTIONS, Integer.toString(minInteractions));
        m.put(KEY_INTERVAL, Integer.toString(recomputeIntervalMinutes));
        m.put(KEY_ALGORITHM, algorithm);
        return m;
    }

    /** Throws ValidationException when any value is out of range or the weights do not sum to 1.0. */
    public void validate() {
        checkRange("Content weight", contentWeight, 0, 1);
        checkRange("Collaborative weight", collaborativeWeight, 0, 1);
        checkRange("Popularity weight", popularityWeight, 0, 1);
        double sum = contentWeight + collaborativeWeight + popularityWeight;
        if (Math.abs(sum - 1.0) > WEIGHT_TOLERANCE) {
            throw new ValidationException(
                    String.format(Locale.ROOT, "Algorithm weights must sum to 1.0 (currently %.3f)", sum));
        }
        checkRange("Number of recommendations", recommendationCount, 1, 50);
        checkRange("Similarity threshold", similarityThreshold, 0, 1);
        checkRange("Minimum interactions", minInteractions, 0, 100);
        checkRange("Recompute interval (minutes)", recomputeIntervalMinutes, 1, 1440);
        if (!ALGORITHMS.contains(algorithm)) {
            throw new ValidationException("Unknown algorithm: " + algorithm);
        }
    }

    private static void checkRange(String name, double value, double min, double max) {
        if (Double.isNaN(value) || value < min || value > max) {
            throw new ValidationException(name + " must be between " + min + " and " + max);
        }
    }

    private static double parseDouble(Map<String, String> m, String key, double fallback) {
        try {
            String v = m.get(key);
            return v == null ? fallback : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(Map<String, String> m, String key, int fallback) {
        try {
            String v = m.get(key);
            return v == null ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double getContentWeight() { return contentWeight; }
    public double getCollaborativeWeight() { return collaborativeWeight; }
    public double getPopularityWeight() { return popularityWeight; }
    public int getRecommendationCount() { return recommendationCount; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public int getMinInteractions() { return minInteractions; }
    public int getRecomputeIntervalMinutes() { return recomputeIntervalMinutes; }
    public String getAlgorithm() { return algorithm; }

    public RecommendationSettings setWeights(double content, double collaborative, double popularity) {
        this.contentWeight = content;
        this.collaborativeWeight = collaborative;
        this.popularityWeight = popularity;
        return this;
    }
    public RecommendationSettings setRecommendationCount(int v) { this.recommendationCount = v; return this; }
    public RecommendationSettings setSimilarityThreshold(double v) { this.similarityThreshold = v; return this; }
    public RecommendationSettings setMinInteractions(int v) { this.minInteractions = v; return this; }
    public RecommendationSettings setRecomputeIntervalMinutes(int v) { this.recomputeIntervalMinutes = v; return this; }
    public RecommendationSettings setAlgorithm(String v) { this.algorithm = v; return this; }
}
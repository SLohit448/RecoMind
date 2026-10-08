package com.recomind.recommender;

import java.util.Map;

/** Cosine similarity between two sparse vectors stored as maps. */
public final class CosineSimilarity {
    private CosineSimilarity() { }

    public static <K> double cosine(Map<K, Double> a, Map<K, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Map<K, Double> small = a.size() <= b.size() ? a : b;
        Map<K, Double> large = small == a ? b : a;
        double dot = 0.0;
        for (Map.Entry<K, Double> e : small.entrySet()) {
            Double other = large.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        double normA = norm(a);
        double normB = norm(b);
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (normA * normB);
    }

    private static <K> double norm(Map<K, Double> v) {
        double sum = 0.0;
        for (double x : v.values()) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }
}
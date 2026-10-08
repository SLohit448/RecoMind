package com.recomind.recommender;

import java.util.HashMap;
import java.util.Map;

/** Score helpers shared by the strategies. */
final class Scores {
    private Scores() { }

    /** Divides every score by the maximum so the best item gets 1.0. */
    static Map<Long, Recommendation> normalizeByMax(Map<Long, Recommendation> raw) {
        double max = raw.values().stream().mapToDouble(Recommendation::getScore).max().orElse(0.0);
        Map<Long, Recommendation> out = new HashMap<>();
        if (max <= 0.0) {
            return out;
        }
        for (Map.Entry<Long, Recommendation> e : raw.entrySet()) {
            out.put(e.getKey(), e.getValue().withScore(e.getValue().getScore() / max));
        }
        return out;
    }
}
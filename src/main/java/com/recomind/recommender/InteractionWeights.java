package com.recomind.recommender;

import com.recomind.model.Interaction;

/** Converts an interaction (view, click, like, rating...) to a signed preference weight. */
public final class InteractionWeights {
    public static final double VIEW = 1.0;
    public static final double CLICK = 2.0;
    public static final double LIKE = 4.0;
    public static final double PURCHASE = 5.0;
    public static final double DISLIKE = -4.0;
    /** Aggregated weight of one (user, item) pair is clamped to +/- this value. */
    public static final double MAX_PAIR_WEIGHT = 5.0;
    private static final double RATING_MIDPOINT = 2.5;
    private static final double RATING_SCALE = 2.0;

    private InteractionWeights() { }

    public static double weightOf(Interaction interaction) {
        String type = interaction.getType() == null ? "" : interaction.getType().toUpperCase();
        switch (type) {
            case "VIEW": return VIEW;
            case "CLICK": return CLICK;
            case "LIKE": return LIKE;
            case "PURCHASE": return PURCHASE;
            case "DISLIKE": return DISLIKE;
            case "RATING":
                return interaction.getRating() == null
                        ? VIEW
                        : (interaction.getRating() - RATING_MIDPOINT) * RATING_SCALE;
            default: return 0.0;
        }
    }
}
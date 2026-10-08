package com.recomind.recommender;

/** One recommended item with its score, the algorithm that contributed most and a human-readable reason. */
public final class Recommendation {
    private final long itemId;
    private final double score;
    private final String algorithm;
    private final String explanation;

    public Recommendation(long itemId, double score, String algorithm, String explanation) {
        this.itemId = itemId;
        this.score = score;
        this.algorithm = algorithm;
        this.explanation = explanation;
    }

    public long getItemId() { return itemId; }
    public double getScore() { return score; }
    public String getAlgorithm() { return algorithm; }
    public String getExplanation() { return explanation; }

    public Recommendation withScore(double newScore) {
        return new Recommendation(itemId, newScore, algorithm, explanation);
    }
}
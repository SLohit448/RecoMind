package com.recomind.recommender;

import com.recomind.model.Item;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** TF-IDF / tag-vector cosine similarity between the user profile and item features. */
public class ContentBasedStrategy implements RecommendationStrategy {
    public static final double PREFERENCE_BOOST = 3.0;
    private static final double LIKED_WEIGHT = 3.0;

    @Override
    public String name() {
        return "CONTENT";
    }

    @Override
    public Map<Long, Recommendation> score(long userId, RecommendationData data, RecommendationSettings settings) {
        Map<Long, Double> seen = data.userItems(userId);
        UserPrefs prefs = data.prefs(userId);

        Map<String, Double> profile = new HashMap<>();
        for (Map.Entry<Long, Double> e : seen.entrySet()) {
            if (e.getValue() <= 0) {
                continue;
            }
            Item item = data.item(e.getKey());
            if (item == null) {
                continue;
            }
            for (String term : RecommendationData.termsOf(item)) {
                profile.merge(term, e.getValue(), Double::sum);
            }
        }
        Set<String> prefTerms = new LinkedHashSet<>();
        for (Long category : prefs.getCategories()) {
            prefTerms.add("c:" + category);
        }
        for (String tag : prefs.getTags()) {
            if (tag != null && !tag.isBlank()) {
                prefTerms.add("t:" + tag.trim().toLowerCase(Locale.ROOT));
            }
        }
        for (String term : prefTerms) {
            profile.merge(term, PREFERENCE_BOOST, Double::sum);
        }
        if (profile.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Double> weightedProfile = new HashMap<>();
        for (Map.Entry<String, Double> e : profile.entrySet()) {
            weightedProfile.put(e.getKey(), e.getValue() * data.idf(e.getKey()));
        }

        Map<Long, Recommendation> raw = new HashMap<>();
        for (Item item : data.items().values()) {
            if (seen.containsKey(item.getId())) {
                continue;
            }
            Set<String> terms = RecommendationData.termsOf(item);
            Map<String, Double> vector = new HashMap<>();
            for (String term : terms) {
                vector.put(term, data.idf(term));
            }
            double similarity = CosineSimilarity.cosine(weightedProfile, vector);
            if (similarity > 0) {
                raw.put(item.getId(), new Recommendation(item.getId(), similarity, name(),
                        explain(terms, seen, prefTerms, data)));
            }
        }
        return Scores.normalizeByMax(raw);
    }

    private String explain(Set<String> terms, Map<Long, Double> seen, Set<String> prefTerms,
                           RecommendationData data) {
        Item best = null;
        double bestScore = 0;
        double bestWeight = 0;
        for (Map.Entry<Long, Double> e : seen.entrySet()) {
            if (e.getValue() <= 0) {
                continue;
            }
            Item other = data.item(e.getKey());
            if (other == null) {
                continue;
            }
            int shared = 0;
            for (String term : RecommendationData.termsOf(other)) {
                if (terms.contains(term)) {
                    shared++;
                }
            }
            double s = shared * e.getValue();
            if (s > bestScore) {
                bestScore = s;
                best = other;
                bestWeight = e.getValue();
            }
        }
        if (best != null) {
            return (bestWeight >= LIKED_WEIGHT ? "Because you liked " : "Because you viewed ") + best.getTitle();
        }
        for (String term : prefTerms) {
            if (terms.contains(term)) {
                if (term.startsWith("c:")) {
                    String name = data.categoryName(Long.parseLong(term.substring(2)));
                    return "Matches your preference for " + (name == null ? "this category" : name);
                }
                return "Matches your interest in " + term.substring(2);
            }
        }
        return "Similar to items you like";
    }
}
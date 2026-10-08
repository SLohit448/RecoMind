package com.recomind.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recomind.exception.ValidationException;
import com.recomind.model.Interaction;
import com.recomind.model.Item;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommenderTest {
    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");

    private static Item item(long id, long category, String tags, String title) {
        Item i = new Item();
        i.setId(id);
        i.setTitle(title);
        i.setCategoryId(category);
        i.setTags(tags);
        i.setPrice(10.0);
        return i;
    }

    private static Interaction ia(long user, long item, String type, Instant when) {
        Interaction i = new Interaction();
        i.setUserId(user);
        i.setItemId(item);
        i.setType(type);
        i.setCreatedAt(Timestamp.from(when));
        return i;
    }

    private static RecommendationData data(List<Interaction> interactions, Map<Long, UserPrefs> prefs) {
        List<Item> catalog = List.of(
                item(1, 1, "tech,gadget", "Phone"),
                item(2, 1, "tech,computer", "Laptop"),
                item(3, 2, "fiction,reading", "Novel"),
                item(4, 3, "cooking,food", "Cookbook"));
        Map<Long, String> categories = new HashMap<>();
        categories.put(1L, "Electronics");
        categories.put(2L, "Books");
        categories.put(3L, "Cooking");
        return new RecommendationData(catalog, interactions, prefs, categories, NOW);
    }

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        Map<String, Double> a = Map.of("x", 1.0, "y", 2.0);
        assertEquals(1.0, CosineSimilarity.cosine(a, a), 1e-9);
    }

    @Test
    void cosineOfOrthogonalOrEmptyVectorsIsZero() {
        assertEquals(0.0, CosineSimilarity.cosine(Map.of("x", 1.0), Map.of("y", 1.0)), 1e-9);
        assertEquals(0.0, CosineSimilarity.cosine(Map.<String, Double>of(), Map.of("y", 1.0)), 1e-9);
    }

    @Test
    void contentBasedRecommendsSimilarItems() {
        RecommendationData d = data(List.of(ia(1, 1, "LIKE", NOW)), Map.of());
        Map<Long, Recommendation> r = new ContentBasedStrategy().score(1, d, new RecommendationSettings());
        assertTrue(r.containsKey(2L));
        assertFalse(r.containsKey(3L));
        assertFalse(r.containsKey(4L));
        assertFalse(r.containsKey(1L));
        assertEquals(1.0, r.get(2L).getScore(), 1e-9);
        assertTrue(r.get(2L).getExplanation().startsWith("Because you liked Phone"));
    }

    @Test
    void contentBasedUsesPreferencesForUserWithoutHistory() {
        UserPrefs prefs = UserPrefs.fromJson("{\"categories\":[3]}");
        RecommendationData d = data(List.of(), Map.of(9L, prefs));
        Map<Long, Recommendation> r = new ContentBasedStrategy().score(9, d, new RecommendationSettings());
        assertEquals(Set.of(4L), r.keySet());
        assertTrue(r.get(4L).getExplanation().contains("Cooking"));
    }

    @Test
    void collaborativeRecommendsWhatSimilarUsersLiked() {
        List<Interaction> list = List.of(
                ia(1, 1, "LIKE", NOW),
                ia(2, 1, "LIKE", NOW), ia(2, 2, "LIKE", NOW),
                ia(3, 3, "LIKE", NOW));
        RecommendationSettings s = new RecommendationSettings().setMinInteractions(1);
        Map<Long, Recommendation> r = new CollaborativeFilteringStrategy().score(1, data(list, Map.of()), s);
        assertEquals(Set.of(2L), r.keySet());
        assertTrue(r.get(2L).getExplanation().startsWith("People who liked Phone"));
    }

    @Test
    void collaborativeNeedsMinimumInteractions() {
        List<Interaction> list = List.of(ia(1, 1, "LIKE", NOW), ia(2, 1, "LIKE", NOW), ia(2, 2, "LIKE", NOW));
        RecommendationSettings s = new RecommendationSettings().setMinInteractions(3);
        assertTrue(new CollaborativeFilteringStrategy().score(1, data(list, Map.of()), s).isEmpty());
    }

    @Test
    void popularityDecaysWithAge() {
        Instant old = NOW.minus(Duration.ofDays(60));
        List<Interaction> list = List.of(
                ia(1, 1, "VIEW", NOW),
                ia(2, 2, "VIEW", old), ia(3, 2, "VIEW", old));
        Map<Long, Recommendation> r = new PopularityStrategy().score(9, data(list, Map.of()), new RecommendationSettings());
        assertEquals(1.0, r.get(1L).getScore(), 1e-9);
        assertEquals(0.5, r.get(2L).getScore(), 1e-6);
        assertEquals("Popular in Electronics", r.get(1L).getExplanation());
    }

    @Test
    void coldStartUserGetsPopularItems() {
        List<Interaction> list = List.of(
                ia(2, 3, "VIEW", NOW), ia(2, 3, "CLICK", NOW),
                ia(3, 3, "LIKE", NOW), ia(3, 4, "VIEW", NOW));
        List<Recommendation> r = new HybridRecommender().recommend(99, data(list, Map.of()), new RecommendationSettings());
        assertFalse(r.isEmpty());
        assertEquals(3L, r.get(0).getItemId());
        assertTrue(r.stream().allMatch(x -> "POPULARITY".equals(x.getAlgorithm())));
    }

    @Test
    void hybridSkipsSeenItemsAndKeepsTopN() {
        List<Interaction> list = List.of(
                ia(1, 1, "LIKE", NOW),
                ia(2, 3, "VIEW", NOW), ia(3, 3, "VIEW", NOW), ia(2, 4, "VIEW", NOW));
        RecommendationData d = data(list, Map.of());
        List<Recommendation> all = new HybridRecommender().recommend(1, d, new RecommendationSettings());
        assertEquals(3, all.size());
        assertTrue(all.stream().noneMatch(x -> x.getItemId() == 1L));
        assertEquals(2L, all.get(0).getItemId());

        List<Recommendation> top2 = new HybridRecommender()
                .recommend(1, d, new RecommendationSettings().setRecommendationCount(2));
        assertEquals(2, top2.size());
        assertEquals(2L, top2.get(0).getItemId());
    }

    @Test
    void hybridWeightsDecideWhichAlgorithmWins() {
        List<Interaction> list = List.of(
                ia(1, 1, "LIKE", NOW),
                ia(2, 3, "VIEW", NOW), ia(2, 3, "CLICK", NOW),
                ia(3, 3, "VIEW", NOW), ia(3, 3, "CLICK", NOW));
        RecommendationData d = data(list, Map.of());
        HybridRecommender h = new HybridRecommender();

        List<Recommendation> contentOnly = h.recommend(1, d, new RecommendationSettings().setWeights(1, 0, 0));
        assertEquals(List.of(2L), contentOnly.stream().map(Recommendation::getItemId).toList());

        List<Recommendation> popularityOnly = h.recommend(1, d, new RecommendationSettings().setWeights(0, 0, 1));
        assertEquals(List.of(3L), popularityOnly.stream().map(Recommendation::getItemId).toList());

        List<Recommendation> mixed = h.recommend(1, d, new RecommendationSettings().setWeights(0.5, 0, 0.5));
        assertEquals(Set.of(2L, 3L), Set.copyOf(mixed.stream().map(Recommendation::getItemId).toList()));

        List<Recommendation> forced = h.recommend(1, d, new RecommendationSettings().setAlgorithm("POPULARITY"));
        assertEquals(List.of(3L), forced.stream().map(Recommendation::getItemId).toList());
    }

    @Test
    void settingsValidationRejectsWeightsThatDoNotSumToOne() {
        RecommendationSettings s = new RecommendationSettings().setWeights(0.5, 0.5, 0.5);
        ValidationException e = assertThrows(ValidationException.class, s::validate);
        assertTrue(e.getMessage().contains("sum"));
    }

    @Test
    void settingsValidationRejectsBadCount() {
        assertThrows(ValidationException.class,
                () -> new RecommendationSettings().setRecommendationCount(0).validate());
    }

    @Test
    void defaultSettingsAreValidAndSurviveARoundTrip() {
        RecommendationSettings defaults = new RecommendationSettings();
        defaults.validate();
        RecommendationSettings back = RecommendationSettings.fromMap(defaults.toMap());
        assertEquals(defaults.getContentWeight(), back.getContentWeight(), 1e-9);
        assertEquals(defaults.getRecommendationCount(), back.getRecommendationCount());
        assertEquals(defaults.getAlgorithm(), back.getAlgorithm());
    }

    @Test
    void invalidPreferenceJsonGivesEmptyPreferences() {
        assertTrue(UserPrefs.fromJson("not json").isEmpty());
        assertTrue(UserPrefs.fromJson(null).isEmpty());
    }
}
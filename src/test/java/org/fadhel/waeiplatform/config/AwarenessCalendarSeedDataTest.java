package org.fadhel.waeiplatform.config;

import org.fadhel.waeiplatform.service.AwarenessDateRule;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwarenessCalendarSeedDataTest {

    private static final Set<String> CATEGORIES = Set.of(
            "FIRST_AID", "EMERGENCY", "CHRONIC", "MENTAL_HEALTH", "PUBLIC_HEALTH", "MATERNAL_CHILD");

    @Test
    void seedDataIsValid() {
        Set<String> slugs = new HashSet<>();
        Set<String> titles = new HashSet<>();

        for (AwarenessCalendarSeedData.Seed seed : AwarenessCalendarSeedData.SEEDS) {
            assertTrue(slugs.add(seed.slug()), "Duplicate slug: " + seed.slug());
            assertTrue(titles.add(seed.title()), "Duplicate Arabic title: " + seed.title());
            assertNotNull(seed.title(), "Missing Arabic title: " + seed.slug());
            assertNotNull(seed.titleEn(), "Missing English title: " + seed.slug());
            assertNotNull(seed.description(), "Missing Arabic description: " + seed.slug());
            assertNotNull(seed.descriptionEn(), "Missing English description: " + seed.slug());
            assertTrue(seed.title().length() <= 100, "Arabic title is too long: " + seed.slug());
            assertTrue(seed.titleEn().length() <= 100, "English title is too long: " + seed.slug());
            assertTrue(CATEGORIES.contains(seed.category()), "Unknown category: " + seed.category());
            assertTrue(AwarenessDateRule.isValid(seed.dateRule()), "Invalid date rule: " + seed.dateRule());
            assertTrue(seed.sourceUrl().startsWith("https://"), "Source must use https: " + seed.slug());
        }

        assertTrue(AwarenessCalendarSeedData.SEEDS.size() >= 40);
    }
}

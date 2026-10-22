package org.fadhel.waeiplatform.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwarenessDateRuleTest {

    @Test
    void resolvesFixedDates() {
        assertEquals(LocalDate.of(2026, 11, 14), AwarenessDateRule.resolve("FIXED:11-14", 2026));
        assertEquals(LocalDate.of(2026, 12, 1), AwarenessDateRule.resolve("FIXED:12-01", 2026));
    }

    @Test
    void resolvesNthWeekdays() {
        assertEquals(LocalDate.of(2026, 9, 12), AwarenessDateRule.resolve("NTH_WEEKDAY:09:SATURDAY:2", 2026));
        assertEquals(LocalDate.of(2026, 5, 5), AwarenessDateRule.resolve("NTH_WEEKDAY:05:TUESDAY:1", 2026));
        assertEquals(LocalDate.of(2026, 3, 12), AwarenessDateRule.resolve("NTH_WEEKDAY:03:THURSDAY:2", 2026));
        assertEquals(LocalDate.of(2026, 11, 18), AwarenessDateRule.resolve("NTH_WEEKDAY:11:WEDNESDAY:3", 2026));
        assertEquals(LocalDate.of(2026, 11, 15), AwarenessDateRule.resolve("NTH_WEEKDAY:11:SUNDAY:3", 2026));
    }

    @Test
    void resolvesLastWeekday() {
        assertEquals(LocalDate.of(2026, 1, 25), AwarenessDateRule.resolve("LAST_WEEKDAY:01:SUNDAY", 2026));
    }

    @Test
    void returnsNullWhenNthWeekdayDoesNotExist() {
        assertNull(AwarenessDateRule.resolve("NTH_WEEKDAY:02:MONDAY:5", 2026));
    }

    @Test
    void nextDateRollsToTheFollowingYear() {
        LocalDate today = LocalDate.of(2026, 2, 1);
        assertEquals(LocalDate.of(2027, 1, 30), AwarenessDateRule.nextDate("FIXED:01-30", today));
        assertEquals(LocalDate.of(2026, 11, 14), AwarenessDateRule.nextDate("FIXED:11-14", today));
    }

    @Test
    void nextDateFindsTheNextLeapDay() {
        LocalDate today = LocalDate.of(2026, 3, 1);
        assertEquals(LocalDate.of(2028, 2, 29), AwarenessDateRule.nextDate("FIXED:02-29", today));
    }

    @Test
    void rejectsInvalidRules() {
        assertFalse(AwarenessDateRule.isValid("FIXED:13-01"));
        assertFalse(AwarenessDateRule.isValid("FIXED:04-31"));
        assertFalse(AwarenessDateRule.isValid("NTH_WEEKDAY:09:FUNDAY:2"));
        assertFalse(AwarenessDateRule.isValid("NTH_WEEKDAY:09:SATURDAY:6"));
        assertFalse(AwarenessDateRule.isValid("LAST_WEEKDAY:01"));
        assertFalse(AwarenessDateRule.isValid(""));
        assertTrue(AwarenessDateRule.isValid("FIXED:02-29"));
    }
}

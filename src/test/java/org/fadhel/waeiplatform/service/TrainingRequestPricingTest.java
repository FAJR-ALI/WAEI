package org.fadhel.waeiplatform.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainingRequestPricingTest {

    @Test
    void totalIsDailyPriceTimesDays() {
        assertEquals(new BigDecimal("376.50"),
                TrainingRequestPricing.total(new BigDecimal("125.50"), 3));
    }

    @Test
    void endUsesLastDayPlusDailyHours() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 9, 0);

        assertEquals(LocalDateTime.of(2026, 11, 3, 11, 0),
                TrainingRequestPricing.end(start, 3, new BigDecimal("2")));
    }

    @Test
    void supportsFractionalDailyHours() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 9, 0);

        assertEquals(LocalDateTime.of(2026, 11, 1, 9, 30),
                TrainingRequestPricing.end(start, 1, new BigDecimal("0.5")));
    }
}

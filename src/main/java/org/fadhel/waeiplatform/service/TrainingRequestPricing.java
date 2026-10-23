package org.fadhel.waeiplatform.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public final class TrainingRequestPricing {

    private TrainingRequestPricing() {
    }

    public static BigDecimal total(BigDecimal dailyPrice, int days) {
        return dailyPrice.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    public static LocalDateTime end(LocalDateTime start, int days, BigDecimal hoursPerDay) {
        long minutes = hoursPerDay.multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return start.plusDays(days - 1L).plusMinutes(minutes);
    }
}

package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProviderWeeklySummaryResponse {

    private String language;

    private Integer days;

    private LocalDateTime generatedAt;

    private LocalDateTime from;

    private LocalDateTime to;

    private String headline;

    private List<String> bullets;

    private String nextAction;

    private Boolean aiGenerated;

    private Metrics metrics;

    @Data
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Metrics {

        private Integer programCount;

        private Integer activeProgramCount;

        private Integer newRequestCount;

        private Integer pendingRequestCount;

        private Integer awaitingPaymentCount;

        private Integer upcomingSessionCount;

        private Integer inProgressSessionCount;

        private Integer completedSessionCount;

        private Integer cancelledSessionCount;

        private Integer onlineMeetingMissingCount;

        private BigDecimal paidProviderRevenue;
    }
}
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
public class BeneficiaryWeeklySummaryResponse {

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

        private Integer requestCount;

        private Integer newRequestCount;

        private Integer pendingProviderResponseCount;

        private Integer awaitingPaymentCount;

        private Integer activeTrainingCount;

        private Integer upcomingTrainingCount;

        private Integer inProgressTrainingCount;

        private Integer completedTrainingCount;

        private Integer cancelledTrainingCount;

        private Integer onlineTrainingMissingJoinLinkCount;

        private BigDecimal paidTrainingAmount;
    }
}
package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiProviderWeeklySummary;
import org.fadhel.waeiplatform.dto.out.AdminWeeklySummaryResponse;
import org.fadhel.waeiplatform.dto.out.OrganizationDTOOut;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.TrainingProgramResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWeeklySummaryService {

    private final CurrentUser currentUser;
    private final OrganizationService organizationService;
    private final TrainingProgramService trainingProgramService;
    private final TrainingRequestService trainingRequestService;
    private final PaymentService paymentService;
    private final GeminiService geminiService;

    public AdminWeeklySummaryResponse getWeeklySummary(String lang, int days) {
        if (days < 1 || days > 30) {
            throw new ApiException("Weekly summary period must be between 1 and 30 days");
        }

        requirePlatformAdmin();

        String language = normalizeLanguage(lang);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(days);
        LocalDateTime to = now.plusDays(days);

        AdminWeeklySummaryResponse.Metrics metrics = computeMetrics(
                organizationService.getAllOrganizations(),
                trainingProgramService.getAllPrograms(),
                trainingRequestService.getAllRequests(),
                paymentService.getAllPayments(),
                now,
                from,
                to);
        AdminWeeklySummaryResponse fallback = buildFallback(language, days, now, from, to, metrics);

        try {
            AiProviderWeeklySummary generated = geminiService.summarizeAdminWeek(buildFacts(metrics, days), language);
            AdminWeeklySummaryResponse result = applyGenerated(fallback, generated);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Gemini admin weekly summary failed, using deterministic fallback: {}", e.getMessage());
        }

        return fallback;
    }

    private String normalizeLanguage(String lang) {
        return "en".equalsIgnoreCase(lang) ? "en" : "ar";
    }

    private void requirePlatformAdmin() {
        if (!currentUser.isPlatformAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the platform administrator can use the weekly summary assistant");
        }
    }

    private AdminWeeklySummaryResponse.Metrics computeMetrics(
            List<OrganizationDTOOut> organizations,
            List<TrainingProgramResponse> programs,
            List<TrainingRequestResponse> requests,
            List<PaymentResponse> payments,
            LocalDateTime now,
            LocalDateTime from,
            LocalDateTime to) {
        int organizationCount = size(organizations);
        int pendingOrganizationReviewCount = countOrganizationStatus(organizations, OrganizationVerificationStatus.PENDING);
        int verifiedOrganizationCount = countOrganizationStatus(organizations, OrganizationVerificationStatus.VERIFIED);
        int rejectedOrganizationCount = countOrganizationStatus(organizations, OrganizationVerificationStatus.REJECTED);
        int newOrganizationApplicationCount = 0;
        int newVerifiedOrganizationCount = 0;

        for (OrganizationDTOOut organization : organizations) {
            if (withinPast(organization.getSubmittedAt(), from, now)) {
                newOrganizationApplicationCount++;
            }
            if (organization.getVerificationStatus() == OrganizationVerificationStatus.VERIFIED
                    && withinPast(organization.getVerifiedAt(), from, now)) {
                newVerifiedOrganizationCount++;
            }
        }

        int programCount = size(programs);
        int activeProgramCount = 0;
        int programsMissingDurationCount = 0;
        for (TrainingProgramResponse program : programs) {
            if (Boolean.TRUE.equals(program.getIsActive())) {
                activeProgramCount++;
            }
            if (isMissingDuration(program)) {
                programsMissingDurationCount++;
            }
        }

        int requestCount = size(requests);
        int newRequestCount = 0;
        int pendingProviderResponseCount = 0;
        int awaitingPaymentCount = 0;
        int activeTrainingCount = 0;
        int upcomingTrainingCount = 0;
        int completedTrainingCount = 0;
        int cancelledTrainingCount = 0;
        int onlineTrainingMissingJoinLinkCount = 0;

        for (TrainingRequestResponse request : requests) {
            String status = request.getStatus();
            if (withinPast(request.getRequestedAt(), from, now)) {
                newRequestCount++;
            }
            if (TrainingRequestStatus.PENDING.name().equals(status)) {
                pendingProviderResponseCount++;
            }
            if (TrainingRequestStatus.ACCEPTED.name().equals(status)
                    || TrainingRequestStatus.PAYMENT_PENDING.name().equals(status)) {
                awaitingPaymentCount++;
            }
            if (TrainingRequestStatus.CONFIRMED.name().equals(status)
                    || TrainingRequestStatus.IN_PROGRESS.name().equals(status)) {
                activeTrainingCount++;
            }
            if (TrainingRequestStatus.COMPLETED.name().equals(status)
                    && withinPast(request.getCompletedAt(), from, now)) {
                completedTrainingCount++;
            }
            if (TrainingRequestStatus.CANCELLED.name().equals(status)
                    && withinPast(request.getCancelledAt(), from, now)) {
                cancelledTrainingCount++;
            }

            boolean upcoming = isUpcomingTraining(request, now, to);
            if (upcoming) {
                upcomingTrainingCount++;
            }
            if (upcoming
                    && "ONLINE".equals(request.getTrainingProgramDeliveryMode())
                    && (TrainingRequestStatus.CONFIRMED.name().equals(status)
                    || TrainingRequestStatus.IN_PROGRESS.name().equals(status))
                    && hasNoJoinUrl(request)) {
                onlineTrainingMissingJoinLinkCount++;
            }
        }

        int paidPaymentCount = 0;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal platformCommissionAmount = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        int pendingPaymentCount = 0;
        BigDecimal pendingPaymentAmount = BigDecimal.ZERO;

        for (PaymentResponse payment : payments) {
            String status = payment.getStatus();
            if (PaymentStatus.PAID.name().equals(status) && withinPast(payment.getPaidAt(), from, now)) {
                paidPaymentCount++;
                paidAmount = paidAmount.add(value(payment.getAmount()));
                platformCommissionAmount = platformCommissionAmount.add(value(payment.getCommissionAmount()));
            }
            if (PaymentStatus.PENDING.name().equals(status)) {
                pendingPaymentCount++;
                pendingPaymentAmount = pendingPaymentAmount.add(value(payment.getAmount()));
            }
            if (PaymentStatus.REFUNDED.name().equals(status) && withinPast(payment.getRefundedAt(), from, now)) {
                refundedAmount = refundedAmount.add(refundedPaymentAmount(payment));
            }
        }

        return AdminWeeklySummaryResponse.Metrics.builder()
                .organizationCount(organizationCount)
                .pendingOrganizationReviewCount(pendingOrganizationReviewCount)
                .newOrganizationApplicationCount(newOrganizationApplicationCount)
                .verifiedOrganizationCount(verifiedOrganizationCount)
                .newVerifiedOrganizationCount(newVerifiedOrganizationCount)
                .rejectedOrganizationCount(rejectedOrganizationCount)
                .programCount(programCount)
                .activeProgramCount(activeProgramCount)
                .inactiveProgramCount(programCount - activeProgramCount)
                .programsMissingDurationCount(programsMissingDurationCount)
                .requestCount(requestCount)
                .newRequestCount(newRequestCount)
                .pendingProviderResponseCount(pendingProviderResponseCount)
                .awaitingPaymentCount(awaitingPaymentCount)
                .activeTrainingCount(activeTrainingCount)
                .upcomingTrainingCount(upcomingTrainingCount)
                .completedTrainingCount(completedTrainingCount)
                .cancelledTrainingCount(cancelledTrainingCount)
                .onlineTrainingMissingJoinLinkCount(onlineTrainingMissingJoinLinkCount)
                .paidPaymentCount(paidPaymentCount)
                .paidAmount(paidAmount.setScale(2, RoundingMode.HALF_UP))
                .platformCommissionAmount(platformCommissionAmount.setScale(2, RoundingMode.HALF_UP))
                .pendingPaymentCount(pendingPaymentCount)
                .pendingPaymentAmount(pendingPaymentAmount.setScale(2, RoundingMode.HALF_UP))
                .refundedAmount(refundedAmount.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private int size(List<?> items) {
        return items == null ? 0 : items.size();
    }

    private int countOrganizationStatus(List<OrganizationDTOOut> organizations, OrganizationVerificationStatus status) {
        if (organizations == null) {
            return 0;
        }
        return (int) organizations.stream().filter(organization -> organization.getVerificationStatus() == status).count();
    }

    private boolean isMissingDuration(TrainingProgramResponse program) {
        return program.getHoursPerDay() == null || program.getMaxDays() == null || program.getMaxDays() <= 0;
    }

    private boolean withinPast(LocalDateTime value, LocalDateTime from, LocalDateTime now) {
        return value != null && !value.isBefore(from) && !value.isAfter(now);
    }

    private boolean isUpcomingTraining(TrainingRequestResponse request, LocalDateTime now, LocalDateTime to) {
        String status = request.getStatus();
        return request.getScheduledStart() != null
                && !request.getScheduledStart().isBefore(now)
                && !request.getScheduledStart().isAfter(to)
                && (TrainingRequestStatus.ACCEPTED.name().equals(status)
                || TrainingRequestStatus.PAYMENT_PENDING.name().equals(status)
                || TrainingRequestStatus.CONFIRMED.name().equals(status)
                || TrainingRequestStatus.IN_PROGRESS.name().equals(status));
    }

    private boolean hasNoJoinUrl(TrainingRequestResponse request) {
        return request.getMeetingJoinUrl() == null || request.getMeetingJoinUrl().isBlank();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal refundedPaymentAmount(PaymentResponse payment) {
        if (payment.getRefundedAmount() != null) {
            return payment.getRefundedAmount();
        }
        return value(payment.getAmount());
    }

    private String buildFacts(AdminWeeklySummaryResponse.Metrics metrics, int days) {
        return """
                audience=platformAdmin
                periodDays=%d
                organizationCount=%d
                pendingOrganizationReviewCount=%d
                newOrganizationApplicationCount=%d
                verifiedOrganizationCount=%d
                newVerifiedOrganizationCount=%d
                rejectedOrganizationCount=%d
                programCount=%d
                activeProgramCount=%d
                inactiveProgramCount=%d
                programsMissingDurationCount=%d
                trainingRequestCount=%d
                newTrainingRequestCount=%d
                requestsWaitingProviderResponseCount=%d
                requestsAwaitingPaymentOrConfirmationCount=%d
                activeTrainingCount=%d
                upcomingTrainingCount=%d
                completedTrainingCount=%d
                cancelledTrainingCount=%d
                onlineTrainingMissingJoinLinkCount=%d
                paidPaymentCount=%d
                paidAmountSAR=%s
                platformCommissionAmountSAR=%s
                pendingPaymentCount=%d
                pendingPaymentAmountSAR=%s
                refundedAmountSAR=%s
                """
                .formatted(
                        days,
                        value(metrics.getOrganizationCount()),
                        value(metrics.getPendingOrganizationReviewCount()),
                        value(metrics.getNewOrganizationApplicationCount()),
                        value(metrics.getVerifiedOrganizationCount()),
                        value(metrics.getNewVerifiedOrganizationCount()),
                        value(metrics.getRejectedOrganizationCount()),
                        value(metrics.getProgramCount()),
                        value(metrics.getActiveProgramCount()),
                        value(metrics.getInactiveProgramCount()),
                        value(metrics.getProgramsMissingDurationCount()),
                        value(metrics.getRequestCount()),
                        value(metrics.getNewRequestCount()),
                        value(metrics.getPendingProviderResponseCount()),
                        value(metrics.getAwaitingPaymentCount()),
                        value(metrics.getActiveTrainingCount()),
                        value(metrics.getUpcomingTrainingCount()),
                        value(metrics.getCompletedTrainingCount()),
                        value(metrics.getCancelledTrainingCount()),
                        value(metrics.getOnlineTrainingMissingJoinLinkCount()),
                        value(metrics.getPaidPaymentCount()),
                        money(metrics.getPaidAmount()),
                        money(metrics.getPlatformCommissionAmount()),
                        value(metrics.getPendingPaymentCount()),
                        money(metrics.getPendingPaymentAmount()),
                        money(metrics.getRefundedAmount()));
    }

    private AdminWeeklySummaryResponse buildFallback(
            String language,
            int days,
            LocalDateTime now,
            LocalDateTime from,
            LocalDateTime to,
            AdminWeeklySummaryResponse.Metrics metrics) {
        return AdminWeeklySummaryResponse.builder()
                .language(language)
                .days(days)
                .generatedAt(now)
                .from(from)
                .to(to)
                .headline(buildFallbackHeadline(language, days, metrics))
                .bullets(buildFallbackBullets(language, days, metrics))
                .nextAction(buildFallbackNextAction(language, metrics))
                .aiGenerated(false)
                .metrics(metrics)
                .build();
    }

    private AdminWeeklySummaryResponse applyGenerated(
            AdminWeeklySummaryResponse fallback,
            AiProviderWeeklySummary generated) {
        if (generated == null) {
            return fallback;
        }

        List<String> bullets = cleanBullets(generated.getBullets(), fallback.getBullets());
        if (bullets.isEmpty()) {
            return fallback;
        }

        String headline = cleanText(generated.getHeadline(), 180);
        if (headline.isBlank()) {
            return fallback;
        }

        String nextAction = cleanText(generated.getNextAction(), 320);
        if (nextAction.isBlank()) {
            nextAction = fallback.getNextAction();
        }

        return fallback.toBuilder()
                .headline(headline)
                .bullets(bullets)
                .nextAction(nextAction)
                .aiGenerated(true)
                .build();
    }

    private List<String> cleanBullets(List<String> raw, List<String> fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }

        List<String> cleaned = raw.stream()
                .map(value -> cleanText(value, 320))
                .filter(value -> !value.isBlank())
                .limit(4)
                .toList();

        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private String cleanText(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String cleaned = value.replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim();
    }

    private String buildFallbackHeadline(String language, int days, AdminWeeklySummaryResponse.Metrics metrics) {
        if (language.equals("ar")) {
            if (value(metrics.getPendingOrganizationReviewCount()) > 0) {
                return "هناك " + value(metrics.getPendingOrganizationReviewCount()) + " طلبات جهات بانتظار المراجعة.";
            }
            if (value(metrics.getPendingProviderResponseCount()) > 0) {
                return "هناك " + value(metrics.getPendingProviderResponseCount()) + " طلبات تدريب بانتظار رد مقدم الخدمة.";
            }
            if (value(metrics.getOnlineTrainingMissingJoinLinkCount()) > 0) {
                return "هناك " + value(metrics.getOnlineTrainingMissingJoinLinkCount()) + " تدريبات أونلاين قادمة بدون رابط انضمام.";
            }
            if (value(metrics.getUpcomingTrainingCount()) > 0) {
                return "هناك " + value(metrics.getUpcomingTrainingCount()) + " تدريبات مجدولة خلال " + days + " يوم قادم.";
            }
            if (hasAmount(metrics.getPlatformCommissionAmount())) {
                return "عمولة المنصة خلال آخر " + days + " يوم: " + money(metrics.getPlatformCommissionAmount()) + " ر.س.";
            }
            return "نشاط المنصة مستقر خلال آخر " + days + " يوم.";
        }

        if (value(metrics.getPendingOrganizationReviewCount()) > 0) {
            return "There are " + value(metrics.getPendingOrganizationReviewCount()) + " organization applications waiting for review.";
        }
        if (value(metrics.getPendingProviderResponseCount()) > 0) {
            return "There are " + value(metrics.getPendingProviderResponseCount()) + " training requests waiting for provider response.";
        }
        if (value(metrics.getOnlineTrainingMissingJoinLinkCount()) > 0) {
            return "There are " + value(metrics.getOnlineTrainingMissingJoinLinkCount()) + " upcoming online trainings missing a join link.";
        }
        if (value(metrics.getUpcomingTrainingCount()) > 0) {
            return "There are " + value(metrics.getUpcomingTrainingCount()) + " scheduled trainings in the next " + days + " days.";
        }
        if (hasAmount(metrics.getPlatformCommissionAmount())) {
            return "Platform commission in the last " + days + " days: " + money(metrics.getPlatformCommissionAmount()) + " SAR.";
        }
        return "Platform activity is stable in the last " + days + " days.";
    }

    private List<String> buildFallbackBullets(String language, int days, AdminWeeklySummaryResponse.Metrics metrics) {
        List<String> bullets = new ArrayList<>();

        if (language.equals("ar")) {
            addIfPositive(bullets, metrics.getPendingOrganizationReviewCount(), "طلبات جهات بانتظار المراجعة: %d.");
            addIfPositive(bullets, metrics.getPendingProviderResponseCount(), "طلبات تدريب بانتظار رد مقدم الخدمة: %d.");
            addIfPositive(bullets, metrics.getUpcomingTrainingCount(), "تدريبات مجدولة قادمة خلال " + days + " يوم: %d.");
            addIfPositive(bullets, metrics.getOnlineTrainingMissingJoinLinkCount(), "تدريبات أونلاين قادمة بدون رابط انضمام: %d.");
            addIfPositive(bullets, metrics.getPendingPaymentCount(), "مدفوعات معلقة: %d.");
            addIfPositive(bullets, metrics.getNewRequestCount(), "طلبات تدريب جديدة خلال آخر " + days + " يوم: %d.");
            addIfPositive(bullets, metrics.getNewOrganizationApplicationCount(), "طلبات جهات جديدة خلال آخر " + days + " يوم: %d.");
            if (hasAmount(metrics.getPlatformCommissionAmount())) {
                bullets.add("عمولة المنصة خلال الفترة: " + money(metrics.getPlatformCommissionAmount()) + " ر.س.");
            }
            if (bullets.isEmpty()) {
                bullets.add("لا توجد مؤشرات مهمة تحتاج مراجعة خلال الفترة.");
            }
            return bullets.stream().limit(4).toList();
        }

        addIfPositive(bullets, metrics.getPendingOrganizationReviewCount(), "Organization applications waiting for review: %d.");
        addIfPositive(bullets, metrics.getPendingProviderResponseCount(), "Training requests waiting for provider response: %d.");
        addIfPositive(bullets, metrics.getUpcomingTrainingCount(), "Upcoming scheduled trainings in the next " + days + " days: %d.");
        addIfPositive(bullets, metrics.getOnlineTrainingMissingJoinLinkCount(), "Upcoming online trainings missing a join link: %d.");
        addIfPositive(bullets, metrics.getPendingPaymentCount(), "Pending payments: %d.");
        addIfPositive(bullets, metrics.getNewRequestCount(), "New training requests in the last " + days + " days: %d.");
        addIfPositive(bullets, metrics.getNewOrganizationApplicationCount(), "New organization applications in the last " + days + " days: %d.");
        if (hasAmount(metrics.getPlatformCommissionAmount())) {
            bullets.add("Platform commission in the period: " + money(metrics.getPlatformCommissionAmount()) + " SAR.");
        }
        if (bullets.isEmpty()) {
            bullets.add("No important metrics need review during the period.");
        }
        return bullets.stream().limit(4).toList();
    }

    private void addIfPositive(List<String> bullets, Integer value, String template) {
        if (value != null && value > 0) {
            bullets.add(String.format(template, value));
        }
    }

    private String buildFallbackNextAction(String language, AdminWeeklySummaryResponse.Metrics metrics) {
        if (language.equals("ar")) {
            if (value(metrics.getPendingOrganizationReviewCount()) > 0) {
                return "راجع طلبات الجهات المعلقة ووثّقها أو ارفضها.";
            }
            if (value(metrics.getPendingProviderResponseCount()) > 0) {
                return "راقب طلبات التدريب التي لم يرد عليها مقدمو الخدمة.";
            }
            if (value(metrics.getOnlineTrainingMissingJoinLinkCount()) > 0) {
                return "تابع إنشاء روابط Zoom للتدريبات الأونلاين القادمة.";
            }
            if (value(metrics.getPendingPaymentCount()) > 0) {
                return "تابع المدفوعات المعلقة وتأثيرها على تأكيد التدريب.";
            }
            if (value(metrics.getUpcomingTrainingCount()) > 0) {
                return "راقب تدريبات الأسبوع القادم ومشاكل الربط أو الحضور.";
            }
            return "تابع نشاط المنصة ومؤشرات النمو.";
        }

        if (value(metrics.getPendingOrganizationReviewCount()) > 0) {
            return "Review pending organization applications and verify or reject them.";
        }
        if (value(metrics.getPendingProviderResponseCount()) > 0) {
            return "Monitor training requests waiting for provider response.";
        }
        if (value(metrics.getOnlineTrainingMissingJoinLinkCount()) > 0) {
            return "Follow up on Zoom join link creation for upcoming online trainings.";
        }
        if (value(metrics.getPendingPaymentCount()) > 0) {
            return "Monitor pending payments and their impact on training confirmation.";
        }
        if (value(metrics.getUpcomingTrainingCount()) > 0) {
            return "Watch next week's scheduled trainings and possible link or attendance issues.";
        }
        return "Monitor platform activity and growth metrics.";
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean hasAmount(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
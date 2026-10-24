package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiProviderWeeklySummary;
import org.fadhel.waeiplatform.dto.out.BeneficiaryWeeklySummaryResponse;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.security.AccessControl;
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
public class BeneficiaryWeeklySummaryService {

    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final OrganizationRepository organizationRepository;
    private final TrainingRequestService trainingRequestService;
    private final GeminiService geminiService;

    public BeneficiaryWeeklySummaryResponse getWeeklySummary(String lang, int days) {
        if (days < 1 || days > 30) {
            throw new ApiException("Weekly summary period must be between 1 and 30 days");
        }

        String language = normalizeLanguage(lang);
        requireBeneficiaryOrganization();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(days);
        LocalDateTime to = now.plusDays(days);

        List<TrainingRequestResponse> requests = trainingRequestService.getRequestsByOrganization(currentUser.getOrganizationId());
        BeneficiaryWeeklySummaryResponse.Metrics metrics = computeMetrics(requests, now, from, to);
        BeneficiaryWeeklySummaryResponse fallback = buildFallback(language, days, now, from, to, metrics);

        try {
            AiProviderWeeklySummary generated = geminiService.summarizeBeneficiaryWeek(buildFacts(metrics, days), language);
            BeneficiaryWeeklySummaryResponse result = applyGenerated(fallback, generated);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Gemini beneficiary weekly summary failed, using deterministic fallback: {}", e.getMessage());
        }

        return fallback;
    }

    private String normalizeLanguage(String lang) {
        return "en".equalsIgnoreCase(lang) ? "en" : "ar";
    }

    private void requireBeneficiaryOrganization() {
        if (currentUser.isPlatformAdmin()) {
            throw beneficiaryDenied();
        }

        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            throw beneficiaryDenied();
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found"));

        if (!accessControl.isBeneficiaryOrganization(organization)) {
            throw beneficiaryDenied();
        }

        accessControl.requireVerifiedOrganization(organization);
    }

    private ResponseStatusException beneficiaryDenied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the beneficiary organization can use the weekly summary assistant");
    }

    private BeneficiaryWeeklySummaryResponse.Metrics computeMetrics(
            List<TrainingRequestResponse> requests,
            LocalDateTime now,
            LocalDateTime from,
            LocalDateTime to) {
        int requestCount = requests == null ? 0 : requests.size();
        int newRequestCount = 0;
        int pendingProviderResponseCount = 0;
        int awaitingPaymentCount = 0;
        int activeTrainingCount = 0;
        int upcomingTrainingCount = 0;
        int inProgressTrainingCount = 0;
        int completedTrainingCount = 0;
        int cancelledTrainingCount = 0;
        int onlineTrainingMissingJoinLinkCount = 0;
        BigDecimal paidTrainingAmount = BigDecimal.ZERO;

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
            if (TrainingRequestStatus.IN_PROGRESS.name().equals(status)) {
                inProgressTrainingCount++;
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

            if (request.getPayments() != null) {
                for (PaymentResponse payment : request.getPayments()) {
                    if (PaymentStatus.PAID.name().equals(payment.getStatus())
                            && withinPast(payment.getPaidAt(), from, now)) {
                        paidTrainingAmount = paidTrainingAmount.add(paymentAmount(payment));
                    }
                }
            }
        }

        return BeneficiaryWeeklySummaryResponse.Metrics.builder()
                .requestCount(requestCount)
                .newRequestCount(newRequestCount)
                .pendingProviderResponseCount(pendingProviderResponseCount)
                .awaitingPaymentCount(awaitingPaymentCount)
                .activeTrainingCount(activeTrainingCount)
                .upcomingTrainingCount(upcomingTrainingCount)
                .inProgressTrainingCount(inProgressTrainingCount)
                .completedTrainingCount(completedTrainingCount)
                .cancelledTrainingCount(cancelledTrainingCount)
                .onlineTrainingMissingJoinLinkCount(onlineTrainingMissingJoinLinkCount)
                .paidTrainingAmount(paidTrainingAmount.setScale(2, RoundingMode.HALF_UP))
                .build();
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

    private BigDecimal paymentAmount(PaymentResponse payment) {
        if (payment.getAmount() != null) {
            return payment.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private String buildFacts(BeneficiaryWeeklySummaryResponse.Metrics metrics, int days) {
        return """
                audience=beneficiaryOrganization
                periodDays=%d
                trainingRequestCount=%d
                newRequestCount=%d
                pendingProviderResponseCount=%d
                awaitingPaymentCount=%d
                activeTrainingCount=%d
                upcomingTrainingCount=%d
                inProgressTrainingCount=%d
                completedTrainingCount=%d
                cancelledTrainingCount=%d
                onlineTrainingMissingJoinLinkCount=%d
                paidTrainingAmountSAR=%s
                """
                .formatted(
                        days,
                        value(metrics.getRequestCount()),
                        value(metrics.getNewRequestCount()),
                        value(metrics.getPendingProviderResponseCount()),
                        value(metrics.getAwaitingPaymentCount()),
                        value(metrics.getActiveTrainingCount()),
                        value(metrics.getUpcomingTrainingCount()),
                        value(metrics.getInProgressTrainingCount()),
                        value(metrics.getCompletedTrainingCount()),
                        value(metrics.getCancelledTrainingCount()),
                        value(metrics.getOnlineTrainingMissingJoinLinkCount()),
                        money(metrics.getPaidTrainingAmount()));
    }

    private BeneficiaryWeeklySummaryResponse buildFallback(
            String language,
            int days,
            LocalDateTime now,
            LocalDateTime from,
            LocalDateTime to,
            BeneficiaryWeeklySummaryResponse.Metrics metrics) {
        return BeneficiaryWeeklySummaryResponse.builder()
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

    private BeneficiaryWeeklySummaryResponse applyGenerated(
            BeneficiaryWeeklySummaryResponse fallback,
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

    private String buildFallbackHeadline(String language, int days, BeneficiaryWeeklySummaryResponse.Metrics metrics) {
        if (language.equals("ar")) {
            if (value(metrics.getPendingProviderResponseCount()) > 0) {
                return "لديك " + value(metrics.getPendingProviderResponseCount()) + " طلب تدريب بانتظار موافقة مقدم الخدمة.";
            }
            if (value(metrics.getAwaitingPaymentCount()) > 0) {
                return "لديك " + value(metrics.getAwaitingPaymentCount()) + " طلب بانتظار الدفع أو التأكيد.";
            }
            if (value(metrics.getUpcomingTrainingCount()) > 0) {
                return "لديك " + value(metrics.getUpcomingTrainingCount()) + " تدريبات مجدولة قادمة خلال " + days + " يوم.";
            }
            if (value(metrics.getCompletedTrainingCount()) > 0) {
                return "أنجزت " + value(metrics.getCompletedTrainingCount()) + " تدريبات خلال آخر " + days + " يوم.";
            }
            if (value(metrics.getRequestCount()) == 0) {
                return "ابدأ بطلب أول برنامج تدريبي لجهتك.";
            }
            return "ملخص آخر " + days + " يوم: ما فيه نشاط جديد مهم.";
        }

        if (value(metrics.getPendingProviderResponseCount()) > 0) {
            return "You have " + value(metrics.getPendingProviderResponseCount()) + " training request waiting for provider approval.";
        }
        if (value(metrics.getAwaitingPaymentCount()) > 0) {
            return "You have " + value(metrics.getAwaitingPaymentCount()) + " requests waiting for payment or confirmation.";
        }
        if (value(metrics.getUpcomingTrainingCount()) > 0) {
            return "You have " + value(metrics.getUpcomingTrainingCount()) + " scheduled trainings in the next " + days + " days.";
        }
        if (value(metrics.getCompletedTrainingCount()) > 0) {
            return "You completed " + value(metrics.getCompletedTrainingCount()) + " trainings in the last " + days + " days.";
        }
        if (value(metrics.getRequestCount()) == 0) {
            return "Start by requesting your first training program.";
        }
        return "No major new activity in the last " + days + " days.";
    }

    private List<String> buildFallbackBullets(String language, int days, BeneficiaryWeeklySummaryResponse.Metrics metrics) {
        List<String> bullets = new ArrayList<>();

        if (language.equals("ar")) {
            addIfPositive(bullets, metrics.getPendingProviderResponseCount(), "طلبات بانتظار موافقة مقدم الخدمة: %d.");
            addIfPositive(bullets, metrics.getAwaitingPaymentCount(), "طلبات بانتظار الدفع أو التأكيد: %d.");
            addIfPositive(bullets, metrics.getUpcomingTrainingCount(), "تدريبات مجدولة قادمة خلال " + days + " يوم: %d.");
            addIfPositive(bullets, metrics.getOnlineTrainingMissingJoinLinkCount(), "تدريبات أونلاين بدون رابط انضمام: %d.");
            if (value(metrics.getCompletedTrainingCount()) > 0 || hasAmount(metrics.getPaidTrainingAmount())) {
                bullets.add("تدريبات مكتملة خلال الفترة: " + value(metrics.getCompletedTrainingCount())
                        + "، وإجمالي المدفوع: " + money(metrics.getPaidTrainingAmount()) + " ر.س.");
            }
            addIfPositive(bullets, metrics.getActiveTrainingCount(), "تدريبات نشطة: %d.");
            if (bullets.isEmpty()) {
                bullets.add("ما فيه طلبات أو تدريبات مهمة تحتاج إجراء.");
            }
            return bullets.stream().limit(4).toList();
        }

        addIfPositive(bullets, metrics.getPendingProviderResponseCount(), "Training requests waiting for provider approval: %d.");
        addIfPositive(bullets, metrics.getAwaitingPaymentCount(), "Requests waiting for payment or confirmation: %d.");
        addIfPositive(bullets, metrics.getUpcomingTrainingCount(), "Upcoming scheduled trainings in the next " + days + " days: %d.");
        addIfPositive(bullets, metrics.getOnlineTrainingMissingJoinLinkCount(), "Online trainings missing a join link: %d.");
        if (value(metrics.getCompletedTrainingCount()) > 0 || hasAmount(metrics.getPaidTrainingAmount())) {
            bullets.add("Completed trainings: " + value(metrics.getCompletedTrainingCount())
                    + ", total paid: " + money(metrics.getPaidTrainingAmount()) + " SAR.");
        }
        addIfPositive(bullets, metrics.getActiveTrainingCount(), "Active trainings: %d.");
        if (bullets.isEmpty()) {
            bullets.add("No important training requests or scheduled trainings need action.");
        }
        return bullets.stream().limit(4).toList();
    }

    private void addIfPositive(List<String> bullets, Integer value, String template) {
        if (value != null && value > 0) {
            bullets.add(String.format(template, value));
        }
    }

    private String buildFallbackNextAction(String language, BeneficiaryWeeklySummaryResponse.Metrics metrics) {
        if (language.equals("ar")) {
            if (value(metrics.getPendingProviderResponseCount()) > 0) {
                return "تابع الطلبات التي تنتظر موافقة مقدم الخدمة وعدّل الموعد إذا لزم.";
            }
            if (value(metrics.getAwaitingPaymentCount()) > 0) {
                return "ادفع الطلبات المقبولة لتأكيد التدريبات.";
            }
            if (value(metrics.getOnlineTrainingMissingJoinLinkCount()) > 0) {
                return "تحقق من روابط انضمام Zoom قبل التدريبات الأونلاين القادمة.";
            }
            if (value(metrics.getUpcomingTrainingCount()) > 0) {
                return "جهّز الموظفين للحضور قبل المواعيد القادمة.";
            }
            if (value(metrics.getRequestCount()) == 0) {
                return "اطلب أول برنامج تدريبي من قائمة البرامج المتاحة.";
            }
            return "راقب حالة الطلبات والتدريبات القادمة.";
        }

        if (value(metrics.getPendingProviderResponseCount()) > 0) {
            return "Follow up on requests waiting for provider approval and adjust the schedule if needed.";
        }
        if (value(metrics.getAwaitingPaymentCount()) > 0) {
            return "Pay approved requests to confirm the trainings.";
        }
        if (value(metrics.getOnlineTrainingMissingJoinLinkCount()) > 0) {
            return "Check Zoom join links before upcoming online trainings.";
        }
        if (value(metrics.getUpcomingTrainingCount()) > 0) {
            return "Prepare employees to attend the upcoming sessions.";
        }
        if (value(metrics.getRequestCount()) == 0) {
            return "Request your first training program from the available programs list.";
        }
        return "Monitor request statuses and upcoming trainings.";
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
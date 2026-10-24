package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiProviderWeeklySummary;
import org.fadhel.waeiplatform.dto.out.PaymentResponse;
import org.fadhel.waeiplatform.dto.out.ProviderWeeklySummaryResponse;
import org.fadhel.waeiplatform.dto.out.TrainingProgramResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
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
public class ProviderWeeklySummaryService {

    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final OrganizationRepository organizationRepository;
    private final TrainingProgramService trainingProgramService;
    private final TrainingRequestService trainingRequestService;
    private final GeminiService geminiService;

    public ProviderWeeklySummaryResponse getWeeklySummary(String lang, int days) {
        if (days < 1 || days > 30) {
            throw new ApiException("Weekly summary period must be between 1 and 30 days");
        }

        String language = normalizeLanguage(lang);
        requireProviderOrganization();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(days);
        LocalDateTime to = now.plusDays(days);

        List<TrainingProgramResponse> programs = trainingProgramService.getMyPrograms();
        List<TrainingRequestResponse> requests = trainingRequestService.getMyProviderRequests();
        ProviderWeeklySummaryResponse.Metrics metrics = computeMetrics(programs, requests, now, from, to);
        ProviderWeeklySummaryResponse fallback = buildFallback(language, days, now, from, to, metrics);

        try {
            AiProviderWeeklySummary generated = geminiService.summarizeProviderWeek(buildFacts(metrics, days), language);
            ProviderWeeklySummaryResponse result = applyGenerated(fallback, generated);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Gemini provider weekly summary failed, using deterministic fallback: {}", e.getMessage());
        }

        return fallback;
    }

    private String normalizeLanguage(String lang) {
        return "en".equalsIgnoreCase(lang) ? "en" : "ar";
    }

    private void requireProviderOrganization() {
        if (currentUser.isPlatformAdmin()) {
            throw providerDenied();
        }

        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            throw providerDenied();
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found"));

        if (!accessControl.isProviderOrganization(organization)) {
            throw providerDenied();
        }

        accessControl.requireVerifiedOrganization(organization);
    }

    private ResponseStatusException providerDenied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the training provider can use the weekly summary assistant");
    }

    private ProviderWeeklySummaryResponse.Metrics computeMetrics(
            List<TrainingProgramResponse> programs,
            List<TrainingRequestResponse> requests,
            LocalDateTime now,
            LocalDateTime from,
            LocalDateTime to) {
        int newRequestCount = 0;
        int pendingRequestCount = 0;
        int awaitingPaymentCount = 0;
        int upcomingSessionCount = 0;
        int inProgressSessionCount = 0;
        int completedSessionCount = 0;
        int cancelledSessionCount = 0;
        int onlineMeetingMissingCount = 0;
        BigDecimal paidProviderRevenue = BigDecimal.ZERO;

        int programCount = programs == null ? 0 : programs.size();
        int activeProgramCount = programs == null ? 0 : (int) programs.stream()
                .filter(program -> Boolean.TRUE.equals(program.getIsActive()))
                .count();

        for (TrainingRequestResponse request : requests) {
            String status = request.getStatus();
            if (withinPast(request.getRequestedAt(), from, now)) {
                newRequestCount++;
            }
            if (TrainingRequestStatus.PENDING.name().equals(status)) {
                pendingRequestCount++;
            }
            if (TrainingRequestStatus.ACCEPTED.name().equals(status)
                    || TrainingRequestStatus.PAYMENT_PENDING.name().equals(status)) {
                awaitingPaymentCount++;
            }
            if (TrainingRequestStatus.IN_PROGRESS.name().equals(status)) {
                inProgressSessionCount++;
            }
            if (TrainingRequestStatus.COMPLETED.name().equals(status)
                    && withinPast(request.getCompletedAt(), from, now)) {
                completedSessionCount++;
            }
            if (TrainingRequestStatus.CANCELLED.name().equals(status)
                    && withinPast(request.getCancelledAt(), from, now)) {
                cancelledSessionCount++;
            }

            boolean upcoming = isUpcomingSession(request, now, to);
            if (upcoming) {
                upcomingSessionCount++;
            }
            if (upcoming
                    && "ONLINE".equals(request.getTrainingProgramDeliveryMode())
                    && (TrainingRequestStatus.CONFIRMED.name().equals(status)
                    || TrainingRequestStatus.IN_PROGRESS.name().equals(status))
                    && hasNoJoinUrl(request)) {
                onlineMeetingMissingCount++;
            }

            if (request.getPayments() != null) {
                for (PaymentResponse payment : request.getPayments()) {
                    if (PaymentStatus.PAID.name().equals(payment.getStatus())
                            && withinPast(payment.getPaidAt(), from, now)) {
                        paidProviderRevenue = paidProviderRevenue.add(paymentProviderAmount(payment));
                    }
                }
            }
        }

        return ProviderWeeklySummaryResponse.Metrics.builder()
                .programCount(programCount)
                .activeProgramCount(activeProgramCount)
                .newRequestCount(newRequestCount)
                .pendingRequestCount(pendingRequestCount)
                .awaitingPaymentCount(awaitingPaymentCount)
                .upcomingSessionCount(upcomingSessionCount)
                .inProgressSessionCount(inProgressSessionCount)
                .completedSessionCount(completedSessionCount)
                .cancelledSessionCount(cancelledSessionCount)
                .onlineMeetingMissingCount(onlineMeetingMissingCount)
                .paidProviderRevenue(paidProviderRevenue.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private boolean withinPast(LocalDateTime value, LocalDateTime from, LocalDateTime now) {
        return value != null && !value.isBefore(from) && !value.isAfter(now);
    }

    private boolean isUpcomingSession(TrainingRequestResponse request, LocalDateTime now, LocalDateTime to) {
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

    private BigDecimal paymentProviderAmount(PaymentResponse payment) {
        if (payment.getProviderAmount() != null) {
            return payment.getProviderAmount();
        }
        if (payment.getAmount() != null) {
            return payment.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private String buildFacts(ProviderWeeklySummaryResponse.Metrics metrics, int days) {
        return """
                periodDays=%d
                programCount=%d
                activeProgramCount=%d
                newRequestCount=%d
                pendingRequestCount=%d
                awaitingPaymentCount=%d
                upcomingSessionCount=%d
                inProgressSessionCount=%d
                completedSessionCount=%d
                cancelledSessionCount=%d
                onlineMeetingMissingCount=%d
                paidProviderRevenueSAR=%s
                """
                .formatted(
                        days,
                        value(metrics.getProgramCount()),
                        value(metrics.getActiveProgramCount()),
                        value(metrics.getNewRequestCount()),
                        value(metrics.getPendingRequestCount()),
                        value(metrics.getAwaitingPaymentCount()),
                        value(metrics.getUpcomingSessionCount()),
                        value(metrics.getInProgressSessionCount()),
                        value(metrics.getCompletedSessionCount()),
                        value(metrics.getCancelledSessionCount()),
                        value(metrics.getOnlineMeetingMissingCount()),
                        money(metrics.getPaidProviderRevenue()));
    }

    private ProviderWeeklySummaryResponse buildFallback(
            String language,
            int days,
            LocalDateTime now,
            LocalDateTime from,
            LocalDateTime to,
            ProviderWeeklySummaryResponse.Metrics metrics) {
        List<String> bullets = buildFallbackBullets(language, days, metrics);
        String headline = buildFallbackHeadline(language, days, metrics);
        String nextAction = buildFallbackNextAction(language, metrics);

        return ProviderWeeklySummaryResponse.builder()
                .language(language)
                .days(days)
                .generatedAt(now)
                .from(from)
                .to(to)
                .headline(headline)
                .bullets(bullets)
                .nextAction(nextAction)
                .aiGenerated(false)
                .metrics(metrics)
                .build();
    }

    private ProviderWeeklySummaryResponse applyGenerated(
            ProviderWeeklySummaryResponse fallback,
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

    private String buildFallbackHeadline(String language, int days, ProviderWeeklySummaryResponse.Metrics metrics) {
        if (language.equals("ar")) {
            if (value(metrics.getPendingRequestCount()) > 0) {
                return "لديك " + value(metrics.getPendingRequestCount()) + " طلب تدريب بانتظار ردك.";
            }
            if (value(metrics.getUpcomingSessionCount()) > 0) {
                return "لديك " + value(metrics.getUpcomingSessionCount()) + " تدريب مجدول خلال " + days + " يوم.";
            }
            if (value(metrics.getCompletedSessionCount()) > 0) {
                return "أنجزت " + value(metrics.getCompletedSessionCount()) + " تدريب خلال آخر " + days + " يوم.";
            }
            if (value(metrics.getActiveProgramCount()) == 0) {
                return "أضف أول برنامج تدريب لبدء استقبال الطلبات.";
            }
            return "ملخص آخر " + days + " يوم: ما فيه نشاط جديد مهم.";
        }

        if (value(metrics.getPendingRequestCount()) > 0) {
            return "You have " + value(metrics.getPendingRequestCount()) + " request waiting for your response.";
        }
        if (value(metrics.getUpcomingSessionCount()) > 0) {
            return "You have " + value(metrics.getUpcomingSessionCount()) + " scheduled training in the next " + days + " days.";
        }
        if (value(metrics.getCompletedSessionCount()) > 0) {
            return "You completed " + value(metrics.getCompletedSessionCount()) + " training in the last " + days + " days.";
        }
        if (value(metrics.getActiveProgramCount()) == 0) {
            return "Add your first training program to start receiving requests.";
        }
        return "No major new activity in the last " + days + " days.";
    }

    private List<String> buildFallbackBullets(String language, int days, ProviderWeeklySummaryResponse.Metrics metrics) {
        List<String> bullets = new ArrayList<>();

        if (language.equals("ar")) {
            addIfPositive(bullets, metrics.getPendingRequestCount(), "طلبات تدريب بانتظار ردك: %d.");
            addIfPositive(bullets, metrics.getNewRequestCount(), "طلبات تدريب جديدة خلال آخر " + days + " يوم: %d.");
            addIfPositive(bullets, metrics.getUpcomingSessionCount(), "تدريبات مجدولة قادمة خلال " + days + " يوم: %d.");
            addIfPositive(bullets, metrics.getAwaitingPaymentCount(), "طلبات بانتظار الدفع أو التأكيد: %d.");
            addIfPositive(bullets, metrics.getOnlineMeetingMissingCount(), "تدريبات أونلاين بدون رابط انضمام: %d.");
            if (value(metrics.getCompletedSessionCount()) > 0 || hasRevenue(metrics.getPaidProviderRevenue())) {
                bullets.add("تدريبات مكتملة خلال الفترة: " + value(metrics.getCompletedSessionCount())
                        + "، وصافي الإيراد المدفوع: " + money(metrics.getPaidProviderRevenue()) + " ر.س.");
            }
            if (bullets.isEmpty()) {
                bullets.add("ما فيه طلبات تدريب أو تدريبات مجدولة مهمة تحتاج إجراء.");
            }
            return bullets.stream().limit(4).toList();
        }

        addIfPositive(bullets, metrics.getPendingRequestCount(), "Training requests waiting for your response: %d.");
        addIfPositive(bullets, metrics.getNewRequestCount(), "New training requests in the last " + days + " days: %d.");
        addIfPositive(bullets, metrics.getUpcomingSessionCount(), "Upcoming scheduled trainings in the next " + days + " days: %d.");
        addIfPositive(bullets, metrics.getAwaitingPaymentCount(), "Requests awaiting payment or confirmation: %d.");
        addIfPositive(bullets, metrics.getOnlineMeetingMissingCount(), "Online trainings missing a join link: %d.");
        if (value(metrics.getCompletedSessionCount()) > 0 || hasRevenue(metrics.getPaidProviderRevenue())) {
            bullets.add("Completed trainings: " + value(metrics.getCompletedSessionCount())
                    + ", paid net revenue: " + money(metrics.getPaidProviderRevenue()) + " SAR.");
        }
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

    private String buildFallbackNextAction(String language, ProviderWeeklySummaryResponse.Metrics metrics) {
        if (language.equals("ar")) {
            if (value(metrics.getPendingRequestCount()) > 0) {
                return "راجع طلبات التدريب الجديدة وحدد القبول أو الرفض.";
            }
            if (value(metrics.getOnlineMeetingMissingCount()) > 0) {
                return "تأكد من روابط انضمام Zoom قبل التدريبات الأونلاين القادمة.";
            }
            if (value(metrics.getAwaitingPaymentCount()) > 0) {
                return "تابع الطلبات المقبولة التي تنتظر الدفع أو التأكيد.";
            }
            if (value(metrics.getUpcomingSessionCount()) > 0) {
                return "جهّز محتوى التدريبات المجدولة القادمة قبل مواعيدها.";
            }
            if (value(metrics.getActiveProgramCount()) == 0) {
                return "أنشئ أول برنامج تدريبي ونشره للجهات المستفيدة.";
            }
            return "حافظ على إتاحة برامجك وراقب طلبات التدريب الجديدة.";
        }

        if (value(metrics.getPendingRequestCount()) > 0) {
            return "Review new training requests and accept or reject them.";
        }
        if (value(metrics.getOnlineMeetingMissingCount()) > 0) {
            return "Check Zoom join links before upcoming online trainings.";
        }
        if (value(metrics.getAwaitingPaymentCount()) > 0) {
            return "Follow accepted requests waiting for payment or confirmation.";
        }
        if (value(metrics.getUpcomingSessionCount()) > 0) {
            return "Prepare training materials before the next scheduled trainings.";
        }
        if (value(metrics.getActiveProgramCount()) == 0) {
            return "Create and publish your first training program.";
        }
        return "Keep your programs active and monitor new training requests.";
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean hasRevenue(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
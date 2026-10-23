package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.integration.ZoomClient;
import org.fadhel.waeiplatform.model.OnlineMeetingStatus;
import org.fadhel.waeiplatform.model.PaymentStatus;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.PaymentRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomMeetingService {

    private static final String DELIVERY_MODE_ONLINE = "ONLINE";
    private static final int MAX_RECURRING_OCCURRENCES = 50;
    private static final DateTimeFormatter ZOOM_START_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final List<TrainingRequestStatus> MEETING_REQUEST_STATUSES =
            List.of(TrainingRequestStatus.CONFIRMED, TrainingRequestStatus.IN_PROGRESS);

    private final ZoomClient zoomClient;
    private final TrainingRequestRepository trainingRequestRepository;
    private final PaymentRepository paymentRepository;
    private final AccessControl accessControl;

    @Value("${app.zoom.batch-size:20}")
    private int batchSize;

    @Value("${app.zoom.retry-cooldown-ms:300000}")
    private long retryCooldownMs;

    @Value("${zoom.timezone:Asia/Riyadh}")
    private String timezone;

    public int createMissingMeetings() {
        if (!zoomClient.isConfigured()) {
            return 0;
        }

        LocalDateTime retryBefore = LocalDateTime.now()
                .minus(Duration.ofMillis(Math.max(0, retryCooldownMs)));
        List<Long> requestIds = trainingRequestRepository.findIdsNeedingOnlineMeeting(
                DELIVERY_MODE_ONLINE,
                MEETING_REQUEST_STATUSES,
                PaymentStatus.PAID,
                retryBefore,
                PageRequest.of(0, Math.max(1, batchSize)));

        int created = 0;
        for (Long requestId : requestIds) {
            if (createForRequest(requestId)) {
                created++;
            }
        }
        return created;
    }

    @Transactional
    public boolean createForRequest(Long requestId) {
        TrainingRequest request = trainingRequestRepository.findById(requestId).orElse(null);
        if (request == null
                || !isEligible(request)
                || hasText(request.getZoomJoinUrl())
                || !hasPaidPayment(requestId)
                || !zoomClient.isConfigured()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        request.setMeetingStatus(OnlineMeetingStatus.PENDING);
        request.setZoomLastAttemptAt(now);
        request.setZoomAttemptCount((request.getZoomAttemptCount() != null
                ? request.getZoomAttemptCount()
                : 0) + 1);

        try {
            ZoomClient.ZoomMeeting meeting = zoomClient.createMeeting(buildMeetingSpec(request));
            request.setZoomMeetingId(meeting.id());
            request.setZoomJoinUrl(meeting.joinUrl());
            request.setZoomPassword(meeting.password());
            request.setZoomCreatedAt(now);
            request.setMeetingStatus(OnlineMeetingStatus.CREATED);
            request.setZoomLastError(null);
            trainingRequestRepository.save(request);
            log.info("Created Zoom meeting {} for training request {}", meeting.id(), requestId);
            return true;
        } catch (RuntimeException e) {
            request.setMeetingStatus(OnlineMeetingStatus.FAILED);
            request.setZoomLastError(limit(e.getMessage(), 1000));
            trainingRequestRepository.save(request);
            log.warn("Could not create Zoom meeting for training request {}: {}", requestId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void deleteForRequest(TrainingRequest request) {
        if (request == null
                || (!hasText(request.getZoomMeetingId()) && !hasText(request.getZoomJoinUrl()))) {
            return;
        }

        if (hasText(request.getZoomMeetingId()) && zoomClient.isConfigured()) {
            try {
                zoomClient.deleteMeeting(request.getZoomMeetingId());
            } catch (RuntimeException e) {
                log.warn("Could not delete Zoom meeting {} for training request {}: {}",
                        request.getZoomMeetingId(), request.getId(), e.getMessage());
            }
        }

        request.setZoomJoinUrl(null);
        request.setZoomPassword(null);
        request.setZoomLastError(null);
        request.setMeetingStatus(OnlineMeetingStatus.DELETED);
        trainingRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public String getFreshHostStartUrl(Long requestId) {
        TrainingRequest request = trainingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Training request not found"));
        accessControl.requireProviderOwner(request);

        if (request.getStatus() != TrainingRequestStatus.CONFIRMED
                && request.getStatus() != TrainingRequestStatus.IN_PROGRESS) {
            throw new ApiException("The Zoom meeting can only be started for a confirmed or in-progress training");
        }
        if (!hasText(request.getZoomMeetingId())
                || !hasText(request.getZoomJoinUrl())
                || request.getMeetingStatus() != OnlineMeetingStatus.CREATED) {
            throw new ApiException("The Zoom meeting link is not ready yet");
        }
        if (!zoomClient.isConfigured()) {
            throw new ApiException("Zoom is not configured");
        }

        return zoomClient.fetchHostStartUrl(request.getZoomMeetingId());
    }

    private boolean isEligible(TrainingRequest request) {
        return request.getTrainingProgram() != null
                && DELIVERY_MODE_ONLINE.equalsIgnoreCase(request.getTrainingProgram().getDeliveryMode())
                && MEETING_REQUEST_STATUSES.contains(request.getStatus())
                && request.getScheduledStart() != null
                && request.getScheduledEnd() != null;
    }

    private boolean hasPaidPayment(Long requestId) {
        return paymentRepository.existsByTrainingRequest_IdAndStatusIn(
                requestId, List.of(PaymentStatus.PAID));
    }

    private ZoomClient.MeetingSpec buildMeetingSpec(TrainingRequest request) {
        int days = request.getDaysCount() != null && request.getDaysCount() > 0
                ? request.getDaysCount()
                : 1;
        String topic = "WAEI training - " + request.getTrainingProgram().getTitle();
        if (topic.length() > 200) {
            topic = topic.substring(0, 200);
        }

        if (days > MAX_RECURRING_OCCURRENCES) {
            return new ZoomClient.MeetingSpec(topic, 3, null, null, timezone, null);
        }
        if (days > 1) {
            return new ZoomClient.MeetingSpec(
                    topic,
                    8,
                    request.getScheduledStart().format(ZOOM_START_TIME_FORMAT),
                    durationMinutes(request.getHoursPerDay()),
                    timezone,
                    days);
        }
        return new ZoomClient.MeetingSpec(
                topic,
                2,
                request.getScheduledStart().format(ZOOM_START_TIME_FORMAT),
                durationMinutes(request.getHoursPerDay()),
                timezone,
                null);
    }

    private int durationMinutes(BigDecimal hoursPerDay) {
        if (hoursPerDay == null || hoursPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            return 60;
        }
        return Math.max(1, hoursPerDay.multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue());
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

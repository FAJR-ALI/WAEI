package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiInternalAnnouncement;
import org.fadhel.waeiplatform.dto.in.InternalAnnouncementRequest;
import org.fadhel.waeiplatform.dto.out.InternalAnnouncementResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalAnnouncementService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final OrganizationRepository organizationRepository;
    private final TrainingRequestService trainingRequestService;
    private final GeminiService geminiService;

    public InternalAnnouncementResponse generate(InternalAnnouncementRequest request) {
        requireBeneficiaryOrganization();

        TrainingRequestResponse trainingRequest = findRequestForCurrentBeneficiary(request.getTrainingRequestId());
        validateAnnounceableRequest(trainingRequest);

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new ApiException("Organization not found"));

        String language = normalizeLanguage(request.getLang());
        String channel = normalizeChannel(request.getChannel());

        String whenLine = buildWhenLine(trainingRequest, language);
        AiInternalAnnouncement generated = geminiService.generateInternalAnnouncement(
                buildFacts(organization, trainingRequest, channel, language, whenLine),
                language);

        List<String> checklist = cleanChecklist(generated.getChecklist());
        String subject = cleanText(generated.getSubject(), 120);
        String message = cleanText(generated.getMessage(), 700);
        String reminder = cleanText(generated.getReminder(), 240);

        if (subject.isBlank() || message.isBlank() || checklist.isEmpty()) {
            throw new ApiException("AI announcement is unavailable, please try again");
        }

        return InternalAnnouncementResponse.builder()
                .trainingRequestId(trainingRequest.getId())
                .language(language)
                .channel(channel)
                .generatedAt(LocalDateTime.now())
                .programTitle(trainingRequest.getTrainingProgramTitle())
                .providerOrganizationName(trainingRequest.getProviderOrganizationName())
                .scheduledStart(trainingRequest.getScheduledStart())
                .scheduledEnd(trainingRequest.getScheduledEnd())
                .whenLine(whenLine)
                .deliveryMode(trainingRequest.getTrainingProgramDeliveryMode())
                .participantCount(trainingRequest.getParticipantCount())
                .onlineJoinUrl(trainingRequest.getMeetingJoinUrl())
                .aiGenerated(true)
                .subject(subject)
                .message(message)
                .checklist(checklist)
                .reminder(reminder)
                .build();
    }

    private String normalizeLanguage(String lang) {
        return "en".equalsIgnoreCase(lang) ? "en" : "ar";
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "notice";
        }
        return channel.toLowerCase();
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
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the beneficiary organization can use the announcement assistant");
    }

    private TrainingRequestResponse findRequestForCurrentBeneficiary(Long trainingRequestId) {
        if (trainingRequestId == null) {
            throw new ApiException("Training request is required");
        }

        return trainingRequestService.getRequestsByOrganization(currentUser.getOrganizationId()).stream()
                .filter(item -> trainingRequestId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Training request not found"));
    }

    private void validateAnnounceableRequest(TrainingRequestResponse request) {
        String status = request.getStatus();
        if (request.getScheduledStart() == null) {
            throw new ApiException("The training date is not set for this request");
        }
        if (!TrainingRequestStatus.ACCEPTED.name().equals(status)
                && !TrainingRequestStatus.CONFIRMED.name().equals(status)
                && !TrainingRequestStatus.IN_PROGRESS.name().equals(status)) {
            throw new ApiException("This training request is not ready for an internal announcement");
        }
    }

    private String buildFacts(
            Organization organization,
            TrainingRequestResponse request,
            String channel,
            String language,
            String whenLine) {
        List<String> lines = new ArrayList<>();
        lines.add("audience=beneficiary internal staff");
        lines.add("channel=" + channel);
        lines.add("language=" + language);
        lines.add("beneficiaryOrganization=" + value(request.getBeneficiaryOrganizationName(), organization.getName()));
        lines.add("sector=" + value(organization.getSector(), ""));
        lines.add("programTitle=" + value(request.getTrainingProgramTitle(), ""));
        lines.add("provider=" + value(request.getProviderOrganizationName(), ""));
        lines.add("scheduledStart=" + formatDateTime(request.getScheduledStart()));
        lines.add("scheduledEnd=" + formatDateTime(request.getScheduledEnd()));
        lines.add("requiredDateLine=" + whenLine);
        lines.add("participantCount=" + value(request.getParticipantCount(), ""));
        lines.add("daysCount=" + value(request.getDaysCount(), ""));
        lines.add("hoursPerDay=" + value(request.getHoursPerDay(), ""));
        lines.add("deliveryMode=" + value(request.getTrainingProgramDeliveryMode(), ""));
        boolean hasJoinUrl = request.getMeetingJoinUrl() != null && !request.getMeetingJoinUrl().isBlank();
        lines.add("onlineJoinLinkProvided=" + (hasJoinUrl ? "yes" : "no"));
        if (hasJoinUrl) {
            lines.add("onlineJoinLink=" + request.getMeetingJoinUrl().trim());
        }
        return String.join("\n", lines);
    }

    private List<String> cleanChecklist(List<String> raw) {
        if (raw == null) {
            return List.of();
        }

        List<String> cleaned = new ArrayList<>();
        for (String value : raw) {
            String item = cleanText(value, 220);
            if (!item.isBlank()) {
                cleaned.add(item);
            }
            if (cleaned.size() == 4) {
                break;
            }
        }
        return cleaned;
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "not set" : value.format(DATE_TIME_FORMATTER);
    }

    private String buildWhenLine(TrainingRequestResponse request, String language) {
        LocalDateTime start = request.getScheduledStart();
        if (start == null) {
            return "";
        }

        boolean english = "en".equalsIgnoreCase(language);
        Locale locale = english ? Locale.ENGLISH : Locale.forLanguageTag("ar");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(
                english ? "EEEE, d MMMM yyyy" : "EEEE، d MMMM yyyy",
                locale).withDecimalStyle(DecimalStyle.STANDARD);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
                .withDecimalStyle(DecimalStyle.STANDARD);

        LocalDateTime end = request.getScheduledEnd();
        if (end == null) {
            return english
                    ? "Date and time: " + start.format(dateFormatter) + ", " + start.format(timeFormatter)
                    : "الموعد: " + start.format(dateFormatter) + "، " + start.format(timeFormatter);
        }

        if (end.toLocalDate().isEqual(start.toLocalDate())) {
            return english
                    ? "Date and time: " + start.format(dateFormatter) + ", " + start.format(timeFormatter)
                    + " to " + end.format(timeFormatter)
                    : "الموعد: " + start.format(dateFormatter) + "، " + start.format(timeFormatter)
                    + " إلى " + end.format(timeFormatter);
        }

        return english
                ? "Date and time: from " + start.format(dateFormatter) + ", " + start.format(timeFormatter)
                + " to " + end.format(dateFormatter) + ", " + end.format(timeFormatter)
                : "الموعد: من " + start.format(dateFormatter) + "، " + start.format(timeFormatter)
                + " إلى " + end.format(dateFormatter) + "، " + end.format(timeFormatter);
    }

    private String value(Object value, Object fallback) {
        if (value != null && !String.valueOf(value).isBlank()) {
            return String.valueOf(value);
        }
        return String.valueOf(fallback);
    }
}
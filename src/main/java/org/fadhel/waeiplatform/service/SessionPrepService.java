package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiSessionPrep;
import org.fadhel.waeiplatform.dto.ai.AiSessionScenario;
import org.fadhel.waeiplatform.dto.in.SessionPrepRequest;
import org.fadhel.waeiplatform.dto.out.SessionPrepResponse;
import org.fadhel.waeiplatform.dto.out.TrainingRequestResponse;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.repository.TrainingProgramRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// "جهّزني": after the provider accepts a request, the AI suggests how to tailor
// the same program to this organization (focus points, scenarios from their workplace, equipment)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionPrepService {

    private final CurrentUser currentUser;
    private final AccessControl accessControl;
    private final OrganizationRepository organizationRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final TrainingRequestService trainingRequestService;
    private final GeminiService geminiService;

    public SessionPrepResponse generate(SessionPrepRequest request) {
        requireProviderOrganization();

        TrainingRequestResponse trainingRequest = findRequestForCurrentProvider(request.getTrainingRequestId());
        validatePreparableRequest(trainingRequest);

        TrainingProgram program = trainingProgramRepository.findById(trainingRequest.getTrainingProgramId())
                .orElseThrow(() -> new ApiException("Training program not found"));

        Organization beneficiary = null;
        if (trainingRequest.getBeneficiaryOrganizationId() != null) {
            beneficiary = organizationRepository.findById(trainingRequest.getBeneficiaryOrganizationId()).orElse(null);
        }
        String sector = beneficiary != null ? value(beneficiary.getSector(), "") : "";

        String language = "en".equalsIgnoreCase(request.getLang()) ? "en" : "ar";

        AiSessionPrep generated = geminiService.generateSessionPrep(
                buildFacts(trainingRequest, program, sector), language);

        List<String> focusPoints = cleanList(generated.getFocusPoints(), 4, 200);
        List<AiSessionScenario> scenarios = cleanScenarios(generated.getScenarios());
        List<String> equipment = cleanList(generated.getEquipment(), 5, 120);
        String audienceNote = cleanText(generated.getAudienceNote(), 300);
        String trainerTip = cleanText(generated.getTrainerTip(), 240);

        if (focusPoints.isEmpty() || scenarios.isEmpty()) {
            throw new ApiException("AI session prep is unavailable, please try again");
        }

        return SessionPrepResponse.builder()
                .trainingRequestId(trainingRequest.getId())
                .language(language)
                .generatedAt(LocalDateTime.now())
                .programTitle(trainingRequest.getTrainingProgramTitle())
                .beneficiaryOrganizationName(trainingRequest.getBeneficiaryOrganizationName())
                .beneficiarySector(sector)
                .participantCount(trainingRequest.getParticipantCount())
                .deliveryMode(trainingRequest.getTrainingProgramDeliveryMode())
                .aiGenerated(true)
                .audienceNote(audienceNote)
                .focusPoints(focusPoints)
                .scenarios(scenarios)
                .equipment(equipment)
                .trainerTip(trainerTip)
                .build();
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
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the training provider can use the session prep assistant");
    }

    // The provider only sees the requests of its own programs
    private TrainingRequestResponse findRequestForCurrentProvider(Long trainingRequestId) {
        if (trainingRequestId == null) {
            throw new ApiException("Training request is required");
        }

        return trainingRequestService.getMyProviderRequests().stream()
                .filter(item -> trainingRequestId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Training request not found"));
    }

    // Only after the provider accepted the request, and before it is finished
    private void validatePreparableRequest(TrainingRequestResponse request) {
        String status = request.getStatus();
        if (!TrainingRequestStatus.ACCEPTED.name().equals(status)
                && !TrainingRequestStatus.PAYMENT_PENDING.name().equals(status)
                && !TrainingRequestStatus.CONFIRMED.name().equals(status)
                && !TrainingRequestStatus.IN_PROGRESS.name().equals(status)) {
            throw new ApiException("Session prep is available after you accept the request");
        }
    }

    private String buildFacts(TrainingRequestResponse request, TrainingProgram program, String sector) {
        List<String> lines = new ArrayList<>();
        lines.add("programTitle=" + value(program.getTitle(), ""));
        lines.add("programCategory=" + value(program.getCategory(), ""));
        lines.add("programDescription=" + cleanText(program.getDescription(), 1200));
        lines.add("deliveryMode=" + value(program.getDeliveryMode(), ""));
        lines.add("daysCount=" + value(request.getDaysCount(), "1"));
        lines.add("hoursPerDay=" + value(request.getHoursPerDay(), ""));
        lines.add("beneficiaryOrganization=" + value(request.getBeneficiaryOrganizationName(), ""));
        lines.add("beneficiarySector=" + value(sector, "not provided"));
        lines.add("participantCount=" + value(request.getParticipantCount(), ""));
        lines.add("requestNotes=" + value(cleanText(request.getNotes(), 600), "none"));
        return String.join("\n", lines);
    }

    private List<AiSessionScenario> cleanScenarios(List<AiSessionScenario> raw) {
        List<AiSessionScenario> cleaned = new ArrayList<>();
        if (raw == null) {
            return cleaned;
        }
        for (AiSessionScenario scenario : raw) {
            if (scenario == null) {
                continue;
            }
            String title = cleanText(scenario.getTitle(), 80);
            String description = cleanText(scenario.getDescription(), 300);
            if (!title.isBlank() && !description.isBlank()) {
                cleaned.add(new AiSessionScenario(title, description));
            }
            if (cleaned.size() == 3) {
                break;
            }
        }
        return cleaned;
    }

    private List<String> cleanList(List<String> raw, int maxItems, int maxLength) {
        List<String> cleaned = new ArrayList<>();
        if (raw == null) {
            return cleaned;
        }
        for (String value : raw) {
            String item = cleanText(value, maxLength);
            if (!item.isBlank()) {
                cleaned.add(item);
            }
            if (cleaned.size() == maxItems) {
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

    private String value(Object value, Object fallback) {
        if (value != null && !String.valueOf(value).isBlank()) {
            return String.valueOf(value);
        }
        return String.valueOf(fallback);
    }
}

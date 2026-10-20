package org.fadhel.waeiplatform.service;


import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiEvaluationResult;
import org.fadhel.waeiplatform.dto.out.AwarenessSessionResponse;
import org.fadhel.waeiplatform.dto.out.ServiceStatsResponse;
import org.fadhel.waeiplatform.dto.out.VideoEvaluationResponse;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.model.AwarenessSession;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.fadhel.waeiplatform.repository.AwarenessSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AwarenessSessionService {

    private final AwarenessSessionRepository awarenessSessionRepository;
    private final AwarenessServiceRepository awarenessServiceRepository;
    private final GeminiService geminiService;

    // User uploads a video -> AI evaluates it -> we save the attempt
    public VideoEvaluationResponse evaluateVideo(Long serviceId, MultipartFile video, Boolean consentGiven, Boolean ageConfirmed) {

        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(serviceId);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }
        if (!service.getIsActive()) {
            throw new ApiException("This training is not active right now");
        }
        // Gemini API terms: users must be 18 or older
        if (ageConfirmed == null || !ageConfirmed) {
            throw new ApiException("Video evaluation is only available for users 18 years or older");
        }
        if (consentGiven == null || !consentGiven) {
            throw new ApiException("You must agree to let the AI analyze your video");
        }
        if (video == null || video.isEmpty()) {
            throw new ApiException("Please upload a video");
        }
        if (video.getSize() > 20 * 1024 * 1024) {
            throw new ApiException("Video is too large, maximum size is 20 MB");
        }
        if (video.getContentType() == null || !video.getContentType().startsWith("video/")) {
            throw new ApiException("The uploaded file must be a video");
        }

        LocalDateTime startedAt = LocalDateTime.now();

        AiEvaluationResult result = geminiService.evaluateVideo(video, service);

        if (result.getRelevant() == null || !result.getRelevant()) {
            throw new ApiException("The video does not show this training, please upload a clear video of yourself practicing it");
        }

        Double score = result.getScore();
        if (score == null || score < 0) {
            score = 0.0;
        }
        if (score > 100) {
            score = 100.0;
        }

        String verdict;
        if (score >= 80) {
            verdict = "CORRECT";
        } else if (score >= 50) {
            verdict = "PARTIALLY_CORRECT";
        } else {
            verdict = "INCORRECT";
        }

        String recommendations = "";
        if (result.getRecommendations() != null) {
            recommendations = String.join("\n", result.getRecommendations());
        }

        // We only save the video name, not the video itself (privacy)
        AwarenessSession session = new AwarenessSession();
        session.setAwarenessService(service);
        session.setStartedAt(startedAt);
        session.setCompletedAt(LocalDateTime.now());
        session.setScore(score);
        session.setResultSummary(result.getSummary());
        session.setRecommendations(recommendations);
        // Generic name only: the original file name may contain the user's name
        session.setVideoFileReference("video_" + System.currentTimeMillis());
        session.setConsentGiven(consentGiven);

        awarenessSessionRepository.save(session);

        return new VideoEvaluationResponse(session.getId(), service.getId(), service.getTitle(), verdict, score,
                result.getSummary(), result.getCorrectSteps(), result.getMistakes(), result.getRecommendations(), result.getSafetyNote());
    }

    public List<AwarenessSessionResponse> getAllSessions() {
        List<AwarenessSession> sessions = awarenessSessionRepository.findAll();
        List<AwarenessSessionResponse> responses = new ArrayList<>();

        for (AwarenessSession s : sessions) {
            responses.add(new AwarenessSessionResponse(s.getId(), s.getAwarenessService().getId(), s.getAwarenessService().getTitle(),
                    s.getStartedAt(), s.getCompletedAt(), s.getScore(), s.getResultSummary(), s.getRecommendations(),
                    s.getVideoFileReference(), s.getConsentGiven()));
        }
        return responses;
    }

    public AwarenessSessionResponse getSessionById(Long id) {
        AwarenessSession s = awarenessSessionRepository.findAwarenessSessionById(id);
        if (s == null) {
            throw new ApiException("Session not found");
        }
        return new AwarenessSessionResponse(s.getId(), s.getAwarenessService().getId(), s.getAwarenessService().getTitle(),
                s.getStartedAt(), s.getCompletedAt(), s.getScore(), s.getResultSummary(), s.getRecommendations(),
                s.getVideoFileReference(), s.getConsentGiven());
    }

    public List<AwarenessSessionResponse> getSessionsByService(Long serviceId) {
        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(serviceId);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }

        List<AwarenessSession> sessions = awarenessSessionRepository.findAwarenessSessionsByAwarenessService(service);
        List<AwarenessSessionResponse> responses = new ArrayList<>();

        for (AwarenessSession s : sessions) {
            responses.add(new AwarenessSessionResponse(s.getId(), service.getId(), service.getTitle(),
                    s.getStartedAt(), s.getCompletedAt(), s.getScore(), s.getResultSummary(), s.getRecommendations(),
                    s.getVideoFileReference(), s.getConsentGiven()));
        }
        return responses;
    }

    public ServiceStatsResponse getServiceStats(Long serviceId) {
        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(serviceId);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }

        int attempts = awarenessSessionRepository.findAwarenessSessionsByAwarenessService(service).size();
        Double average = awarenessSessionRepository.getAverageScore(serviceId);
        if (average == null) {
            average = 0.0;
        }

        return new ServiceStatsResponse(service.getId(), service.getTitle(), attempts, average,
                service.getTitle() + " has " + attempts + " attempts with an average score of " + average);
    }

    public void deleteSession(Long id) {
        AwarenessSession session = awarenessSessionRepository.findAwarenessSessionById(id);
        if (session == null) {
            throw new ApiException("Session not found");
        }
        awarenessSessionRepository.delete(session);
    }
}

package org.fadhel.waeiplatform.service;


import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiEvaluationResult;
import org.fadhel.waeiplatform.dto.ai.AiQuizQuestion;
import org.fadhel.waeiplatform.dto.in.QuizSubmitRequest;
import org.fadhel.waeiplatform.dto.out.*;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.model.AwarenessSession;
import org.fadhel.waeiplatform.model.SessionQuizQuestion;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.fadhel.waeiplatform.repository.AwarenessSessionRepository;
import org.fadhel.waeiplatform.repository.SessionQuizQuestionRepository;
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
    private final SessionQuizQuestionRepository sessionQuizQuestionRepository;
    private final GeminiService geminiService;

    // User uploads a video -> AI evaluates it -> we save ONLY the result (no video, no personal data)
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

        // Video does not show the training -> tell the user why, and do NOT save the attempt
        if (result.getRelevant() == null || !result.getRelevant()) {
            String reason = result.getRejectionReason();
            if (reason == null || reason.isBlank()) {
                reason = "The video does not show this training";
            }
            throw new ApiException(reason + " - please upload a clear video of yourself practicing \"" + service.getTitle() + "\"");
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

        // Only the evaluation is saved: the video itself is never stored
        AwarenessSession session = new AwarenessSession();
        session.setAwarenessService(service);
        session.setStartedAt(startedAt);
        session.setCompletedAt(LocalDateTime.now());
        session.setScore(score);
        session.setResultSummary(result.getSummary());
        session.setRecommendations(recommendations);
        session.setConsentGiven(consentGiven);

        awarenessSessionRepository.save(session);

        // Save the 3 quiz questions (we keep the correct answer on the server, the user does not see it)
        List<QuizQuestionResponse> quiz = new ArrayList<>();
        if (result.getQuiz() != null) {
            int count = 0;
            for (AiQuizQuestion q : result.getQuiz()) {
                if (count == 3) {
                    break;
                }
                // Skip any broken question from the AI
                if (q.getQuestion() == null || q.getOptionA() == null || q.getOptionB() == null || q.getOptionC() == null
                        || q.getCorrectAnswer() == null || !q.getCorrectAnswer().matches("A|B|C")) {
                    continue;
                }
                SessionQuizQuestion question = new SessionQuizQuestion();
                question.setQuestion(q.getQuestion());
                question.setOptionA(cut(q.getOptionA(), 255));
                question.setOptionB(cut(q.getOptionB(), 255));
                question.setOptionC(cut(q.getOptionC(), 255));
                question.setCorrectAnswer(q.getCorrectAnswer());
                question.setExplanation(q.getExplanation());
                question.setAwarenessSession(session);
                sessionQuizQuestionRepository.save(question);

                quiz.add(new QuizQuestionResponse(question.getId(), question.getQuestion(),
                        question.getOptionA(), question.getOptionB(), question.getOptionC()));
                count++;
            }
        }

        return new VideoEvaluationResponse(session.getId(), service.getId(), service.getTitle(), verdict, score,
                result.getSummary(), result.getCorrectSteps(), result.getMistakes(), result.getRecommendations(),
                result.getSafetyNote(), service.getOfficialVideoUrl(), quiz);
    }

    // Get the quiz again (for example if the page was refreshed)
    public List<QuizQuestionResponse> getQuiz(Long sessionId) {
        AwarenessSession session = awarenessSessionRepository.findAwarenessSessionById(sessionId);
        if (session == null) {
            throw new ApiException("Session not found");
        }

        List<SessionQuizQuestion> questions = sessionQuizQuestionRepository.findSessionQuizQuestionsByAwarenessSessionOrderByIdAsc(session);
        if (questions.isEmpty()) {
            throw new ApiException("This session has no quiz");
        }

        List<QuizQuestionResponse> responses = new ArrayList<>();
        for (SessionQuizQuestion q : questions) {
            responses.add(new QuizQuestionResponse(q.getId(), q.getQuestion(), q.getOptionA(), q.getOptionB(), q.getOptionC()));
        }
        return responses;
    }

    // The trainee sends the 3 answers -> we check them and show the explanation of each one
    public QuizResultResponse submitQuiz(Long sessionId, QuizSubmitRequest request) {
        AwarenessSession session = awarenessSessionRepository.findAwarenessSessionById(sessionId);
        if (session == null) {
            throw new ApiException("Session not found");
        }
        if (session.getQuizScore() != null) {
            throw new ApiException("You already answered this quiz");
        }

        List<SessionQuizQuestion> questions = sessionQuizQuestionRepository.findSessionQuizQuestionsByAwarenessSessionOrderByIdAsc(session);
        if (questions.isEmpty()) {
            throw new ApiException("This session has no quiz");
        }
        if (request.getAnswers().size() != questions.size()) {
            throw new ApiException("You must answer all " + questions.size() + " questions");
        }

        int correctCount = 0;
        List<QuizAnswerResult> answers = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            SessionQuizQuestion q = questions.get(i);
            String answer = request.getAnswers().get(i);

            boolean isCorrect = q.getCorrectAnswer().equals(answer);
            if (isCorrect) {
                correctCount++;
            }

            q.setUserAnswer(answer);
            sessionQuizQuestionRepository.save(q);

            answers.add(new QuizAnswerResult(q.getId(), q.getQuestion(), answer, q.getCorrectAnswer(), isCorrect, q.getExplanation()));
        }

        session.setQuizScore(correctCount);
        awarenessSessionRepository.save(session);

        String message;
        if (correctCount == questions.size()) {
            message = "Excellent! You answered all questions correctly";
        } else if (correctCount > 0) {
            message = "Good job, read the explanations to learn from your mistakes";
        } else {
            message = "Read the explanations and watch the official video, then try the training again";
        }

        return new QuizResultResponse(session.getId(), correctCount, questions.size(), message, answers);
    }

    public List<AwarenessSessionResponse> getAllSessions() {
        List<AwarenessSession> sessions = awarenessSessionRepository.findAll();
        List<AwarenessSessionResponse> responses = new ArrayList<>();

        for (AwarenessSession s : sessions) {
            responses.add(toResponse(s));
        }
        return responses;
    }

    public AwarenessSessionResponse getSessionById(Long id) {
        AwarenessSession s = awarenessSessionRepository.findAwarenessSessionById(id);
        if (s == null) {
            throw new ApiException("Session not found");
        }
        return toResponse(s);
    }

    public List<AwarenessSessionResponse> getSessionsByService(Long serviceId) {
        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(serviceId);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }

        List<AwarenessSession> sessions = awarenessSessionRepository.findAwarenessSessionsByAwarenessService(service);
        List<AwarenessSessionResponse> responses = new ArrayList<>();

        for (AwarenessSession s : sessions) {
            responses.add(toResponse(s));
        }
        return responses;
    }

    // Admin: attempts + average score for one training (no trainee identity)
    public ServiceStatsResponse getServiceStats(Long serviceId) {
        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(serviceId);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }
        return buildStats(service);
    }

    // Admin dashboard: stats for ALL trainings in one request
    public List<ServiceStatsResponse> getAllServicesStats() {
        List<AwarenessService> services = awarenessServiceRepository.findAll();
        List<ServiceStatsResponse> responses = new ArrayList<>();

        for (AwarenessService service : services) {
            responses.add(buildStats(service));
        }
        return responses;
    }

    public void deleteSession(Long id) {
        AwarenessSession session = awarenessSessionRepository.findAwarenessSessionById(id);
        if (session == null) {
            throw new ApiException("Session not found");
        }
        awarenessSessionRepository.delete(session);
    }

    private ServiceStatsResponse buildStats(AwarenessService service) {
        int attempts = awarenessSessionRepository.findAwarenessSessionsByAwarenessService(service).size();

        Double average = awarenessSessionRepository.getAverageScore(service.getId());
        if (average == null) {
            average = 0.0;
        }
        average = Math.round(average * 10) / 10.0;

        Double averageQuiz = awarenessSessionRepository.getAverageQuizScore(service.getId());
        if (averageQuiz == null) {
            averageQuiz = 0.0;
        }
        averageQuiz = Math.round(averageQuiz * 10) / 10.0;

        return new ServiceStatsResponse(service.getId(), service.getTitle(), attempts, average, averageQuiz,
                service.getTitle() + " has " + attempts + " attempts with an average score of " + average);
    }

    // Makes sure the AI text fits in the database column
    private String cut(String text, int max) {
        if (text.length() > max) {
            return text.substring(0, max);
        }
        return text;
    }

    private AwarenessSessionResponse toResponse(AwarenessSession s) {
        return new AwarenessSessionResponse(s.getId(), s.getAwarenessService().getId(), s.getAwarenessService().getTitle(),
                s.getStartedAt(), s.getCompletedAt(), s.getScore(), s.getResultSummary(), s.getRecommendations(),
                s.getQuizScore(), s.getConsentGiven());
    }
}

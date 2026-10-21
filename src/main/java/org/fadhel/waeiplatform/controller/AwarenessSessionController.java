package org.fadhel.waeiplatform.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.QuizSubmitRequest;
import org.fadhel.waeiplatform.service.AwarenessSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/awareness-session")
@RequiredArgsConstructor
public class AwarenessSessionController {

    private final AwarenessSessionService awarenessSessionService;

    // Postman: Body -> form-data -> video (File) + consentGiven (Text: true) + ageConfirmed (Text: true)
    @PostMapping("/evaluate/{serviceId}")
    public ResponseEntity<?> evaluateVideo(@PathVariable Long serviceId,
                                           @RequestParam MultipartFile video,
                                           @RequestParam Boolean consentGiven,
                                           @RequestParam Boolean ageConfirmed) {
        return ResponseEntity.status(200).body(awarenessSessionService.evaluateVideo(serviceId, video, consentGiven, ageConfirmed));
    }

    // Get the 3 quiz questions of a session (without the correct answers)
    @GetMapping("/quiz/{sessionId}")
    public ResponseEntity<?> getQuiz(@PathVariable Long sessionId) {
        return ResponseEntity.status(200).body(awarenessSessionService.getQuiz(sessionId));
    }

    // Body: { "answers": ["A", "C", "B"] }
    @PostMapping("/quiz/submit/{sessionId}")
    public ResponseEntity<?> submitQuiz(@PathVariable Long sessionId, @RequestBody @Valid QuizSubmitRequest request) {
        return ResponseEntity.status(200).body(awarenessSessionService.submitQuiz(sessionId, request));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllSessions() {
        return ResponseEntity.status(200).body(awarenessSessionService.getAllSessions());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(awarenessSessionService.getSessionById(id));
    }

    @GetMapping("/get-by-service/{serviceId}")
    public ResponseEntity<?> getSessionsByService(@PathVariable Long serviceId) {
        return ResponseEntity.status(200).body(awarenessSessionService.getSessionsByService(serviceId));
    }

    @GetMapping("/stats/{serviceId}")
    public ResponseEntity<?> getServiceStats(@PathVariable Long serviceId) {
        return ResponseEntity.status(200).body(awarenessSessionService.getServiceStats(serviceId));
    }

    // Admin dashboard: attempts + average score for every training
    @GetMapping("/stats")
    public ResponseEntity<?> getAllServicesStats() {
        return ResponseEntity.status(200).body(awarenessSessionService.getAllServicesStats());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        awarenessSessionService.deleteSession(id);
        return ResponseEntity.status(200).body(new ApiResponse("Session deleted successfully"));
    }
}


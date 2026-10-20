package org.fadhel.waeiplatform.controller;


import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
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

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        awarenessSessionService.deleteSession(id);
        return ResponseEntity.status(200).body(new ApiResponse("Session deleted successfully"));
    }
}


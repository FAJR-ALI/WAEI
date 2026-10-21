package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.KidsStoryGenerateRequest;
import org.fadhel.waeiplatform.dto.in.KidsStoryUpdateRequest;
import org.fadhel.waeiplatform.service.KidsStoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Admin only: generate stories with AI and review them before publishing
@RestController
@RequestMapping("/api/v1/kids-story")
@RequiredArgsConstructor
public class KidsStoryController {

    private final KidsStoryService kidsStoryService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateStory(@RequestBody @Valid KidsStoryGenerateRequest request) {
        return ResponseEntity.status(200).body(kidsStoryService.generateStory(request));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllStories() {
        return ResponseEntity.status(200).body(kidsStoryService.getAllStories());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getStoryById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(kidsStoryService.getStoryById(id));
    }

    // Example: /get-by-status/PENDING
    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<?> getStoriesByStatus(@PathVariable String status) {
        return ResponseEntity.status(200).body(kidsStoryService.getStoriesByStatus(status));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateStory(@PathVariable Long id, @RequestBody @Valid KidsStoryUpdateRequest request) {
        kidsStoryService.updateStory(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Story updated, it needs review again"));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveStory(@PathVariable Long id) {
        kidsStoryService.approveStory(id);
        return ResponseEntity.status(200).body(new ApiResponse("Story approved and published"));
    }

    // Postman: /reject/1?note=القصة فيها تفاصيل مخيفة
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectStory(@PathVariable Long id, @RequestParam String note) {
        kidsStoryService.rejectStory(id, note);
        return ResponseEntity.status(200).body(new ApiResponse("Story rejected"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteStory(@PathVariable Long id) {
        kidsStoryService.deleteStory(id);
        return ResponseEntity.status(200).body(new ApiResponse("Story deleted successfully"));
    }
}

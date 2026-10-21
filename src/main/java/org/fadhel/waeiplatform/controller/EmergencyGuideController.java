package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.EmergencyGuideRequest;
import org.fadhel.waeiplatform.dto.in.GuideSearchRequest;
import org.fadhel.waeiplatform.service.EmergencyGuideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/guide")
@RequiredArgsConstructor
public class EmergencyGuideController {

    private final EmergencyGuideService emergencyGuideService;

    // ===================== Visitor (no login) =====================

    @GetMapping("/published")
    public ResponseEntity<?> getPublishedGuides() {
        return ResponseEntity.status(200).body(emergencyGuideService.getPublishedGuides());
    }

    @GetMapping("/published/{id}")
    public ResponseEntity<?> getPublishedGuideById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(emergencyGuideService.getPublishedGuideById(id));
    }

    // Example: /category/FIRST_AID
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getPublishedGuidesByCategory(@PathVariable String category) {
        return ResponseEntity.status(200).body(emergencyGuideService.getPublishedGuidesByCategory(category));
    }

    @PostMapping("/smart-search")
    public ResponseEntity<?> smartSearch(@RequestBody @Valid GuideSearchRequest request) {
        return ResponseEntity.status(200).body(emergencyGuideService.smartSearch(request));
    }

    // ===================== Admin =====================

    @GetMapping("/get")
    public ResponseEntity<?> getAllGuides() {
        return ResponseEntity.status(200).body(emergencyGuideService.getAllGuides());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getGuideById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(emergencyGuideService.getGuideById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addGuide(@RequestBody @Valid EmergencyGuideRequest request) {
        emergencyGuideService.addGuide(request);
        return ResponseEntity.status(200).body(new ApiResponse("Emergency case added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateGuide(@PathVariable Long id, @RequestBody @Valid EmergencyGuideRequest request) {
        emergencyGuideService.updateGuide(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Emergency case updated successfully"));
    }

    @PutMapping("/change-publish/{id}")
    public ResponseEntity<?> changePublishStatus(@PathVariable Long id) {
        emergencyGuideService.changePublishStatus(id);
        return ResponseEntity.status(200).body(new ApiResponse("Publish status changed successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteGuide(@PathVariable Long id) {
        emergencyGuideService.deleteGuide(id);
        return ResponseEntity.status(200).body(new ApiResponse("Emergency case deleted successfully"));
    }
}

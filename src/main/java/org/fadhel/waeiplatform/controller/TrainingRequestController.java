package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.TrainingRequestRequest;
import org.fadhel.waeiplatform.service.TrainingRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training-request")
@RequiredArgsConstructor
public class TrainingRequestController {

    private final TrainingRequestService trainingRequestService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllRequests() {
        return ResponseEntity.status(200).body(trainingRequestService.getAllRequests());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(trainingRequestService.getRequestById(id));
    }

    @GetMapping("/get-by-organization/{organizationId}")
    public ResponseEntity<?> getRequestsByOrganization(@PathVariable Long organizationId) {
        return ResponseEntity.status(200).body(trainingRequestService.getRequestsByOrganization(organizationId));
    }

    @GetMapping("/get-by-account/{accountId}")
    public ResponseEntity<?> getRequestsByAccount(@PathVariable Long accountId) {
        return ResponseEntity.status(200).body(trainingRequestService.getRequestsByAccount(accountId));
    }

    @GetMapping("/get-by-program/{programId}")
    public ResponseEntity<?> getRequestsByProgram(@PathVariable Long programId) {
        return ResponseEntity.status(200).body(trainingRequestService.getRequestsByProgram(programId));
    }

    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<?> getRequestsByStatus(@PathVariable String status) {
        return ResponseEntity.status(200).body(trainingRequestService.getRequestsByStatus(status));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addRequest(@Valid @RequestBody TrainingRequestRequest request) {
        trainingRequestService.addRequest(request);
        return ResponseEntity.status(200).body(new ApiResponse("Training request added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @Valid @RequestBody TrainingRequestRequest request) {
        trainingRequestService.updateRequest(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Training request updated successfully"));
    }

    @PutMapping("/change-status/{id}")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestParam String status) {
        trainingRequestService.changeStatus(id, status);
        return ResponseEntity.status(200).body(new ApiResponse("Training request status changed successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        trainingRequestService.deleteRequest(id);
        return ResponseEntity.status(200).body(new ApiResponse("Training request deleted successfully"));
    }
}

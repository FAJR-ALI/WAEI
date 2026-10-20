package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.TrainingProgramRequest;
import org.fadhel.waeiplatform.service.TrainingProgramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training-program")
@RequiredArgsConstructor
public class TrainingProgramController {

    private final TrainingProgramService trainingProgramService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllPrograms() {
        return ResponseEntity.status(200).body(trainingProgramService.getAllPrograms());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getProgramById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(trainingProgramService.getProgramById(id));
    }

    @GetMapping("/get-active")
    public ResponseEntity<?> getActivePrograms() {
        return ResponseEntity.status(200).body(trainingProgramService.getActivePrograms());
    }

    @GetMapping("/get-by-category/{category}")
    public ResponseEntity<?> getProgramsByCategory(@PathVariable String category) {
        return ResponseEntity.status(200).body(trainingProgramService.getProgramsByCategory(category));
    }

    @GetMapping("/get-by-organization/{organizationId}")
    public ResponseEntity<?> getProgramsByOrganization(@PathVariable Long organizationId) {
        return ResponseEntity.status(200).body(trainingProgramService.getProgramsByOrganization(organizationId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProgram(@Valid @RequestBody TrainingProgramRequest request) {
        trainingProgramService.addProgram(request);
        return ResponseEntity.status(200).body(new ApiResponse("Training program added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProgram(@PathVariable Long id, @Valid @RequestBody TrainingProgramRequest request) {
        trainingProgramService.updateProgram(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Training program updated successfully"));
    }

    @PutMapping("/change-status/{id}")
    public ResponseEntity<?> changeActiveStatus(@PathVariable Long id) {
        trainingProgramService.changeActiveStatus(id);
        return ResponseEntity.status(200).body(new ApiResponse("Training program status changed successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProgram(@PathVariable Long id) {
        trainingProgramService.deleteProgram(id);
        return ResponseEntity.status(200).body(new ApiResponse("Training program deleted successfully"));
    }
}

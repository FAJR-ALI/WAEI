package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.TrainingProgramRequest;
import org.fadhel.waeiplatform.service.TrainingProgramCategories;
import org.fadhel.waeiplatform.service.TrainingProgramService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.status(200).body(TrainingProgramCategories.all());
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
        return ResponseEntity.status(200).body(trainingProgramService.addProgram(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProgram(@PathVariable Long id, @Valid @RequestBody TrainingProgramRequest request) {
        return ResponseEntity.status(200).body(trainingProgramService.updateProgram(id, request));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getProgramImage(@PathVariable Long id) {
        TrainingProgramService.ImageDownload download = trainingProgramService.getImage(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .cacheControl(CacheControl.noCache())
                .body(download.resource());
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProgramImage(@PathVariable Long id,
                                                @RequestParam("image") MultipartFile image) {
        return ResponseEntity.status(200).body(trainingProgramService.uploadImage(id, image));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<?> removeProgramImage(@PathVariable Long id) {
        return ResponseEntity.status(200).body(trainingProgramService.removeImage(id));
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

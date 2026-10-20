package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.AwarenessServiceRequest;
import org.fadhel.waeiplatform.service.AwarenessServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/awareness-service")
@RequiredArgsConstructor
public class AwarenessServiceController {

    private final AwarenessServiceService awarenessServiceService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllServices() {
        return ResponseEntity.status(200).body(awarenessServiceService.getAllServices());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getServiceById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(awarenessServiceService.getServiceById(id));
    }

    @GetMapping("/get-active")
    public ResponseEntity<?> getActiveServices() {
        return ResponseEntity.status(200).body(awarenessServiceService.getActiveServices());
    }

    @GetMapping("/get-by-category/{category}")
    public ResponseEntity<?> getServicesByCategory(@PathVariable String category) {
        return ResponseEntity.status(200).body(awarenessServiceService.getServicesByCategory(category));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addService(@RequestBody @Valid AwarenessServiceRequest request) {
        awarenessServiceService.addService(request);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness service added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody @Valid AwarenessServiceRequest request) {
        awarenessServiceService.updateService(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness service updated successfully"));
    }

    @PutMapping("/change-status/{id}")
    public ResponseEntity<?> changeActiveStatus(@PathVariable Long id) {
        awarenessServiceService.changeActiveStatus(id);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness service status changed successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        awarenessServiceService.deleteService(id);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness service deleted successfully"));
    }
}

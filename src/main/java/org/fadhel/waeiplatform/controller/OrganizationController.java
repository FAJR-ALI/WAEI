package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.OrganizationDTOIn;
import org.fadhel.waeiplatform.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    // GET ALL ORGANIZATIONS
    @GetMapping("/get")
    public ResponseEntity<?> getAllOrganizations() {
        return ResponseEntity.status(200).body(organizationService.getAllOrganizations());
    }

    // GET ORGANIZATION BY ID
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getOrganizationById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(organizationService.getOrganizationById(id));
    }

    // ADD ORGANIZATION
    @PostMapping("/add")
    public ResponseEntity<?> addOrganization(@Valid @RequestBody OrganizationDTOIn dto) {
        organizationService.addOrganization(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Organization added successfully"));
    }

    // UPDATE ORGANIZATION
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Integer id, @Valid @RequestBody OrganizationDTOIn dto) {
        organizationService.updateOrganization(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Organization updated successfully"));
    }

    // DELETE ORGANIZATION
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Integer id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.status(200).body(new ApiResponse("Organization deleted successfully"));
    }
}

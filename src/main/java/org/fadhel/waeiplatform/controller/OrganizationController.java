package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.OrganizationDTOIn;
import org.fadhel.waeiplatform.dto.in.OrganizationRegistrationRequest;
import org.fadhel.waeiplatform.dto.in.OrganizationResubmitRequest;
import org.fadhel.waeiplatform.dto.in.RejectOrganizationRequest;
import org.fadhel.waeiplatform.model.OrganizationVerificationStatus;
import org.fadhel.waeiplatform.service.OrganizationService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllOrganizations() {
        return ResponseEntity.status(200).body(organizationService.getAllOrganizations());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(organizationService.getOrganizationById(id));
    }

    @GetMapping("/registrations")
    public ResponseEntity<?> getRegistrations(
            @RequestParam(defaultValue = "PENDING") OrganizationVerificationStatus status) {
        return ResponseEntity.status(200).body(organizationService.getRegistrations(status));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerOrganization(
            @Valid @ModelAttribute OrganizationRegistrationRequest request) {
        return ResponseEntity.status(200).body(organizationService.registerOrganization(request));
    }

    @PostMapping(value = "/{id}/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> resubmitOrganization(
            @PathVariable Long id,
            @Valid @ModelAttribute OrganizationResubmitRequest request) {
        return ResponseEntity.status(200).body(organizationService.resubmitRegistration(id, request));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verifyOrganization(@PathVariable Long id) {
        return ResponseEntity.status(200).body(organizationService.verifyOrganization(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectOrganization(@PathVariable Long id,
                                                @Valid @RequestBody RejectOrganizationRequest request) {
        return ResponseEntity.status(200).body(organizationService.rejectOrganization(id, request.getReason()));
    }

    @GetMapping("/{id}/document")
    public ResponseEntity<Resource> downloadOrganizationDocument(@PathVariable Long id) {
        OrganizationService.DocumentDownload download = organizationService.getOrganizationDocument(id);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.resource());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @Valid @RequestBody OrganizationDTOIn dto) {
        organizationService.updateOrganization(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Organization updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.status(200).body(new ApiResponse("Organization deleted successfully"));
    }
}

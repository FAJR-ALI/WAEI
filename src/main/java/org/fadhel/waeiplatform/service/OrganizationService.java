package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.OrganizationDTOIn;
import org.fadhel.waeiplatform.dto.out.OrganizationDTOOut;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    // CONVERT ENTITY TO DTO OUT
    private OrganizationDTOOut convertToDTO(Organization organization) {

        OrganizationDTOOut dto = new OrganizationDTOOut();

        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setType(organization.getType());
        dto.setSector(organization.getSector());
        dto.setEmail(organization.getEmail());
        dto.setPhoneNumber(organization.getPhoneNumber());
        dto.setVerificationStatus(organization.getVerificationStatus());
        dto.setCreatedAt(organization.getCreatedAt());
        dto.setDocumentReference(organization.getDocumentReference());

        return dto;
    }

    // GET ALL
    public List<OrganizationDTOOut> getAllOrganizations() {
        return organizationRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    // GET BY ID
    public OrganizationDTOOut getOrganizationById(Long id) {
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new ApiException("Organization not found"));
        return convertToDTO(organization);
    }

    // ADD
    public void addOrganization(OrganizationDTOIn dto) {
        Organization existingOrganization = organizationRepository.findOrganizationByEmail(dto.getEmail());
        if (existingOrganization != null) {
            throw new ApiException("Organization email already exists");
        }

        Organization organization = new Organization();

        organization.setName(dto.getName());
        organization.setType(dto.getType());
        organization.setSector(dto.getSector());
        organization.setEmail(dto.getEmail());
        organization.setPhoneNumber(dto.getPhoneNumber());
        organization.setDocumentReference(dto.getDocumentReference());
        organization.setVerificationStatus("PENDING");

        organizationRepository.save(organization);
    }

    // UPDATE
    public void updateOrganization(Long id, OrganizationDTOIn dto) {

        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new ApiException("Organization not found"));

        Organization existingOrganization = organizationRepository.findOrganizationByEmail(dto.getEmail());

        if (existingOrganization != null &&
                !existingOrganization.getId().equals(id)) {
            throw new ApiException("Organization email already exists");
        }

        organization.setName(dto.getName());
        organization.setType(dto.getType());
        organization.setSector(dto.getSector());
        organization.setEmail(dto.getEmail());
        organization.setPhoneNumber(dto.getPhoneNumber());
        organization.setDocumentReference(dto.getDocumentReference());

        organizationRepository.save(organization);
    }

    // DELETE
    public void deleteOrganization(Long id) {
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new ApiException("Organization not found"));
        organizationRepository.delete(organization);
    }
}
package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.TrainingProgramRequest;
import org.fadhel.waeiplatform.dto.out.TrainingProgramResponse;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.TrainingProgram;
import org.fadhel.waeiplatform.repository.OrganizationRepository;
import org.fadhel.waeiplatform.repository.TrainingProgramRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.fadhel.waeiplatform.security.AccessControl;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingProgramService {

    private final TrainingProgramRepository trainingProgramRepository;
    private final OrganizationRepository organizationRepository;
    private final TrainingRequestRepository trainingRequestRepository;
    private final CurrentUser currentUser;
    private final AccessControl accessControl;

    private TrainingProgramResponse convertToResponse(TrainingProgram program) {
        Organization organization = program.getOrganization();

        return new TrainingProgramResponse(
                program.getId(),
                program.getTitle(),
                program.getCategory(),
                program.getDescription(),
                program.getPrice(),
                program.getDeliveryMode(),
                program.getIsActive(),
                organization != null ? organization.getId() : null,
                organization != null ? organization.getName() : null);
    }

    public List<TrainingProgramResponse> getAllPrograms() {
        return trainingProgramRepository.findAll().stream().map(this::convertToResponse).toList();
    }

    public TrainingProgramResponse getProgramById(Long id) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));
        return convertToResponse(program);
    }

    public List<TrainingProgramResponse> getActivePrograms() {
        return trainingProgramRepository.findTrainingProgramsByIsActive(true).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingProgramResponse> getProgramsByCategory(String category) {
        return trainingProgramRepository.findTrainingProgramsByCategory(category).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingProgramResponse> getProgramsByOrganization(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException("Organization not found");
        }
        return trainingProgramRepository.findTrainingProgramsByOrganization_Id(organizationId).stream()
                .map(this::convertToResponse).toList();
    }

    public List<TrainingProgramResponse> getMyPrograms() {
        Long organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            return List.of();
        }
        return trainingProgramRepository.findTrainingProgramsByOrganization_Id(organizationId).stream()
                .map(this::convertToResponse).toList();
    }

    @Transactional
    public void addProgram(TrainingProgramRequest request) {
        Organization organization = resolveProviderOrganization(request.getOrganizationId());

        TrainingProgram existing = trainingProgramRepository
                .findTrainingProgramByTitleAndOrganization_Id(request.getTitle(), organization.getId());
        if (existing != null) {
            throw new ApiException("A training program with this title already exists for this organization");
        }

        TrainingProgram program = new TrainingProgram();
        applyFields(program, request, organization);

        trainingProgramRepository.save(program);
    }

    @Transactional
    public void updateProgram(Long id, TrainingProgramRequest request) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));

        accessControl.requireCanManageProgram(program);

        Organization organization;
        if (currentUser.isPlatformAdmin() && request.getOrganizationId() != null) {
            organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ApiException("Organization not found"));
            if (!accessControl.isProviderOrganization(organization)) {
                throw new ApiException("Training programs can only belong to a provider organization");
            }
        } else {
            organization = program.getOrganization();
        }

        TrainingProgram existing = trainingProgramRepository
                .findTrainingProgramByTitleAndOrganization_Id(request.getTitle(), organization.getId());
        if (existing != null && !existing.getId().equals(id)) {
            throw new ApiException("A training program with this title already exists for this organization");
        }

        applyFields(program, request, organization);

        trainingProgramRepository.save(program);
    }

    @Transactional
    public void changeActiveStatus(Long id) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));

        accessControl.requireCanManageProgram(program);

        program.setIsActive(!Boolean.TRUE.equals(program.getIsActive()));

        trainingProgramRepository.save(program);
    }

    @Transactional
    public void deleteProgram(Long id) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));

        accessControl.requireCanManageProgram(program);

        if (trainingRequestRepository.existsByTrainingProgram_Id(id)) {
            throw new ApiException("Cannot delete a training program that has training requests");
        }

        trainingProgramRepository.delete(program);
    }

    private Organization resolveProviderOrganization(Long requestedOrganizationId) {
        Long organizationId = requestedOrganizationId;

        if (!currentUser.isPlatformAdmin()) {
            organizationId = currentUser.getOrganizationId();
        }

        if (organizationId == null) {
            throw new ApiException("An organization is required to create a training program");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found"));

        if (!accessControl.isProviderOrganization(organization)) {
            throw new ApiException("Training programs can only be created by a provider organization");
        }

        return organization;
    }

    private void applyFields(TrainingProgram program, TrainingProgramRequest request, Organization organization) {
        program.setTitle(request.getTitle());
        program.setCategory(request.getCategory());
        program.setDescription(request.getDescription());
        program.setPrice(request.getPrice());
        program.setDeliveryMode(request.getDeliveryMode());
        program.setOrganization(organization);
        if (request.getIsActive() != null) {
            program.setIsActive(request.getIsActive());
        }
    }
}

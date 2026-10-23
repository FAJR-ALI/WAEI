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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ProgramImageStorageService programImageStorageService;

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
                organization != null ? organization.getName() : null,
                program.getHoursPerDay(),
                program.getMaxDays(),
                program.getImageStoredName() != null && !program.getImageStoredName().isBlank()
                        ? "/api/v1/training-program/" + program.getId() + "/image"
                        : null);
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
    public TrainingProgramResponse addProgram(TrainingProgramRequest request) {
        Organization organization = resolveProviderOrganization(request.getOrganizationId());

        TrainingProgram existing = trainingProgramRepository
                .findTrainingProgramByTitleAndOrganization_Id(request.getTitle(), organization.getId());
        if (existing != null) {
            throw new ApiException("A training program with this title already exists for this organization");
        }

        TrainingProgram program = new TrainingProgram();
        applyFields(program, request, organization);

        trainingProgramRepository.save(program);
        return convertToResponse(program);
    }

    @Transactional
    public TrainingProgramResponse updateProgram(Long id, TrainingProgramRequest request) {
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

        accessControl.requireVerifiedOrganization(organization);

        TrainingProgram existing = trainingProgramRepository
                .findTrainingProgramByTitleAndOrganization_Id(request.getTitle(), organization.getId());
        if (existing != null && !existing.getId().equals(id)) {
            throw new ApiException("A training program with this title already exists for this organization");
        }

        applyFields(program, request, organization);

        trainingProgramRepository.save(program);
        return convertToResponse(program);
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

        String imageStoredName = program.getImageStoredName();
        trainingProgramRepository.delete(program);
        programImageStorageService.deleteQuietly(imageStoredName);
    }

    @Transactional
    public TrainingProgramResponse uploadImage(Long id, MultipartFile image) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));

        accessControl.requireCanManageProgram(program);

        ProgramImageStorageService.StoredImage storedImage = programImageStorageService.store(image);
        String previousImage = program.getImageStoredName();

        program.setImageStoredName(storedImage.storedName());
        program.setImageContentType(storedImage.contentType());

        try {
            trainingProgramRepository.save(program);
        } catch (RuntimeException e) {
            programImageStorageService.deleteQuietly(storedImage.storedName());
            throw e;
        }

        programImageStorageService.deleteQuietly(previousImage);
        return convertToResponse(program);
    }

    @Transactional
    public TrainingProgramResponse removeImage(Long id) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));

        accessControl.requireCanManageProgram(program);

        String previousImage = program.getImageStoredName();
        program.setImageStoredName(null);
        program.setImageContentType(null);
        trainingProgramRepository.save(program);
        programImageStorageService.deleteQuietly(previousImage);
        return convertToResponse(program);
    }

    public ImageDownload getImage(Long id) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ApiException("Training program not found"));

        Resource resource = programImageStorageService.load(program.getImageStoredName());
        String contentType = program.getImageContentType() != null
                ? program.getImageContentType()
                : "application/octet-stream";
        return new ImageDownload(resource, contentType);
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

        accessControl.requireVerifiedOrganization(organization);

        return organization;
    }

    private void applyFields(TrainingProgram program, TrainingProgramRequest request, Organization organization) {
        String category = request.getCategory() == null ? null : request.getCategory().trim();
        if (!TrainingProgramCategories.isSupported(category)) {
            throw new ApiException("Invalid training program category");
        }

        program.setTitle(request.getTitle());
        program.setCategory(category);
        program.setDescription(request.getDescription());
        program.setPrice(request.getPrice());
        program.setHoursPerDay(request.getHoursPerDay());
        program.setMaxDays(request.getMaxDays());
        program.setDeliveryMode(request.getDeliveryMode());
        program.setOrganization(organization);
        if (request.getIsActive() != null) {
            program.setIsActive(request.getIsActive());
        }
    }

    public record ImageDownload(Resource resource, String contentType) {
    }
}

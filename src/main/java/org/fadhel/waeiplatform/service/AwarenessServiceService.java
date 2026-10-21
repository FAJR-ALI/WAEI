package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.AwarenessServiceRequest;
import org.fadhel.waeiplatform.dto.out.AwarenessServiceResponse;
import org.fadhel.waeiplatform.model.AwarenessCalendarEvent;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.repository.AwarenessCalendarEventRepository;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AwarenessServiceService {

    private final AwarenessServiceRepository awarenessServiceRepository;
    private final AwarenessCalendarEventRepository awarenessCalendarEventRepository;

    public List<AwarenessServiceResponse> getAllServices() {
        List<AwarenessService> services = awarenessServiceRepository.findAll();
        List<AwarenessServiceResponse> responses = new ArrayList<>();

        for (AwarenessService s : services) {
            responses.add(new AwarenessServiceResponse(s.getId(), s.getTitle(), s.getCategory(), s.getDescription(), s.getOfficialVideoUrl(), s.getIsActive()));
        }
        return responses;
    }

    public AwarenessServiceResponse getServiceById(Long id) {
        AwarenessService s = awarenessServiceRepository.findAwarenessServiceById(id);
        if (s == null) {
            throw new ApiException("Awareness service not found");
        }
        return new AwarenessServiceResponse(s.getId(), s.getTitle(), s.getCategory(), s.getDescription(), s.getOfficialVideoUrl(), s.getIsActive());
    }

    public List<AwarenessServiceResponse> getActiveServices() {
        List<AwarenessService> services = awarenessServiceRepository.findAwarenessServicesByIsActive(true);
        List<AwarenessServiceResponse> responses = new ArrayList<>();

        for (AwarenessService s : services) {
            responses.add(new AwarenessServiceResponse(s.getId(), s.getTitle(), s.getCategory(), s.getDescription(), s.getOfficialVideoUrl(), s.getIsActive()));
        }
        return responses;
    }

    public List<AwarenessServiceResponse> getServicesByCategory(String category) {
        List<AwarenessService> services = awarenessServiceRepository.findAwarenessServicesByCategory(category);
        List<AwarenessServiceResponse> responses = new ArrayList<>();

        for (AwarenessService s : services) {
            responses.add(new AwarenessServiceResponse(s.getId(), s.getTitle(), s.getCategory(), s.getDescription(), s.getOfficialVideoUrl(), s.getIsActive()));
        }
        return responses;
    }

    public void addService(AwarenessServiceRequest request) {
        if (awarenessServiceRepository.findAwarenessServiceByTitle(request.getTitle()) != null) {
            throw new ApiException("A service with this title already exists");
        }

        AwarenessService service = new AwarenessService();
        service.setTitle(request.getTitle());
        service.setCategory(request.getCategory());
        service.setDescription(request.getDescription());
        service.setOfficialVideoUrl(request.getOfficialVideoUrl());
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        awarenessServiceRepository.save(service);
    }

    public void updateService(Long id, AwarenessServiceRequest request) {
        AwarenessService oldService = awarenessServiceRepository.findAwarenessServiceById(id);
        if (oldService == null) {
            throw new ApiException("Awareness service not found");
        }

        oldService.setTitle(request.getTitle());
        oldService.setCategory(request.getCategory());
        oldService.setDescription(request.getDescription());
        oldService.setOfficialVideoUrl(request.getOfficialVideoUrl());
        if (request.getIsActive() != null) {
            oldService.setIsActive(request.getIsActive());
        }

        awarenessServiceRepository.save(oldService);
    }

    // Activate / deactivate the service without deleting it
    public void changeActiveStatus(Long id) {
        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(id);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }

        if (service.getIsActive()) {
            service.setIsActive(false);
        } else {
            service.setIsActive(true);
        }

        awarenessServiceRepository.save(service);
    }

    public void deleteService(Long id) {
        AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(id);
        if (service == null) {
            throw new ApiException("Awareness service not found");
        }

        // Calendar days that promote this training stay, they just lose the link
        List<AwarenessCalendarEvent> events = awarenessCalendarEventRepository.findAwarenessCalendarEventsByAwarenessService(service);
        for (AwarenessCalendarEvent event : events) {
            event.setAwarenessService(null);
            awarenessCalendarEventRepository.save(event);
        }

        awarenessServiceRepository.delete(service);
    }
}

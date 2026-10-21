package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiGuideMatch;
import org.fadhel.waeiplatform.dto.in.EmergencyGuideRequest;
import org.fadhel.waeiplatform.dto.in.GuideSearchRequest;
import org.fadhel.waeiplatform.dto.out.EmergencyGuideCardResponse;
import org.fadhel.waeiplatform.dto.out.EmergencyGuideResponse;
import org.fadhel.waeiplatform.dto.out.GuideMatchResponse;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.model.EmergencyGuide;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.fadhel.waeiplatform.repository.EmergencyGuideRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyGuideService {

    private final EmergencyGuideRepository emergencyGuideRepository;
    private final AwarenessServiceRepository awarenessServiceRepository;
    private final GeminiService geminiService;

    // ===================== Visitor =====================

    public List<EmergencyGuideCardResponse> getPublishedGuides() {
        return toCards(emergencyGuideRepository.findEmergencyGuidesByIsPublishedOrderByTitleAsc(true));
    }

    public List<EmergencyGuideCardResponse> getPublishedGuidesByCategory(String category) {
        if (!category.matches("FIRST_AID|KIDS|FIRE|CHRONIC|WEATHER")) {
            throw new ApiException("Category must be FIRST_AID, KIDS, FIRE, CHRONIC or WEATHER");
        }
        return toCards(emergencyGuideRepository.findEmergencyGuidesByIsPublishedAndCategoryOrderByTitleAsc(true, category));
    }

    public EmergencyGuideResponse getPublishedGuideById(Long id) {
        EmergencyGuide guide = emergencyGuideRepository.findEmergencyGuideById(id);
        if (guide == null || !guide.getIsPublished()) {
            throw new ApiException("Emergency case not found");
        }
        return toResponse(guide);
    }

    // The visitor describes what happened -> AI picks the closest PUBLISHED case
    public GuideMatchResponse smartSearch(GuideSearchRequest request) {
        List<EmergencyGuide> guides = emergencyGuideRepository.findEmergencyGuidesByIsPublishedOrderByTitleAsc(true);
        if (guides.isEmpty()) {
            throw new ApiException("The emergency guide is empty right now");
        }

        AiGuideMatch result = geminiService.matchGuide(request.getDescription(), guides);

        if (result.getGuideId() != null) {
            for (EmergencyGuide g : guides) {
                if (g.getId().equals(result.getGuideId())) {
                    return new GuideMatchResponse(true, g.getId(), g.getTitle(), g.getSummary(), g.getIsUrgent(), result.getReason());
                }
            }
        }

        return new GuideMatchResponse(false, null, null, null, false,
                "We could not find a matching case. If it is an emergency call 997 now.");
    }

    // ===================== Admin =====================

    public List<EmergencyGuideResponse> getAllGuides() {
        List<EmergencyGuide> guides = emergencyGuideRepository.findAll();
        List<EmergencyGuideResponse> responses = new ArrayList<>();
        for (EmergencyGuide g : guides) {
            responses.add(toResponse(g));
        }
        return responses;
    }

    public EmergencyGuideResponse getGuideById(Long id) {
        EmergencyGuide guide = emergencyGuideRepository.findEmergencyGuideById(id);
        if (guide == null) {
            throw new ApiException("Emergency case not found");
        }
        return toResponse(guide);
    }

    public void addGuide(EmergencyGuideRequest request) {
        if (emergencyGuideRepository.findEmergencyGuideByTitle(request.getTitle()) != null) {
            throw new ApiException("An emergency case with this title already exists");
        }
        EmergencyGuide guide = new EmergencyGuide();
        fillGuide(guide, request);
        emergencyGuideRepository.save(guide);
    }

    public void updateGuide(Long id, EmergencyGuideRequest request) {
        EmergencyGuide guide = emergencyGuideRepository.findEmergencyGuideById(id);
        if (guide == null) {
            throw new ApiException("Emergency case not found");
        }
        EmergencyGuide sameTitle = emergencyGuideRepository.findEmergencyGuideByTitle(request.getTitle());
        if (sameTitle != null && !sameTitle.getId().equals(id)) {
            throw new ApiException("An emergency case with this title already exists");
        }
        fillGuide(guide, request);
        emergencyGuideRepository.save(guide);
    }

    // Publish, hide without deleting
    public void changePublishStatus(Long id) {
        EmergencyGuide guide = emergencyGuideRepository.findEmergencyGuideById(id);
        if (guide == null) {
            throw new ApiException("Emergency case not found");
        }
        if (guide.getIsPublished()) {
            guide.setIsPublished(false);
        } else {
            guide.setIsPublished(true);
        }
        emergencyGuideRepository.save(guide);
    }

    public void deleteGuide(Long id) {
        EmergencyGuide guide = emergencyGuideRepository.findEmergencyGuideById(id);
        if (guide == null) {
            throw new ApiException("Emergency case not found");
        }
        emergencyGuideRepository.delete(guide);
    }

    // ===================== Helpers =====================

    private void fillGuide(EmergencyGuide guide, EmergencyGuideRequest request) {
        guide.setTitle(request.getTitle());
        guide.setCategory(request.getCategory());
        guide.setSummary(request.getSummary());
        guide.setSigns(request.getSigns());
        guide.setSteps(request.getSteps());
        guide.setDonts(request.getDonts());
        guide.setWhenToCall(request.getWhenToCall());
        guide.setSourceName(request.getSourceName());
        guide.setSourceUrl(request.getSourceUrl());

        if (request.getIcon() == null || request.getIcon().isBlank()) {
            guide.setIcon("HEART");
        } else {
            guide.setIcon(request.getIcon());
        }
        if (request.getIsUrgent() != null) {
            guide.setIsUrgent(request.getIsUrgent());
        }
        if (request.getIsPublished() != null) {
            guide.setIsPublished(request.getIsPublished());
        }

        if (request.getAwarenessServiceId() != null) {
            AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(request.getAwarenessServiceId());
            if (service == null) {
                throw new ApiException("Awareness service not found");
            }
            guide.setAwarenessService(service);
        } else {
            guide.setAwarenessService(null);
        }
    }

    private List<String> toList(String text) {
        List<String> items = new ArrayList<>();
        if (text == null) {
            return items;
        }
        for (String line : text.split("\n")) {
            String item = line.trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private boolean hasActiveTraining(EmergencyGuide g) {
        return g.getAwarenessService() != null && Boolean.TRUE.equals(g.getAwarenessService().getIsActive());
    }

    private List<EmergencyGuideCardResponse> toCards(List<EmergencyGuide> guides) {
        List<EmergencyGuideCardResponse> cards = new ArrayList<>();
        for (EmergencyGuide g : guides) {
            cards.add(new EmergencyGuideCardResponse(g.getId(), g.getTitle(), g.getCategory(), g.getSummary(),
                    g.getIsUrgent(), g.getIcon(), hasActiveTraining(g)));
        }
        return cards;
    }

    private EmergencyGuideResponse toResponse(EmergencyGuide g) {
        Long serviceId = null;
        String serviceTitle = null;
        if (hasActiveTraining(g)) {
            serviceId = g.getAwarenessService().getId();
            serviceTitle = g.getAwarenessService().getTitle();
        }
        return new EmergencyGuideResponse(g.getId(), g.getTitle(), g.getCategory(), g.getSummary(),
                toList(g.getSigns()), toList(g.getSteps()), toList(g.getDonts()), toList(g.getWhenToCall()),
                g.getIsUrgent(), g.getIcon(), g.getSourceName(), g.getSourceUrl(), g.getIsPublished(),
                serviceId, serviceTitle, g.getUpdatedAt());
    }
}

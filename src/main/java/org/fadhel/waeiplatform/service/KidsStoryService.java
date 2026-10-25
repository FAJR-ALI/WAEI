package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.KidsStoryPublicResponse;
import org.fadhel.waeiplatform.model.KidsStory;
import org.fadhel.waeiplatform.repository.KidsStoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KidsStoryService {

    private final KidsStoryRepository kidsStoryRepository;

    // ===================== Kids mode (public, APPROVED only) =====================

    public List<KidsStoryPublicResponse> getPublishedStories() {
        return toPublicList(kidsStoryRepository.findKidsStoriesByStatus("APPROVED"));
    }

    public List<KidsStoryPublicResponse> getPublishedStoriesByAge(String ageGroup) {
        if (!ageGroup.matches("3-5|6-8|9-12")) {
            throw new ApiException("Age group must be 3-5, 6-8 or 9-12");
        }
        return toPublicList(kidsStoryRepository.findKidsStoriesByStatusAndAgeGroup("APPROVED", ageGroup));
    }

    public List<KidsStoryPublicResponse> getPublishedStoriesByTopic(String topic) {
        return toPublicList(kidsStoryRepository.findKidsStoriesByStatusAndTopic("APPROVED", topic));
    }

    public KidsStoryPublicResponse getPublishedStoryById(Long id) {
        KidsStory story = kidsStoryRepository.findKidsStoryById(id);
        // Same message for "not found" and "not approved" so kids never see unreviewed stories
        if (story == null || !story.getStatus().equals("APPROVED")) {
            throw new ApiException("Story not found");
        }
        return toPublicResponse(story);
    }

    private List<KidsStoryPublicResponse> toPublicList(List<KidsStory> stories) {
        List<KidsStoryPublicResponse> responses = new ArrayList<>();
        for (KidsStory s : stories) {
            responses.add(toPublicResponse(s));
        }
        return responses;
    }

    private KidsStoryPublicResponse toPublicResponse(KidsStory s) {
        return new KidsStoryPublicResponse(s.getId(), s.getTitle(), s.getTopic(), s.getAgeGroup(), s.getContent(), s.getLesson());
    }

}

package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.ai.AiStoryResult;
import org.fadhel.waeiplatform.dto.in.KidsStoryGenerateRequest;
import org.fadhel.waeiplatform.dto.in.KidsStoryUpdateRequest;
import org.fadhel.waeiplatform.dto.out.KidsStoryPublicResponse;
import org.fadhel.waeiplatform.dto.out.KidsStoryResponse;
import org.fadhel.waeiplatform.model.KidsStory;
import org.fadhel.waeiplatform.repository.KidsStoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KidsStoryService {

    private final KidsStoryRepository kidsStoryRepository;
    private final GeminiService geminiService;

    // ===================== Admin =====================

    // AI writes the story -> saved as PENDING -> admin must approve it before kids can see it
    public KidsStoryResponse generateStory(KidsStoryGenerateRequest request) {
        AiStoryResult result = geminiService.generateKidsStory(request.getTopic(), request.getAgeGroup());

        if (result.getTitle() == null || result.getContent() == null || result.getContent().isBlank()) {
            throw new ApiException("AI could not write the story, please try again");
        }

        KidsStory story = new KidsStory();
        story.setTitle(cut(result.getTitle(), 150));
        story.setTopic(request.getTopic());
        story.setAgeGroup(request.getAgeGroup());
        story.setContent(result.getContent());
        if (result.getLesson() != null) {
            story.setLesson(cut(result.getLesson(), 255));
        }
        story.setStatus("PENDING");

        kidsStoryRepository.save(story);
        return toResponse(story);
    }

    public List<KidsStoryResponse> getAllStories() {
        List<KidsStory> stories = kidsStoryRepository.findAll();
        List<KidsStoryResponse> responses = new ArrayList<>();

        for (KidsStory s : stories) {
            responses.add(toResponse(s));
        }
        return responses;
    }

    // PENDING / APPROVED / REJECTED
    public List<KidsStoryResponse> getStoriesByStatus(String status) {
        if (!status.matches("PENDING|APPROVED|REJECTED")) {
            throw new ApiException("Status must be PENDING, APPROVED or REJECTED");
        }

        List<KidsStory> stories = kidsStoryRepository.findKidsStoriesByStatus(status);
        List<KidsStoryResponse> responses = new ArrayList<>();

        for (KidsStory s : stories) {
            responses.add(toResponse(s));
        }
        return responses;
    }

    public KidsStoryResponse getStoryById(Long id) {
        KidsStory story = kidsStoryRepository.findKidsStoryById(id);
        if (story == null) {
            throw new ApiException("Story not found");
        }
        return toResponse(story);
    }

    // Admin edits the text -> it goes back to PENDING so it is reviewed again
    public void updateStory(Long id, KidsStoryUpdateRequest request) {
        KidsStory story = kidsStoryRepository.findKidsStoryById(id);
        if (story == null) {
            throw new ApiException("Story not found");
        }

        story.setTitle(request.getTitle());
        story.setContent(request.getContent());
        story.setLesson(request.getLesson());
        story.setStatus("PENDING");
        story.setPublishedAt(null);

        kidsStoryRepository.save(story);
    }

    public void approveStory(Long id) {
        KidsStory story = kidsStoryRepository.findKidsStoryById(id);
        if (story == null) {
            throw new ApiException("Story not found");
        }
        if (story.getStatus().equals("APPROVED")) {
            throw new ApiException("Story is already approved");
        }

        story.setStatus("APPROVED");
        story.setAdminNote(null);
        story.setPublishedAt(LocalDateTime.now());

        kidsStoryRepository.save(story);
    }

    public void rejectStory(Long id, String note) {
        KidsStory story = kidsStoryRepository.findKidsStoryById(id);
        if (story == null) {
            throw new ApiException("Story not found");
        }
        if (note == null || note.isBlank()) {
            throw new ApiException("Please write the reason for rejecting the story");
        }
        if (note.length() > 255) {
            throw new ApiException("Reason must be at most 255 characters");
        }

        story.setStatus("REJECTED");
        story.setAdminNote(note);
        story.setPublishedAt(null);

        kidsStoryRepository.save(story);
    }

    public void deleteStory(Long id) {
        KidsStory story = kidsStoryRepository.findKidsStoryById(id);
        if (story == null) {
            throw new ApiException("Story not found");
        }
        kidsStoryRepository.delete(story);
    }

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

    // Makes sure the AI text fits in the database column
    private String cut(String text, int max) {
        if (text.length() > max) {
            return text.substring(0, max);
        }
        return text;
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

    private KidsStoryResponse toResponse(KidsStory s) {
        return new KidsStoryResponse(s.getId(), s.getTitle(), s.getTopic(), s.getAgeGroup(), s.getContent(), s.getLesson(),
                s.getStatus(), s.getAdminNote(), s.getCreatedAt(), s.getPublishedAt());
    }
}

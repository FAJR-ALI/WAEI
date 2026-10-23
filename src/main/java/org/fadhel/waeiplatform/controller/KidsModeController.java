package org.fadhel.waeiplatform.controller;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.service.KidsStoryService;
import org.fadhel.waeiplatform.service.KidsStoryNarrationService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Kids mode: public, read-only, approved stories only.
// No login, no video, no POST: we never receive or store anything about the child.
@RestController
@RequestMapping("/api/v1/kids")
@RequiredArgsConstructor
public class KidsModeController {

    private final KidsStoryService kidsStoryService;
    private final KidsStoryNarrationService kidsStoryNarrationService;

    @GetMapping(value = "/stories/{id}/narration", produces = "audio/wav")
    public ResponseEntity<byte[]> narrateStory(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(kidsStoryNarrationService.narrate(id));
    }

    @GetMapping("/stories")
    public ResponseEntity<?> getStories() {
        return ResponseEntity.status(200).body(kidsStoryService.getPublishedStories());
    }

    @GetMapping("/stories/{id}")
    public ResponseEntity<?> getStoryById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(kidsStoryService.getPublishedStoryById(id));
    }

    // Example: /stories/age/6-8
    @GetMapping("/stories/age/{ageGroup}")
    public ResponseEntity<?> getStoriesByAge(@PathVariable String ageGroup) {
        return ResponseEntity.status(200).body(kidsStoryService.getPublishedStoriesByAge(ageGroup));
    }

    @GetMapping("/stories/topic/{topic}")
    public ResponseEntity<?> getStoriesByTopic(@PathVariable String topic) {
        return ResponseEntity.status(200).body(kidsStoryService.getPublishedStoriesByTopic(topic));
    }
}

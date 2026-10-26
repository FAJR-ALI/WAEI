package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.dto.ai.AiStoryResult;
import org.fadhel.waeiplatform.service.KidsStoryBuilderService;
import org.fadhel.waeiplatform.service.KidsVoiceService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kids")
@RequiredArgsConstructor
public class KidsInteractionController {
    private final KidsVoiceService kidsVoiceService;
    private final KidsStoryBuilderService kidsStoryBuilderService;

    public record VoiceRequest(@NotBlank @Size(max = 2000) String text,
                               @NotBlank @Pattern(regexp = "ar|en") String lang,
                               @NotBlank @Pattern(regexp = "3-5|6-8|9-12") String ageGroup) {}
    public record StoryRequest(@NotBlank @Pattern(regexp = "3-5|6-8|9-12") String ageGroup,
                               @NotBlank @Pattern(regexp = "layan|saad|rabbit|cat") String character,
                               @NotBlank @Pattern(regexp = "home|school|park") String place,
                               @NotBlank @Pattern(regexp = "road|medicine|smoke|help") String topic,
                               @NotBlank @Pattern(regexp = "ar|en") String lang) {}

    @PostMapping("/voice")
    public ResponseEntity<byte[]> voice(@Valid @RequestBody VoiceRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType("audio/wav"))
                .body(kidsVoiceService.speak(request.text(), request.lang(), request.ageGroup()));
    }

    @PostMapping("/stories/create")
    public ResponseEntity<AiStoryResult> createStory(@Valid @RequestBody StoryRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(kidsStoryBuilderService.generate(
                request.ageGroup(), request.character(), request.place(), request.topic(), request.lang()));
    }
}

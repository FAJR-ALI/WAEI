package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.dto.out.KidsStoryPublicResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KidsStoryNarrationService {
    private final KidsStoryService kidsStoryService;
    private final GeminiSpeechService geminiSpeechService;

    // Bounded, process-local cache; publication is checked even on cache hits.
    private final Map<String, byte[]> audioCache = new LinkedHashMap<>(16, 0.75f, true);

    public synchronized byte[] narrate(Long id) {
        KidsStoryPublicResponse story = kidsStoryService.getPublishedStoryById(id);
        String text = story.getTitle() + ".\n" + story.getContent()
                + (story.getLesson() == null || story.getLesson().isBlank() ? "" : "\n" + story.getLesson());
        if (text.length() > 6000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This story is too long for narration");
        }
        String key = id + ":" + story.getAgeGroup() + ":" + text;
        byte[] cached = audioCache.get(key);
        if (cached != null) return cached;
        byte[] audio = geminiSpeechService.narrate(text, story.getAgeGroup());
        if (audioCache.size() >= 8) audioCache.remove(audioCache.keySet().iterator().next());
        audioCache.put(key, audio);
        return audio;
    }
}

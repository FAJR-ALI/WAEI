package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KidsVoiceService {
    private final GeminiSpeechService speechService;
    private final Map<String, byte[]> cache = new LinkedHashMap<>(8, 0.75f, true);

    public byte[] speak(String text, String lang, String ageGroup) {
        if (text == null || text.isBlank() || text.length() > 2000
                || !("ar".equals(lang) || "en".equals(lang))
                || !("3-5".equals(ageGroup) || "6-8".equals(ageGroup) || "9-12".equals(ageGroup))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid narration text or language");
        }
        String key = lang + ":" + ageGroup + ":" + text.trim();
        synchronized (cache) {
            byte[] existing = cache.get(key);
            if (existing != null) return existing;
        }
        byte[] audio = speechService.narrate(text.trim(), ageGroup, lang);
        synchronized (cache) {
            if (cache.size() >= 8) cache.remove(cache.keySet().iterator().next());
            cache.put(key, audio);
        }
        return audio;
    }
}

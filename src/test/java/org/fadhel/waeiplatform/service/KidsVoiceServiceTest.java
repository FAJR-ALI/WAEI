package org.fadhel.waeiplatform.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class KidsVoiceServiceTest {
    @Test
    void cachesByTextLanguageAndAgeAndRejectsInvalidInput() {
        var speech = mock(GeminiSpeechService.class);
        when(speech.narrate(anyString(), anyString(), anyString())).thenReturn(new byte[]{1, 2});
        var service = new KidsVoiceService(speech);
        assertArrayEquals(service.speak("Hello", "en", "6-8"), service.speak("Hello", "en", "6-8"));
        verify(speech, times(1)).narrate("Hello", "6-8", "en");
        service.speak("مرحبا", "ar", "3-5");
        verify(speech).narrate("مرحبا", "3-5", "ar");
        assertThrows(ResponseStatusException.class, () -> service.speak(" ", "ar", "6-8"));
        assertThrows(ResponseStatusException.class, () -> service.speak("x".repeat(2001), "ar", "6-8"));
        assertThrows(ResponseStatusException.class, () -> service.speak("Hello", "other", "6-8"));
    }
}

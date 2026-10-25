package org.fadhel.waeiplatform.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class GeminiSpeechErrorTest {
    @Test
    void preservesQuotaAndAccessErrorsInsteadOfHidingThemBehindBadGateway() {
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, GeminiSpeechService.providerFailure(429).getStatusCode());
        assertEquals("Speech quota exceeded", GeminiSpeechService.providerFailure(429).getReason());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, GeminiSpeechService.providerFailure(403).getStatusCode());
        assertEquals("Speech model is unavailable", GeminiSpeechService.providerFailure(404).getReason());
        assertEquals("Speech provider rejected the narration request", GeminiSpeechService.providerFailure(400).getReason());
        assertEquals(HttpStatus.BAD_GATEWAY, GeminiSpeechService.providerFailure(500).getStatusCode());
    }
}

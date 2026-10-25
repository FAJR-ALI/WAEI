package org.fadhel.waeiplatform.service;

import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class GeminiSpeechService {
    private static final Logger log = LoggerFactory.getLogger(GeminiSpeechService.class);
    @Value("${gemini.api.key:}")
    private String apiKey;
    @Value("${gemini.tts.model:gemini-3.8-flash-tts}")
    private String model;
    @Value("${gemini.tts.voice:Kore}")
    private String voice;

    public byte[] narrate(String text, String ageGroup) {
        return narrate(text, ageGroup, "ar");
    }

    public byte[] narrate(String text, String ageGroup, String language) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Story narrator is not configured");
        }
        try (Client client = Client.builder().apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(45000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            Part transcript = Part.builder().text(text)
                    .speechMetadata(SpeechMetadata.builder()
                            .style("Read the transcript verbatim in " + (language.equals("en") ? "clear English" : "clear Arabic")
                                    + " with a warm, gentle voice for children aged "
                                    + ageGroup + ". Speak slowly with natural pauses, without adding words.")
                            .build()).build();
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("AUDIO")
                    .speechConfig(SpeechConfig.builder()
                            .voiceConfig(VoiceConfig.builder().voice(voice).build()).build())
                    .build();
            GenerateContentResponse response = client.models.generateContent(model, Content.fromParts(transcript), config);
            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    Blob audio = part.inlineData().get();
                    return asWav(audio.data().orElseThrow(), audio.mimeType().orElse(""));
                }
            }
            throw new IllegalStateException("No audio returned");
        } catch (com.google.genai.errors.ApiException e) {
            log.warn("Speech provider failure: HTTP {} (model {})", e.code(), model);
            throw providerFailure(e.code());
        } catch (Exception e) {
            log.warn("Speech generation failed: {}", e.getClass().getSimpleName());
            // Never expose provider errors, credentials, or story content to the client.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Story narration is unavailable, please try again");
        }
    }

    static ResponseStatusException providerFailure(int code) {
        return switch (code) {
            case 429 -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Speech quota exceeded");
            case 401, 403 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Speech provider denied access");
            case 404 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Speech model is unavailable");
            case 400 -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Speech provider rejected the narration request");
            default -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Speech provider is unavailable");
        };
    }

    static byte[] asWav(byte[] data, String mimeType) {
        if (data.length == 0 || data.length > 8 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid audio size");
        }
        if (data.length >= 44 && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WAVE")) {
            return data;
        }
        String mime = mimeType.toLowerCase(Locale.ROOT);
        if (!(mime.startsWith("audio/l16") || mime.startsWith("audio/pcm")) || data.length % 2 != 0) {
            throw new IllegalArgumentException("Unsupported audio format");
        }
        int rate = 24000;
        for (String parameter : mime.split(";")) {
            if (parameter.trim().startsWith("rate=")) rate = Integer.parseInt(parameter.trim().substring(5));
        }
        if (rate < 8000 || rate > 48000) throw new IllegalArgumentException("Unsupported sample rate");
        // Gemini's PCM output is 16-bit little-endian mono.
        return ByteBuffer.allocate(44 + data.length).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + data.length)
                .put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16)
                .putShort((short) 1).putShort((short) 1).putInt(rate).putInt(rate * 2)
                .putShort((short) 2).putShort((short) 16)
                .put("data".getBytes(StandardCharsets.US_ASCII)).putInt(data.length).put(data).array();
    }
}

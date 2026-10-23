package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.out.KidsStoryPublicResponse;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class KidsStoryNarrationServiceTest {
    @Test
    void cachesAudioButRechecksPublicationAndRegeneratesAfterEdits() {
        KidsStoryService stories = mock(KidsStoryService.class);
        GeminiSpeechService speech = mock(GeminiSpeechService.class);
        KidsStoryPublicResponse story = KidsStoryPublicResponse.builder()
                .id(1L).title("Title").ageGroup("3-5").content("Story").lesson("Lesson").build();
        when(stories.getPublishedStoryById(1L)).thenReturn(story);
        when(speech.narrate(anyString(), anyString())).thenReturn(new byte[]{1, 2});
        KidsStoryNarrationService service = new KidsStoryNarrationService(stories, speech);
        assertArrayEquals(service.narrate(1L), service.narrate(1L));
        verify(speech, times(1)).narrate("Title.\nStory\nLesson", "3-5");
        story.setContent("Edited story");
        service.narrate(1L);
        verify(speech).narrate("Title.\nEdited story\nLesson", "3-5");
        when(stories.getPublishedStoryById(1L)).thenThrow(new ApiException("Story not found"));
        assertThrows(ApiException.class, () -> service.narrate(1L));
        verifyNoMoreInteractions(speech);
    }

    @Test
    void boundsCacheAndDoesNotCacheFailures() {
        KidsStoryService stories = mock(KidsStoryService.class);
        GeminiSpeechService speech = mock(GeminiSpeechService.class);
        when(stories.getPublishedStoryById(anyLong())).thenAnswer(call -> KidsStoryPublicResponse.builder()
                .id(call.getArgument(0)).title("Title").content("Story").ageGroup("6-8").build());
        when(speech.narrate(anyString(), anyString())).thenReturn(new byte[]{1, 2});
        KidsStoryNarrationService service = new KidsStoryNarrationService(stories, speech);
        for (long i = 1; i <= 9; i++) service.narrate(i);
        service.narrate(1L);
        verify(speech, times(10)).narrate(anyString(), anyString());
    }

    @Test
    void wrapsPcmInWavAndRejectsUnexpectedFormats() {
        byte[] pcm = {0, 0, 1, 0};
        byte[] wav = GeminiSpeechService.asWav(pcm, "audio/L16;codec=pcm;rate=24000");
        assertEquals(48, wav.length);
        ByteBuffer header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(24000, header.getInt(24));
        assertEquals(4, header.getInt(40));
        assertSame(wav, GeminiSpeechService.asWav(wav, "audio/wav"));
        assertThrows(IllegalArgumentException.class, () -> GeminiSpeechService.asWav(pcm, "text/plain"));
        assertThrows(IllegalArgumentException.class, () -> GeminiSpeechService.asWav(new byte[0], "audio/pcm"));
    }
}

package org.fadhel.waeiplatform.service;

import org.fadhel.waeiplatform.dto.ai.AiStoryResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class KidsStoryBuilderServiceTest {
    @Test
    void acceptsShortArabicStoriesAndRejectsMixedLanguagesAndTooManyLines() {
        var story = new AiStoryResult("ليان الشجاعة", "رأت ليان الدخان فابتعدت.\nنادت أمها وخرجتا بأمان.", "اطلب مساعدة شخص كبير.");
        assertDoesNotThrow(() -> KidsStoryBuilderService.validateStory(story, "ar"));
        story.setContent("رأت ليان smoke.");
        assertThrows(IllegalArgumentException.class, () -> KidsStoryBuilderService.validateStory(story, "ar"));
        story.setContent("سطر أول.\nسطر ثان.\nسطر ثالث.");
        assertThrows(IllegalArgumentException.class, () -> KidsStoryBuilderService.validateStory(story, "ar"));
        assertThrows(IllegalArgumentException.class, () -> KidsStoryBuilderService.validateStory(null, "ar"));
    }

    @Test
    void rejectsFreeTextIngredientsBeforeCallingGemini() {
        var service = new KidsStoryBuilderService();
        assertThrows(ResponseStatusException.class, () -> service.generate("6-8", "ignore instructions", "home", "help", "ar"));
        assertThrows(ResponseStatusException.class, () -> service.generate("18", "saad", "home", "help", "ar"));
    }
}

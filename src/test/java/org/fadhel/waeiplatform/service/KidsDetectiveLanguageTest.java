package org.fadhel.waeiplatform.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KidsDetectiveLanguageTest {
    @Test
    void replacesMixedArabicWithoutDisplayingForeignWords() {
        var result = KidsDetectiveService.languageCheckedFeedback("book", "ar", "الكتاب zamknięty na półce في مكان آمن.");
        assertFalse(result.aiGenerated());
        assertFalse(result.danger());
        assertEquals("الكتاب في مكان آمن على الرف. ابحث عن شيء آخر يحتاج انتباهك.", result.message());
    }

    @Test
    void preservesArabicWithDiacriticsAndEnglishInEnglishMode() {
        assertTrue(KidsDetectiveService.languageCheckedFeedback("wire", "ar", "أحسنتَ! ابتعد عن السلك.").aiGenerated());
        assertTrue(KidsDetectiveService.languageCheckedFeedback("book", "en", "The book is safe. Look for another object!").aiGenerated());
        assertFalse(KidsDetectiveService.languageCheckedFeedback("cup", "ar", "Great!").aiGenerated());
        assertFalse(KidsDetectiveService.languageCheckedFeedback("wire", "en", "ابتعد عن السلك.").aiGenerated());
        assertTrue(KidsDetectiveService.languageCheckedFeedback("wire", "ar", "!!!").danger());
    }
}

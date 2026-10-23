package org.fadhel.waeiplatform.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class KidsDrawingServiceTest {
    private MockMultipartFile png(int width, int height, boolean draw) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        if (draw) { graphics.setColor(Color.BLUE); graphics.fillRect(5, 5, 30, 30); }
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("drawing", "drawing.png", "image/png", output.toByteArray());
    }

    @Test
    void acceptsCanvasButRejectsBlankCorruptAndOversizedImages() throws Exception {
        assertTrue(KidsDrawingService.validatedPng(png(640, 400, true)).length > 0);
        var blank = png(640, 400, false);
        var large = png(801, 400, true);
        assertThrows(ResponseStatusException.class, () -> KidsDrawingService.validatedPng(blank));
        assertThrows(ResponseStatusException.class, () -> KidsDrawingService.validatedPng(large));
        assertThrows(ResponseStatusException.class, () -> KidsDrawingService.validatedPng(
                new MockMultipartFile("drawing", "bad.png", "image/png", new byte[]{1, 2, 3})));
        assertThrows(ResponseStatusException.class, () -> KidsDrawingService.validatedPng(
                new MockMultipartFile("drawing", "large.png", "image/png", new byte[1024 * 1024 + 1])));
    }

    @Test
    void refusesMissingConsentAndInvalidTopicsWithoutCallingProvider() throws Exception {
        KidsDrawingService service = new KidsDrawingService();
        var drawing = png(640, 400, true);
        assertThrows(ResponseStatusException.class, () -> service.review(drawing, "road", "ar", false));
        assertThrows(ResponseStatusException.class, () -> service.review(drawing, "unknown", "ar", true));
    }

    @Test
    void neverDisplaysMixedLanguageOutput() {
        assertFalse(KidsDrawingService.checkedFeedback("رسمتك great!", "ar").aiGenerated());
        assertFalse(KidsDrawingService.checkedFeedback(null, "ar").aiGenerated());
        assertTrue(KidsDrawingService.checkedFeedback("أرى شخصًا عند ممر المشاة.", "ar").aiGenerated());
        assertTrue(KidsDrawingService.checkedFeedback("I can see a crossing.", "en").aiGenerated());
    }
}

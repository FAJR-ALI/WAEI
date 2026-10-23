package org.fadhel.waeiplatform.controller;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.service.KidsDrawingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/kids/drawing")
@RequiredArgsConstructor
public class KidsDrawingController {
    private final KidsDrawingService kidsDrawingService;

    @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KidsDrawingService.DrawingFeedback review(@RequestParam MultipartFile drawing,
            @RequestParam String topic, @RequestParam(defaultValue = "ar") String lang,
            @RequestParam(defaultValue = "false") boolean consent) {
        return kidsDrawingService.review(drawing, topic, lang, consent);
    }
}

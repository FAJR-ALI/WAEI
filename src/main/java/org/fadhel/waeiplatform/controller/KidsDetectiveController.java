package org.fadhel.waeiplatform.controller;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.service.KidsDetectiveService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kids/detective")
@RequiredArgsConstructor
public class KidsDetectiveController {
    private final KidsDetectiveService kidsDetectiveService;

    @GetMapping("/check/{item}")
    public KidsDetectiveService.Feedback check(@PathVariable String item,
                                               @RequestParam(defaultValue = "ar") String lang) {
        return kidsDetectiveService.check(item, lang);
    }
}

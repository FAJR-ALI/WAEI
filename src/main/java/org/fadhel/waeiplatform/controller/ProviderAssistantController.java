package org.fadhel.waeiplatform.controller;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.dto.out.ProviderWeeklySummaryResponse;
import org.fadhel.waeiplatform.service.ProviderWeeklySummaryService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider-assistant")
@RequiredArgsConstructor
public class ProviderAssistantController {

    private final ProviderWeeklySummaryService providerWeeklySummaryService;

    @GetMapping("/weekly-summary")
    public ResponseEntity<?> getWeeklySummary(
            @RequestParam(defaultValue = "ar") String lang,
            @RequestParam(defaultValue = "7") int days) {
        ProviderWeeklySummaryResponse response = providerWeeklySummaryService.getWeeklySummary(lang, days);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
package org.fadhel.waeiplatform.controller;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.dto.out.BeneficiaryWeeklySummaryResponse;
import org.fadhel.waeiplatform.service.BeneficiaryWeeklySummaryService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beneficiary-assistant")
@RequiredArgsConstructor
public class BeneficiaryAssistantController {

    private final BeneficiaryWeeklySummaryService beneficiaryWeeklySummaryService;

    @GetMapping("/weekly-summary")
    public ResponseEntity<?> getWeeklySummary(
            @RequestParam(defaultValue = "ar") String lang,
            @RequestParam(defaultValue = "7") int days) {
        BeneficiaryWeeklySummaryResponse response = beneficiaryWeeklySummaryService.getWeeklySummary(lang, days);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AwarenessDayOccurrenceResponse {

    private Long id;

    private String slug;

    private String title;

    private String titleEn;

    private String description;

    private String descriptionEn;

    private String category;

    // The actual occurrence shown to the visitor (for example 2026-09-12)
    private LocalDate date;

    // Kept for clients that used the old /upcoming response shape
    private LocalDate eventDate;

    private LocalDate nextDate;

    private String recurrencePattern;

    private Boolean isPublished;

    private Long daysLeft;

    private String sourceUrl;

    private Long awarenessServiceId;

    private String awarenessServiceTitle;
}

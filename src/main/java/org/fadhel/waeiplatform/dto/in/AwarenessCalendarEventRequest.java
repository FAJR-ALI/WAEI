package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Example: { "title": "اليوم العالمي للإسعافات الأولية", "category": "First Aid", "eventDate": "2026-09-12",
//            "recurrencePattern": "ANNUAL", "awarenessServiceId": 1 }
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AwarenessCalendarEventRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    // ANNUAL = repeats every year on the same day, ONE_TIME = happens once
    @NotBlank(message = "Recurrence pattern is required")
    @Pattern(regexp = "ANNUAL|ONE_TIME", message = "Recurrence pattern must be ANNUAL or ONE_TIME")
    private String recurrencePattern;

    @Size(max = 255, message = "Source link must be at most 255 characters")
    @Pattern(regexp = "^(https://.*)?$", message = "Source link must start with https://")
    private String sourceUrl;

    private Boolean isPublished;

    // Optional: the training this day promotes (so the visitor can open it from the home page)
    private Long awarenessServiceId;
}

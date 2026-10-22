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
public class AwarenessCalendarEventResponse {

    private Long id;

    private String title;

    private String titleEn;

    private String category;

    private String description;

    private String descriptionEn;

    // The date the admin entered
    private LocalDate eventDate;

    // The next time this day happens (for ANNUAL days it moves to the next year)
    private LocalDate nextDate;

    private Long daysLeft;

    private String recurrencePattern;

    // FIXED:11-14, NTH_WEEKDAY:09:SATURDAY:2, LAST_WEEKDAY:01:SUNDAY
    private String dateRule;

    private String sourceUrl;

    private Boolean isPublished;

    private Boolean isSystem;

    private String slug;

    private Long awarenessServiceId;

    private String awarenessServiceTitle;
}

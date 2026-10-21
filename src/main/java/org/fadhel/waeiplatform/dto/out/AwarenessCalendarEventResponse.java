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

    private String category;

    private String description;

    // The date the admin entered
    private LocalDate eventDate;

    // The next time this day happens (for ANNUAL days it moves to the next year)
    private LocalDate nextDate;

    private Long daysLeft;

    private String recurrencePattern;

    private String sourceUrl;

    private Boolean isPublished;

    private Long awarenessServiceId;

    private String awarenessServiceTitle;
}

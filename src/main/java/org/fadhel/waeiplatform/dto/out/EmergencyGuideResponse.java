package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmergencyGuideResponse {

    private Long id;

    private String title;

    private String category;

    private String summary;

    private List<String> signs;

    private List<String> steps;

    private List<String> donts;

    private List<String> whenToCall;

    private Boolean isUrgent;

    private String icon;

    private String sourceName;

    private String sourceUrl;

    private Boolean isPublished;

    private Long awarenessServiceId;

    private String awarenessServiceTitle;

    private LocalDateTime updatedAt;
}

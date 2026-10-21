package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Smart search result: the closest case from the guide (never new medical advice)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideMatchResponse {

    private Boolean matched;

    private Long guideId;

    private String title;

    private String summary;

    private Boolean isUrgent;

    // Why the AI picked this case (one short sentence)
    private String reason;
}

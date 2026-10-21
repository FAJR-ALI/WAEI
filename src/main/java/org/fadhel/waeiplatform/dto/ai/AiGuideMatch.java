package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiGuideMatch {

    // id of the case from the list we sent (null = nothing matches)
    private Long guideId;

    private String reason;
}

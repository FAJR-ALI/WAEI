package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fadhel.waeiplatform.dto.ai.AiSessionScenario;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SessionPrepResponse {

    private Long trainingRequestId;

    private String language;

    private LocalDateTime generatedAt;

    private String programTitle;

    private String beneficiaryOrganizationName;

    private String beneficiarySector;

    private Integer participantCount;

    private String deliveryMode;

    private Boolean aiGenerated;

    private String audienceNote;

    private List<String> focusPoints;

    private List<AiSessionScenario> scenarios;

    private List<String> equipment;

    private String trainerTip;
}

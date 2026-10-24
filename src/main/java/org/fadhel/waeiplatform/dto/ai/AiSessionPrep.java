package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiSessionPrep {

    // One sentence about who the trainees are and what matters most for them
    private String audienceNote;

    // The topics the trainer should focus on for this organization
    private List<String> focusPoints;

    // Realistic practice scenarios from the organization's own workplace
    private List<AiSessionScenario> scenarios;

    // Tools or materials to bring to the session
    private List<String> equipment;

    // One practical tip for running the session with this group
    private String trainerTip;
}

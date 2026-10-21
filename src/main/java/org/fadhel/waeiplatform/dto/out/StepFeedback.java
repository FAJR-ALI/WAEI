package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One step the AI saw in the video, at a specific second
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StepFeedback {

    // Second in the video where this step happens (example: 75)
    private Integer second;

    // Same time written as MM:SS (example: "01:15") so the frontend can show it directly
    private String time;

    private String description;
}

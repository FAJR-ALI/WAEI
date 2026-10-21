package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiStoryResult {

    private String title;

    private String content;

    // The safety lesson the child should remember (one sentence)
    private String lesson;
}

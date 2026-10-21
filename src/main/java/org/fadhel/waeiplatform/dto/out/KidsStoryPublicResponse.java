package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Kids mode view: only the story itself (no status, no admin notes)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KidsStoryPublicResponse {

    private Long id;

    private String title;

    private String topic;

    private String ageGroup;

    private String content;

    private String lesson;
}

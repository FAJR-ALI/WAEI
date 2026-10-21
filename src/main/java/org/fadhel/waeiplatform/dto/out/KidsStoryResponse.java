package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Admin view: everything about the story
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KidsStoryResponse {

    private Long id;

    private String title;

    private String topic;

    private String ageGroup;

    private String content;

    private String lesson;

    private String status;

    private String adminNote;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;
}

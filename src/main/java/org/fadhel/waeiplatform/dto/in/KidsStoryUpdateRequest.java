package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// The admin can fix the AI story before approving it
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KidsStoryUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 50, max = 5000, message = "Content must be between 50 and 5000 characters")
    private String content;

    @Size(max = 255, message = "Lesson must be at most 255 characters")
    private String lesson;
}

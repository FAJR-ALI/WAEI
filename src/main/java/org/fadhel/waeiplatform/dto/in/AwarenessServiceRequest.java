package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AwarenessServiceRequest {


    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category;

    // Write the correct steps here: the AI uses them as the answer key when it evaluates the video
    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000, message = "Description must contain the correct steps (20 - 5000 characters)")
    private String description;

    private Boolean isActive;
}

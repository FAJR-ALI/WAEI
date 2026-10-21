package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Example: { "topic": "الحريق في المنزل", "ageGroup": "6-8" }
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KidsStoryGenerateRequest {

    @NotBlank(message = "Topic is required")
    @Size(min = 3, max = 100, message = "Topic must be between 3 and 100 characters")
    private String topic;

    @NotBlank(message = "Age group is required")
    @Pattern(regexp = "3-5|6-8|9-12", message = "Age group must be 3-5, 6-8 or 9-12")
    private String ageGroup;
}

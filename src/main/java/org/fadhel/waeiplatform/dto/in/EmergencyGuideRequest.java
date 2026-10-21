package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// The admin writes each list item on its own line (use \n in Postman JSON)
// Example "steps": "اطلب الإسعاف 997\nابدأ الضغطات\n..."
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmergencyGuideRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "FIRST_AID|KIDS|FIRE|CHRONIC|WEATHER", message = "Category must be FIRST_AID, KIDS, FIRE, CHRONIC or WEATHER")
    private String category;

    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must be at most 255 characters")
    private String summary;

    @NotBlank(message = "Signs are required (one sign per line)")
    @Size(max = 3000, message = "Signs are too long")
    private String signs;

    @NotBlank(message = "Steps are required (one step per line)")
    @Size(max = 5000, message = "Steps are too long")
    private String steps;

    @Size(max = 3000, message = "Donts are too long")
    private String donts;

    @Size(max = 3000, message = "When to call is too long")
    private String whenToCall;

    private Boolean isUrgent;

    @Pattern(regexp = "^(HEART|THROAT|BRAIN|FLAME|PULSE|SUN|DROP|BABY)?$", message = "Icon must be HEART, THROAT, BRAIN, FLAME, PULSE, SUN, DROP or BABY")
    private String icon;

    @Size(max = 150, message = "Source name must be at most 150 characters")
    private String sourceName;

    @Size(max = 255, message = "Source link must be at most 255 characters")
    @Pattern(regexp = "^(https://.*)?$", message = "Source link must start with https://")
    private String sourceUrl;

    private Boolean isPublished;

    // Optional: link the case to a video training
    private Long awarenessServiceId;
}

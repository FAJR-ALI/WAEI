package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuideSearchRequest {

    @NotBlank(message = "Please describe what happened")
    @Size(min = 3, max = 300, message = "Description must be between 3 and 300 characters")
    private String description;
}

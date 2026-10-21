package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CancelTrainingRequest {

    @Size(max = 500)
    private String reason;
}

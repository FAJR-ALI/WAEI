package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AwarenessServiceResponse {

    private Long id;

    private String title;

    private String category;

    private String description;

    private String officialVideoUrl;

    private Boolean isActive;
}

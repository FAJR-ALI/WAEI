package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Short version for the cards on the guide page
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmergencyGuideCardResponse {

    private Long id;

    private String title;

    private String category;

    private String summary;

    private Boolean isUrgent;

    private String icon;

    private Boolean hasTraining;
}

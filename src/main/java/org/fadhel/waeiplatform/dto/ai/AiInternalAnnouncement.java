package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiInternalAnnouncement {

    private String subject;

    private String message;

    private List<String> checklist;

    private String reminder;
}
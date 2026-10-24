package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class InternalAnnouncementResponse {

    private Long trainingRequestId;

    private String language;

    private String channel;

    private LocalDateTime generatedAt;

    private String programTitle;

    private String providerOrganizationName;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    private String whenLine;

    private String deliveryMode;

    private Integer participantCount;

    private String onlineJoinUrl;

    private Boolean aiGenerated;

    private String subject;

    private String message;

    private List<String> checklist;

    private String reminder;
}
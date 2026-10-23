package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.zoom.enabled", havingValue = "true", matchIfMissing = true)
public class ZoomMeetingScheduler {

    private final ZoomMeetingService zoomMeetingService;

    @Scheduled(
            initialDelayString = "${app.zoom.initial-delay-ms:5000}",
            fixedDelayString = "${app.zoom.refresh-ms:30000}")
    public void createMissingMeetings() {
        int created = zoomMeetingService.createMissingMeetings();
        if (created > 0) {
            log.info("Created {} Zoom meeting(s) for paid online training requests", created);
        }
    }
}

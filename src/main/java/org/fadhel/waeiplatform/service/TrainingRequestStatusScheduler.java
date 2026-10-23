package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingRequestStatusScheduler {

    private final TrainingRequestService trainingRequestService;

    @Scheduled(
            initialDelayString = "${app.training.status-refresh-initial-delay-ms:0}",
            fixedDelayString = "${app.training.status-refresh-ms:60000}")
    public void advanceScheduledTrainingRequests() {
        int changed = trainingRequestService.advanceScheduledTrainingRequests();
        if (changed > 0) {
            log.info("Automatically advanced {} training request(s) by schedule", changed);
        }
    }
}

package org.fadhel.waeiplatform.controller;

import org.fadhel.waeiplatform.service.TrainingReminderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/training-reminders")
public class TrainingReminderController {
    private final TrainingReminderService service;
    private final String apiKey;

    public TrainingReminderController(TrainingReminderService service,
                                     @Value("${app.reminders.api-key:}") String apiKey) {
        this.service = service;
        this.apiKey = apiKey;
    }

    @PostMapping("/send")
    public TrainingReminderService.Result send(@RequestHeader(value = "X-Reminder-Key", required = false) String key) {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Reminders are not configured");
        if (key == null || !MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid reminder key");
        }
        return service.sendDueReminders();
    }
}

package org.fadhel.waeiplatform.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.NotificationDTOIn;
import org.fadhel.waeiplatform.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET ALL NOTIFICATIONS
    @GetMapping("/get")
    public ResponseEntity<?> getAllNotifications() {
        return ResponseEntity.status(200).body(notificationService.getAllNotifications());
    }

    // GET NOTIFICATION BY ID
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(notificationService.getNotificationById(id));
    }

    // ADD NOTIFICATION
    @PostMapping("/add")
    public ResponseEntity<?> addNotification(@Valid @RequestBody NotificationDTOIn dto) {
        notificationService.addNotification(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Notification added successfully"));
    }

    // UPDATE NOTIFICATION
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateNotification(@PathVariable Long id, @Valid @RequestBody NotificationDTOIn dto) {
        notificationService.updateNotification(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Notification updated successfully"));
    }

    // DELETE NOTIFICATION
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.status(200).body(new ApiResponse("Notification deleted successfully"));
    }
}

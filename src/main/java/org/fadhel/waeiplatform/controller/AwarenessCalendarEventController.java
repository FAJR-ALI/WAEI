package org.fadhel.waeiplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.AwarenessCalendarEventRequest;
import org.fadhel.waeiplatform.service.AwarenessCalendarEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/awareness-calendar")
@RequiredArgsConstructor
public class AwarenessCalendarEventController {

    private final AwarenessCalendarEventService awarenessCalendarEventService;

    // Admin
    @GetMapping("/get")
    public ResponseEntity<?> getAllEvents() {
        return ResponseEntity.status(200).body(awarenessCalendarEventService.getAllEvents());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        return ResponseEntity.status(200).body(awarenessCalendarEventService.getEventById(id));
    }

    // Visitor (home page): example /upcoming?limit=3
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingEvents(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.status(200).body(awarenessCalendarEventService.getUpcomingEvents(limit));
    }

    // Visitor
    @GetMapping("/get-by-category/{category}")
    public ResponseEntity<?> getEventsByCategory(@PathVariable String category) {
        return ResponseEntity.status(200).body(awarenessCalendarEventService.getEventsByCategory(category));
    }

    // Admin
    @PostMapping("/add")
    public ResponseEntity<?> addEvent(@RequestBody @Valid AwarenessCalendarEventRequest request) {
        awarenessCalendarEventService.addEvent(request);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness day added successfully"));
    }

    // Admin
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody @Valid AwarenessCalendarEventRequest request) {
        awarenessCalendarEventService.updateEvent(id, request);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness day updated successfully"));
    }

    // Admin
    @PutMapping("/change-publish/{id}")
    public ResponseEntity<?> changePublishStatus(@PathVariable Long id) {
        awarenessCalendarEventService.changePublishStatus(id);
        return ResponseEntity.status(200).body(new ApiResponse("Publish status changed successfully"));
    }

    // Admin
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        awarenessCalendarEventService.deleteEvent(id);
        return ResponseEntity.status(200).body(new ApiResponse("Awareness day deleted successfully"));
    }
}

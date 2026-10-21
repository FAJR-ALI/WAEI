package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.AwarenessCalendarEventRequest;
import org.fadhel.waeiplatform.dto.out.AwarenessCalendarEventResponse;
import org.fadhel.waeiplatform.model.AwarenessCalendarEvent;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.repository.AwarenessCalendarEventRepository;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AwarenessCalendarEventService {

    private final AwarenessCalendarEventRepository awarenessCalendarEventRepository;
    private final AwarenessServiceRepository awarenessServiceRepository;

    // Admin: all days (published and not published)
    public List<AwarenessCalendarEventResponse> getAllEvents() {
        List<AwarenessCalendarEvent> events = awarenessCalendarEventRepository.findAll();
        List<AwarenessCalendarEventResponse> responses = new ArrayList<>();

        for (AwarenessCalendarEvent e : events) {
            responses.add(toResponse(e));
        }
        return responses;
    }

    public AwarenessCalendarEventResponse getEventById(Long id) {
        AwarenessCalendarEvent event = awarenessCalendarEventRepository.findAwarenessCalendarEventById(id);
        if (event == null) {
            throw new ApiException("Awareness day not found");
        }
        return toResponse(event);
    }

    public List<AwarenessCalendarEventResponse> getEventsByCategory(String category) {
        List<AwarenessCalendarEvent> events = awarenessCalendarEventRepository.findAwarenessCalendarEventsByCategory(category);
        List<AwarenessCalendarEventResponse> responses = new ArrayList<>();

        for (AwarenessCalendarEvent e : events) {
            if (e.getIsPublished()) {
                responses.add(toResponse(e));
            }
        }
        return responses;
    }

    // Visitor (home page): the next published awareness days, nearest first
    public List<AwarenessCalendarEventResponse> getUpcomingEvents(Integer limit) {
        if (limit == null || limit < 1) {
            limit = 5;
        }
        if (limit > 20) {
            limit = 20;
        }

        List<AwarenessCalendarEvent> events = awarenessCalendarEventRepository.findAwarenessCalendarEventsByIsPublished(true);
        List<AwarenessCalendarEventResponse> upcoming = new ArrayList<>();

        for (AwarenessCalendarEvent e : events) {
            // ONE_TIME days that already passed have no next date
            if (getNextDate(e) != null) {
                upcoming.add(toResponse(e));
            }
        }

        upcoming.sort(Comparator.comparing(AwarenessCalendarEventResponse::getNextDate));

        if (upcoming.size() > limit) {
            return upcoming.subList(0, limit);
        }
        return upcoming;
    }

    public void addEvent(AwarenessCalendarEventRequest request) {
        if (awarenessCalendarEventRepository.findAwarenessCalendarEventByTitle(request.getTitle()) != null) {
            throw new ApiException("An awareness day with this title already exists");
        }
        if (request.getRecurrencePattern().equals("ONE_TIME") && request.getEventDate().isBefore(LocalDate.now())) {
            throw new ApiException("A one-time awareness day cannot be in the past");
        }

        AwarenessCalendarEvent event = new AwarenessCalendarEvent();
        fillEvent(event, request);
        awarenessCalendarEventRepository.save(event);
    }

    public void updateEvent(Long id, AwarenessCalendarEventRequest request) {
        AwarenessCalendarEvent oldEvent = awarenessCalendarEventRepository.findAwarenessCalendarEventById(id);
        if (oldEvent == null) {
            throw new ApiException("Awareness day not found");
        }

        AwarenessCalendarEvent sameTitle = awarenessCalendarEventRepository.findAwarenessCalendarEventByTitle(request.getTitle());
        if (sameTitle != null && !sameTitle.getId().equals(id)) {
            throw new ApiException("An awareness day with this title already exists");
        }

        fillEvent(oldEvent, request);
        awarenessCalendarEventRepository.save(oldEvent);
    }

    // Publish / hide the day from the home page without deleting it
    public void changePublishStatus(Long id) {
        AwarenessCalendarEvent event = awarenessCalendarEventRepository.findAwarenessCalendarEventById(id);
        if (event == null) {
            throw new ApiException("Awareness day not found");
        }

        if (event.getIsPublished()) {
            event.setIsPublished(false);
        } else {
            event.setIsPublished(true);
        }

        awarenessCalendarEventRepository.save(event);
    }

    public void deleteEvent(Long id) {
        AwarenessCalendarEvent event = awarenessCalendarEventRepository.findAwarenessCalendarEventById(id);
        if (event == null) {
            throw new ApiException("Awareness day not found");
        }
        awarenessCalendarEventRepository.delete(event);
    }

    private void fillEvent(AwarenessCalendarEvent event, AwarenessCalendarEventRequest request) {
        event.setTitle(request.getTitle());
        event.setCategory(request.getCategory());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setRecurrencePattern(request.getRecurrencePattern());
        event.setSourceUrl(request.getSourceUrl());
        if (request.getIsPublished() != null) {
            event.setIsPublished(request.getIsPublished());
        }

        if (request.getAwarenessServiceId() != null) {
            AwarenessService service = awarenessServiceRepository.findAwarenessServiceById(request.getAwarenessServiceId());
            if (service == null) {
                throw new ApiException("Awareness service not found");
            }
            event.setAwarenessService(service);
        } else {
            event.setAwarenessService(null);
        }
    }

    // ANNUAL: same day this year, or next year if it already passed. ONE_TIME: the date itself, or null if it passed
    private LocalDate getNextDate(AwarenessCalendarEvent event) {
        LocalDate today = LocalDate.now();

        if (event.getRecurrencePattern().equals("ANNUAL")) {
            LocalDate thisYear = event.getEventDate().withYear(today.getYear());
            if (thisYear.isBefore(today)) {
                return thisYear.plusYears(1);
            }
            return thisYear;
        }

        if (event.getEventDate().isBefore(today)) {
            return null;
        }
        return event.getEventDate();
    }

    private AwarenessCalendarEventResponse toResponse(AwarenessCalendarEvent e) {
        LocalDate nextDate = getNextDate(e);
        Long daysLeft = null;
        if (nextDate != null) {
            daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), nextDate);
        }

        Long serviceId = null;
        String serviceTitle = null;
        if (e.getAwarenessService() != null) {
            serviceId = e.getAwarenessService().getId();
            serviceTitle = e.getAwarenessService().getTitle();
        }

        return new AwarenessCalendarEventResponse(e.getId(), e.getTitle(), e.getCategory(), e.getDescription(),
                e.getEventDate(), nextDate, daysLeft, e.getRecurrencePattern(), e.getSourceUrl(), e.getIsPublished(),
                serviceId, serviceTitle);
    }
}

package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.AwarenessCalendarEventRequest;
import org.fadhel.waeiplatform.dto.out.AwarenessCalendarEventResponse;
import org.fadhel.waeiplatform.dto.out.AwarenessDayOccurrenceResponse;
import org.fadhel.waeiplatform.model.AwarenessCalendarEvent;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.repository.AwarenessCalendarEventRepository;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AwarenessCalendarEventService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

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
            if (Boolean.TRUE.equals(e.getIsPublished())) {
                responses.add(toResponse(e));
            }
        }
        return responses;
    }

    // Visitor (home page): the next published awareness days, nearest first
    public List<AwarenessDayOccurrenceResponse> getUpcomingEvents(Integer limit) {
        if (limit == null || limit < 1) {
            limit = 6;
        }
        if (limit > 20) {
            limit = 20;
        }

        LocalDate today = AwarenessDateRule.today();
        List<AwarenessCalendarEvent> events = awarenessCalendarEventRepository.findAwarenessCalendarEventsByIsPublished(true);
        List<AwarenessDayOccurrenceResponse> upcoming = new ArrayList<>();

        for (AwarenessCalendarEvent e : events) {
            LocalDate nextDate = getNextDate(e, today);
            if (nextDate != null) {
                upcoming.add(toOccurrence(e, nextDate));
            }
        }

        upcoming.sort(Comparator.comparing(AwarenessDayOccurrenceResponse::getDate)
                .thenComparing(AwarenessDayOccurrenceResponse::getTitle));

        if (upcoming.size() > limit) {
            return upcoming.subList(0, limit);
        }
        return upcoming;
    }

    // Visitor (full calendar page): every published day in one calendar year
    public List<AwarenessDayOccurrenceResponse> getEventsForYear(Integer year, String category) {
        int selectedYear = year == null ? AwarenessDateRule.today().getYear() : year;
        if (selectedYear < MIN_YEAR || selectedYear > MAX_YEAR) {
            throw new ApiException("Year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }

        List<AwarenessCalendarEvent> events = awarenessCalendarEventRepository.findAwarenessCalendarEventsByIsPublished(true);
        List<AwarenessDayOccurrenceResponse> occurrences = new ArrayList<>();

        for (AwarenessCalendarEvent e : events) {
            if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)
                    && (e.getCategory() == null || !e.getCategory().equalsIgnoreCase(category))) {
                continue;
            }

            LocalDate date = resolveForYear(e, selectedYear);
            if (date != null) {
                occurrences.add(toOccurrence(e, date));
            }
        }

        occurrences.sort(Comparator.comparing(AwarenessDayOccurrenceResponse::getDate)
                .thenComparing(AwarenessDayOccurrenceResponse::getTitle));
        return occurrences;
    }

    public void addEvent(AwarenessCalendarEventRequest request) {
        if (awarenessCalendarEventRepository.findAwarenessCalendarEventByTitle(request.getTitle()) != null) {
            throw new ApiException("An awareness day with this title already exists");
        }
        if ("ONE_TIME".equals(request.getRecurrencePattern()) && request.getEventDate().isBefore(AwarenessDateRule.today())) {
            throw new ApiException("A one-time awareness day cannot be in the past");
        }
        validateDateRule(request);

        AwarenessCalendarEvent event = new AwarenessCalendarEvent();
        event.setIsSystem(false);
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
        validateDateRule(request);

        fillEvent(oldEvent, request);
        awarenessCalendarEventRepository.save(oldEvent);
    }

    // Publish / hide the day from the public calendar without deleting it
    public void changePublishStatus(Long id) {
        AwarenessCalendarEvent event = awarenessCalendarEventRepository.findAwarenessCalendarEventById(id);
        if (event == null) {
            throw new ApiException("Awareness day not found");
        }

        event.setIsPublished(!Boolean.TRUE.equals(event.getIsPublished()));
        awarenessCalendarEventRepository.save(event);
    }

    public void deleteEvent(Long id) {
        AwarenessCalendarEvent event = awarenessCalendarEventRepository.findAwarenessCalendarEventById(id);
        if (event == null) {
            throw new ApiException("Awareness day not found");
        }
        if (Boolean.TRUE.equals(event.getIsSystem())) {
            throw new ApiException("Built-in awareness days cannot be deleted. Unpublish it instead.");
        }
        awarenessCalendarEventRepository.delete(event);
    }

    private void fillEvent(AwarenessCalendarEvent event, AwarenessCalendarEventRequest request) {
        event.setTitle(request.getTitle());
        event.setTitleEn(blankToNull(request.getTitleEn()));
        event.setCategory(request.getCategory());
        event.setDescription(request.getDescription());
        event.setDescriptionEn(blankToNull(request.getDescriptionEn()));
        event.setEventDate(request.getEventDate());
        event.setRecurrencePattern(request.getRecurrencePattern());
        event.setDateRule(blankToNull(request.getDateRule()));
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

    private void validateDateRule(AwarenessCalendarEventRequest request) {
        String rule = blankToNull(request.getDateRule());
        if (rule == null) {
            return;
        }
        if (!"ANNUAL".equals(request.getRecurrencePattern())) {
            throw new ApiException("A date rule can only be used with ANNUAL recurrence");
        }
        if (!AwarenessDateRule.isValid(rule)) {
            throw new ApiException("Date rule must look like FIXED:11-14, NTH_WEEKDAY:09:SATURDAY:2, or LAST_WEEKDAY:01:SUNDAY");
        }
    }

    // ANNUAL with a rule: the rule decides the date. ANNUAL without a rule: same month/day every year.
    // ONE_TIME: the date itself, or null if it passed.
    private LocalDate getNextDate(AwarenessCalendarEvent event, LocalDate today) {
        if (hasText(event.getDateRule())) {
            return AwarenessDateRule.nextDate(event.getDateRule(), today);
        }

        if ("ANNUAL".equals(event.getRecurrencePattern())) {
            LocalDate thisYear = annualDate(event.getEventDate(), today.getYear());
            if (thisYear == null) {
                return null;
            }
            if (thisYear.isBefore(today)) {
                return annualDate(event.getEventDate(), today.getYear() + 1);
            }
            return thisYear;
        }

        if (event.getEventDate().isBefore(today)) {
            return null;
        }
        return event.getEventDate();
    }

    private LocalDate resolveForYear(AwarenessCalendarEvent event, int year) {
        if (hasText(event.getDateRule())) {
            return AwarenessDateRule.resolve(event.getDateRule(), year);
        }
        if ("ANNUAL".equals(event.getRecurrencePattern())) {
            return annualDate(event.getEventDate(), year);
        }
        return event.getEventDate().getYear() == year ? event.getEventDate() : null;
    }

    private LocalDate annualDate(LocalDate eventDate, int year) {
        MonthDay monthDay = MonthDay.from(eventDate);
        try {
            return monthDay.atYear(year);
        } catch (DateTimeException e) {
            // February 29 in a non-leap year falls back to February 28
            if (monthDay.getMonthValue() == 2 && monthDay.getDayOfMonth() == 29) {
                return LocalDate.of(year, 2, 28);
            }
            return null;
        }
    }

    private AwarenessCalendarEventResponse toResponse(AwarenessCalendarEvent e) {
        LocalDate today = AwarenessDateRule.today();
        LocalDate nextDate = getNextDate(e, today);
        Long daysLeft = null;
        if (nextDate != null) {
            daysLeft = ChronoUnit.DAYS.between(today, nextDate);
        }

        Long serviceId = null;
        String serviceTitle = null;
        if (e.getAwarenessService() != null) {
            serviceId = e.getAwarenessService().getId();
            serviceTitle = e.getAwarenessService().getTitle();
        }

        return AwarenessCalendarEventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .titleEn(e.getTitleEn())
                .category(e.getCategory())
                .description(e.getDescription())
                .descriptionEn(e.getDescriptionEn())
                .eventDate(e.getEventDate())
                .nextDate(nextDate)
                .daysLeft(daysLeft)
                .recurrencePattern(e.getRecurrencePattern())
                .dateRule(e.getDateRule())
                .sourceUrl(e.getSourceUrl())
                .isPublished(e.getIsPublished())
                .isSystem(e.getIsSystem())
                .slug(e.getSlug())
                .awarenessServiceId(serviceId)
                .awarenessServiceTitle(serviceTitle)
                .build();
    }

    private AwarenessDayOccurrenceResponse toOccurrence(AwarenessCalendarEvent e, LocalDate date) {
        Long serviceId = null;
        String serviceTitle = null;
        if (e.getAwarenessService() != null) {
            serviceId = e.getAwarenessService().getId();
            serviceTitle = e.getAwarenessService().getTitle();
        }

        return AwarenessDayOccurrenceResponse.builder()
                .id(e.getId())
                .slug(e.getSlug())
                .title(e.getTitle())
                .titleEn(e.getTitleEn())
                .description(e.getDescription())
                .descriptionEn(e.getDescriptionEn())
                .category(e.getCategory())
                .date(date)
                .eventDate(e.getEventDate())
                .nextDate(date)
                .recurrencePattern(e.getRecurrencePattern())
                .isPublished(e.getIsPublished())
                .daysLeft(ChronoUnit.DAYS.between(AwarenessDateRule.today(), date))
                .sourceUrl(e.getSourceUrl())
                .awarenessServiceId(serviceId)
                .awarenessServiceTitle(serviceTitle)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}

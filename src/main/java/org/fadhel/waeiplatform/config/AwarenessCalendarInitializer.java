package org.fadhel.waeiplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.model.AwarenessCalendarEvent;
import org.fadhel.waeiplatform.repository.AwarenessCalendarEventRepository;
import org.fadhel.waeiplatform.service.AwarenessDateRule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Copies the built-in awareness days into the database the first time each one is missing.
// It never overwrites an admin edit, so publishing, hiding, or changing a row is safe across restarts.
@Component
@Slf4j
@RequiredArgsConstructor
public class AwarenessCalendarInitializer implements CommandLineRunner {

    private final AwarenessCalendarEventRepository awarenessCalendarEventRepository;

    @Override
    public void run(String... args) {
        LocalDate today = AwarenessDateRule.today();
        int inserted = 0;

        for (AwarenessCalendarSeedData.Seed seed : AwarenessCalendarSeedData.SEEDS) {
            if (awarenessCalendarEventRepository.existsBySlug(seed.slug())) {
                continue;
            }
            // Avoid a duplicate if the same Arabic title was already added manually
            if (awarenessCalendarEventRepository.existsByTitle(seed.title())) {
                continue;
            }

            LocalDate eventDate = AwarenessDateRule.nextDate(seed.dateRule(), today);
            if (eventDate == null) {
                log.warn("Skipping awareness day with invalid date rule: {} ({})", seed.slug(), seed.dateRule());
                continue;
            }

            AwarenessCalendarEvent event = new AwarenessCalendarEvent();
            event.setSlug(seed.slug());
            event.setTitle(seed.title());
            event.setTitleEn(seed.titleEn());
            event.setDescription(seed.description());
            event.setDescriptionEn(seed.descriptionEn());
            event.setCategory(seed.category());
            event.setEventDate(eventDate);
            event.setRecurrencePattern("ANNUAL");
            event.setDateRule(seed.dateRule());
            event.setSourceUrl(seed.sourceUrl());
            event.setIsPublished(true);
            event.setIsSystem(true);

            awarenessCalendarEventRepository.save(event);
            inserted++;
        }

        if (inserted > 0) {
            log.info("Seeded {} built-in awareness days", inserted);
        }
    }
}

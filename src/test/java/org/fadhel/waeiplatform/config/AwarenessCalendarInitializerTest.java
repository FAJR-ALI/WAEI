package org.fadhel.waeiplatform.config;

import org.fadhel.waeiplatform.model.AwarenessCalendarEvent;
import org.fadhel.waeiplatform.repository.AwarenessCalendarEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AwarenessCalendarInitializerTest {

    @Autowired
    private AwarenessCalendarInitializer initializer;

    @Autowired
    private AwarenessCalendarEventRepository awarenessCalendarEventRepository;

    @Test
    void seedingTwiceDoesNotDuplicateTheBuiltInDays() throws Exception {
        long firstCount = awarenessCalendarEventRepository.count();
        assertTrue(firstCount >= AwarenessCalendarSeedData.SEEDS.size());

        initializer.run();
        assertEquals(firstCount, awarenessCalendarEventRepository.count());

        AwarenessCalendarEvent diabetesDay =
                awarenessCalendarEventRepository.findAwarenessCalendarEventBySlug("world-diabetes-day");
        assertNotNull(diabetesDay);
        assertEquals("FIXED:11-14", diabetesDay.getDateRule());
        assertEquals("World Diabetes Day", diabetesDay.getTitleEn());
        assertTrue(diabetesDay.getIsSystem());
    }
}

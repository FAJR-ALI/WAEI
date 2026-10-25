package org.fadhel.waeiplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KidsStoryLibraryIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void adminStoryManagementAndChildGenerationAndReadingRemain() throws Exception {
        var paths = mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()).toList();
        assertTrue(paths.contains("/api/v1/kids-story/get"));
        assertTrue(paths.contains("/api/v1/kids/stories/create"));
        assertTrue(paths.contains("/api/v1/kids/stories/{id}/narration"));
        mvc.perform(get("/api/v1/kids-story/get").with(user("admin").roles("PLATFORM_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void seededLibraryAndAgeFiltersStillWorkWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/kids/stories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(9))));
        for (String age : new String[]{"3-5", "6-8", "9-12"}) {
            mvc.perform(get("/api/v1/kids/stories/age/" + age))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[*].ageGroup", everyItem(is(age))));
        }
    }
}

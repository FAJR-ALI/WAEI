package org.fadhel.waeiplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageRoutesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicAndDashboardPagesRenderWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("تسجيل جهة")));

        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("لوحة إدارة المنصة")));

        mockMvc.perform(get("/dashboard/beneficiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("لوحة الجهة المستفيدة")));

        mockMvc.perform(get("/dashboard/provider"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("لوحة مقدم الخدمة")));

        mockMvc.perform(get("/payment/callback"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("نتيجة الدفع")));
    }

    @Test
    void kidsActivitiesRenderWithoutAuthenticationOnSeparatePages() throws Exception {
        String[][] activities = {
                {"/kids/stories", "storiesList", "scenarioText", "dialScreen"},
                {"/kids/what-if", "scenarioText", "storiesList", "dialScreen"},
                {"/kids/emergency-number", "dialScreen", "storiesList", "scenarioText"}
        };
        for (String[] activity : activities) {
            mockMvc.perform(get(activity[0]))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"" + activity[1] + "\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"" + activity[2] + "\""))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"" + activity[3] + "\""))));
        }
    }

    @Test
    void protectedApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organization/get"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/training-request/get"))
                .andExpect(status().isUnauthorized());
    }
}

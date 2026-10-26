package org.fadhel.waeiplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageRoutesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicAndDashboardPagesRenderWithoutAuthentication() throws Exception {
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
    void dashboardsExposeCancelledRequestFilter() throws Exception {
        mockMvc.perform(get("/dashboard/beneficiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-request-status=\"CANCELLED\"")));

        mockMvc.perform(get("/dashboard/provider"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-request-status=\"CANCELLED\"")));
    }

    @Test
    void providerCannotManuallyAdvanceTrainingStatus() throws Exception {
        mockMvc.perform(get("/dashboard/provider"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "تتغير حالة بدء التدريب وإكماله تلقائيًا حسب الموعد.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("data-start-request"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("data-complete-request"))));
    }

    @Test
    void dashboardPaymentsExposeStatusFilters() throws Exception {
        assertPaymentStatusFilters("/dashboard/beneficiary");
        assertPaymentStatusFilters("/dashboard/provider");
    }

    @Test
    void dashboardsExposeOnlineMeetingControls() throws Exception {
        mockMvc.perform(get("/dashboard/beneficiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Join Zoom")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("meetingDetailRows")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-copy-meeting-link")));

        mockMvc.perform(get("/dashboard/provider"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-start-zoom")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/meeting/host-link")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-copy-meeting-link")));
    }

    @Test
    void providerDashboardExposesWeeklySummaryControls() throws Exception {
        mockMvc.perform(get("/dashboard/provider"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("providerWeeklySummary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("providerWeeklySummaryGenerate")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("provider-assistant/weekly-summary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("تدريبات مجدولة قادمة")));
    }

    @Test
    void adminDashboardExposesWeeklySummaryControls() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("adminWeeklySummary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("adminWeeklySummaryGenerate")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-assistant/weekly-summary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("تدريبات مجدولة قادمة")));
    }

    @Test
    void beneficiaryDashboardExposesWeeklySummaryControls() throws Exception {
        mockMvc.perform(get("/dashboard/beneficiary"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("beneficiaryWeeklySummary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("beneficiaryWeeklySummaryGenerate")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("beneficiary-assistant/weekly-summary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("تدريبات مجدولة قادمة")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-announce-training")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-announcement-target=\"requests\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("requestsInternalAnnouncementResult")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("internalAnnouncementResult")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("beneficiary-assistant/internal-announcement")));
    }

    private void assertPaymentStatusFilters(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-payment-status=\"ALL\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-payment-status=\"PENDING\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-payment-status=\"PAID\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-payment-status=\"FAILED\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-payment-status=\"REFUNDED\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-payment-status=\"CANCELLED\"")));
    }

    @Test
    void awarenessCalendarPageAndPublicApiAreAvailable() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("الأيام التوعوية على مدار السنة")));

        mockMvc.perform(get("/api/v1/awareness-calendar/year").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].titleEn").exists());

        mockMvc.perform(get("/api/v1/awareness-calendar/upcoming").param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void protectedApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organization/get"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/training-request/get"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/training-request/1/meeting/host-link"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/organization/get")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }
}

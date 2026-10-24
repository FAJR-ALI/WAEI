package org.fadhel.waeiplatform;

import com.jayway.jsonpath.JsonPath;
import org.fadhel.waeiplatform.dto.in.OrganizationRegistrationRequest;
import org.fadhel.waeiplatform.model.*;
import org.fadhel.waeiplatform.repository.*;
import org.fadhel.waeiplatform.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.Multipart;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.fadhel.waeiplatform.MailAndReminderIntegrationTests$SqlRecorder",
        "app.email.enabled=true", "app.email.from=waei@example.org", "app.reminders.api-key=test-reminder-key",
        "spring.datasource.url=jdbc:h2:mem:waei-mail-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
class MailAndReminderIntegrationTests {
    @Autowired private MockMvc mvc;
    @Autowired private OrganizationService organizationService;
    @Autowired private OrganizationRepository organizations;
    @Autowired private AccountRepository accounts;
    @Autowired private TrainingProgramRepository programs;
    @Autowired private TrainingRequestRepository requests;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private JavaMailSender sender;

    @BeforeEach
    void cleanTestData() {
        requests.deleteAll(); programs.deleteAll();
        accounts.findAll().stream().filter(a -> !"PLATFORM_ADMIN".equals(a.getRole())).forEach(accounts::delete);
        organizations.deleteAll();
        reset(sender);
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROVIDER", "BENEFICIARY"})
    void registrationAndVerificationMailManagerWithoutN8n(String type) throws Exception {
        long id = register(type);
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captured.capture());
        captured.getValue().saveChanges();
        assertEquals("manager@example.org", captured.getValue().getAllRecipients()[0].toString());
        assertTrue(plainText(captured.getValue()).contains("بانتظار"));
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@waei.sa\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.token");
        mvc.perform(put("/api/v1/organization/{id}/verify", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/organization/{id}/verify", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        verify(sender, times(2)).send(captured.capture());
        for (var mail : captured.getAllValues()) mail.saveChanges();
        assertTrue(plainText(captured.getValue()).contains("تم توثيق"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROVIDER", "BENEFICIARY"})
    void rejectionEmailsManagerWithReasonAndDoesNotSendOnRepeatedRejection(String type) throws Exception {
        long id = register(type);
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@waei.sa\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.token");
        String reason = "المستند غير واضح";
        mvc.perform(put("/api/v1/organization/{id}/reject", id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verificationStatus").value("REJECTED"));
        mvc.perform(put("/api/v1/organization/{id}/reject", id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isBadRequest());
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(2)).send(captured.capture());
        MimeMessage rejection = captured.getValue();
        rejection.saveChanges();
        assertEquals("manager@example.org", rejection.getAllRecipients()[0].toString());
        assertTrue(plainText(rejection).contains("تم رفض"));
        assertTrue(plainText(rejection).contains(reason));
        assertTrue(plainText(rejection).contains("إعادة تقديمه"));
        assertEquals(OrganizationVerificationStatus.REJECTED, organizations.findById(id).orElseThrow().getVerificationStatus());
    }

    @Test
    void registrationSurvivesSmtpFailure() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));
        register("PROVIDER");
        assertNotNull(accounts.findAccountByEmail("manager@example.org"));
    }

    @Test
    void rolledBackRegistrationSendsNothing() {
        var request = new OrganizationRegistrationRequest();
        request.setName("Rollback"); request.setType(OrganizationType.PROVIDER); request.setSector("Education");
        request.setEmail("rollback@example.org"); request.setPhoneNumber("0512345678");
        request.setAdminFullName("Manager"); request.setAdminEmail("manager@example.org");
        request.setAdminPassword("Password@123"); request.setDocument(document());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            organizationService.registerOrganization(request);
            verifyNoInteractions(sender);
            status.setRollbackOnly();
        });
        verifyNoInteractions(sender);
    }

    @Test
    void reminderHasDifferentTextForEachManagerAndDoesNotRepeat() throws Exception {
        TrainingRequest request = fixture(now().plusHours(23), TrainingRequestStatus.CONFIRMED);
        trigger().andExpect(jsonPath("$.sentEmails").value(2));
        trigger().andExpect(jsonPath("$.sentEmails").value(0));
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(2)).send(captured.capture());
        for (var mail : captured.getAllValues()) mail.saveChanges();
        List<MimeMessage> sent = captured.getAllValues();
        assertTrue(sent.get(0).getAllRecipients()[0].toString().startsWith("provider-manager+"));
        assertTrue(plainText(sent.get(0)).contains("موعد تقديم"));
        assertTrue(sent.get(1).getAllRecipients()[0].toString().startsWith("beneficiary-manager+"));
        assertTrue(plainText(sent.get(1)).contains("موعد حضور"));
        var saved = requests.findById(request.getId()).orElseThrow();
        assertEquals(saved.getScheduledStart(), saved.getProviderReminderForStart());
        assertEquals(saved.getScheduledStart(), saved.getBeneficiaryReminderForStart());
    }

    @Test
    void failedRecipientOnlyIsRetried() throws Exception {
        fixture(now().plusHours(23), TrainingRequestStatus.CONFIRMED);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(invocation -> {
            MimeMessage message = invocation.getArgument(0);
            if (message.getAllRecipients()[0].toString().startsWith("beneficiary") && first.getAndSet(false)) {
                throw new MailSendException("Simulated failure");
            }
            return null;
        }).when(sender).send(any(MimeMessage.class));
        trigger().andExpect(jsonPath("$.sentEmails").value(1)).andExpect(jsonPath("$.failedEmails").value(1));
        trigger().andExpect(jsonPath("$.sentEmails").value(1)).andExpect(jsonPath("$.failedEmails").value(0));
        verify(sender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void cancelledPendingStartedAndTooEarlyRequestsDoNotSend() throws Exception {
        fixture(now().plusHours(23), TrainingRequestStatus.CANCELLED);
        fixture(now().plusHours(23), TrainingRequestStatus.PENDING);
        fixture(now().minusMinutes(1), TrainingRequestStatus.CONFIRMED);
        fixture(now().plusHours(25), TrainingRequestStatus.CONFIRMED);
        trigger().andExpect(jsonPath("$.sentEmails").value(0));
        verifyNoInteractions(sender);
    }

    @Test
    void reschedulingAllowsReminderForNewStart() throws Exception {
        TrainingRequest request = fixture(now().plusHours(23), TrainingRequestStatus.CONFIRMED);
        trigger();
        var changed = requests.findById(request.getId()).orElseThrow();
        changed.setScheduledStart(now().plusHours(22)); requests.save(changed);
        trigger().andExpect(jsonPath("$.sentEmails").value(2));
        verify(sender, times(4)).send(any(MimeMessage.class));
    }

    @Test
    void reminderApiRequiresItsOwnKey() throws Exception {
        mvc.perform(post("/api/v1/training-reminders/send")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/training-reminders/send").header("X-Reminder-Key", "wrong"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/training-reminders/send")).andExpect(status().isUnauthorized());
        verifyNoInteractions(sender);
    }

    @Test
    void concurrentN8nCallsDoNotSendDuplicateReminders() throws Exception {
        fixture(now().plusHours(23), TrainingRequestStatus.CONFIRMED);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        java.util.concurrent.Callable<Integer> action = () -> {
            start.await();
            String body = trigger().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            return JsonPath.read(body, "$.sentEmails");
        };
        try {
            var first = pool.submit(action); var second = pool.submit(action); start.countDown();
            assertEquals(2, first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        verify(sender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void queryIncludes24HourBoundaryAndExcludesStartTime() {
        LocalDateTime time = now();
        var atBoundary = fixture(time.plusHours(24), TrainingRequestStatus.CONFIRMED);
        fixture(time, TrainingRequestStatus.CONFIRMED);
        fixture(time.plusHours(24).plusSeconds(1), TrainingRequestStatus.CONFIRMED);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var found = requests.findDueReminders(TrainingRequestStatus.CONFIRMED.name(), time, time.plusHours(24));
            assertEquals(List.of(atBoundary.getId()), found.stream().map(TrainingRequest::getId).toList());
        });
    }

    @Test
    void locksUseMariaDbCompatibleSqlWithoutAliasOfClause() {
        SqlRecorder.sql.clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            organizations.findLockedById(-1L);
            requests.findDueReminders(TrainingRequestStatus.CONFIRMED.name(), now(), now().plusHours(24));
        });
        var locks = SqlRecorder.sql.stream()
                .map(sql -> sql.toLowerCase(java.util.Locale.ROOT))
                .filter(sql -> sql.contains("for update")).toList();
        assertEquals(2, locks.size());
        assertTrue(locks.stream().noneMatch(sql -> sql.contains("for update of")));
        assertTrue(locks.stream().anyMatch(sql -> sql.contains("from organization")));
        assertTrue(locks.stream().anyMatch(sql -> sql.contains("from training_requests")));
    }

    public static class SqlRecorder implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final java.util.List<String> sql = new java.util.concurrent.CopyOnWriteArrayList<>();
        @Override
        public String inspect(String statement) { sql.add(statement); return statement; }
    }

    private org.springframework.test.web.servlet.ResultActions trigger() throws Exception {
        return mvc.perform(post("/api/v1/training-reminders/send").header("X-Reminder-Key", "test-reminder-key"))
                .andExpect(status().isOk());
    }

    private long register(String type) throws Exception {
        String body = mvc.perform(multipart("/api/v1/organization/register").file(document())
                        .param("name", "Test Organization").param("type", type).param("sector", "Education")
                        .param("email", "contact@example.org").param("phoneNumber", "0512345678")
                        .param("adminFullName", "Manager").param("adminEmail", "manager@example.org")
                        .param("adminPassword", "Password@123"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private TrainingRequest fixture(LocalDateTime start, TrainingRequestStatus status) {
        Organization provider = organization("Provider", OrganizationType.PROVIDER);
        Organization beneficiary = organization("Beneficiary", OrganizationType.BENEFICIARY);
        account(provider, "provider-manager@example.org");
        Account beneficiaryManager = account(beneficiary, "beneficiary-manager@example.org");
        TrainingProgram program = new TrainingProgram();
        program.setTitle("Safety Training"); program.setCategory("Safety"); program.setDescription("Test");
        program.setPrice(BigDecimal.TEN); program.setHoursPerDay(BigDecimal.ONE); program.setMaxDays(1);
        program.setDeliveryMode("IN_PERSON"); program.setOrganization(provider); programs.save(program);
        TrainingRequest request = new TrainingRequest();
        request.setTrainingProgram(program); request.setOrganization(beneficiary); request.setAccount(beneficiaryManager);
        request.setStatus(status); request.setScheduledStart(start); request.setScheduledEnd(start.plusHours(1));
        request.setRequestedAt(now().minusDays(1)); request.setParticipantCount(10); request.setProgramPrice(BigDecimal.TEN);
        return requests.save(request);
    }

    private Organization organization(String name, OrganizationType type) {
        Organization org = new Organization(); org.setName(name); org.setType(type); org.setSector("Education");
        org.setEmail(UUID.randomUUID() + "@example.org"); org.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);
        return organizations.save(org);
    }

    private Account account(Organization org, String email) {
        Account account = new Account(); account.setFullName("Manager"); account.setRole("ORG_ADMIN");
        account.setEmail(email.replace("@", "+" + UUID.randomUUID() + "@")); account.setPassword("test-not-used");
        account.setStatus("ACTIVE"); account.setOrganization(org); return accounts.save(account);
    }

    private String plainText(Part part) throws Exception {
        if (part.isMimeType("text/plain")) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = plainText(multipart.getBodyPart(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Riyadh")).withNano(0); }
    private MockMultipartFile document() {
        return new MockMultipartFile("document", "test.pdf", "application/pdf", "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));
    }
}

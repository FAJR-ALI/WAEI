package org.fadhel.waeiplatform;

import jakarta.persistence.EntityManager;
import org.fadhel.waeiplatform.dto.in.TrainingRequestRequest;
import org.fadhel.waeiplatform.integration.UltraMsgClient;
import org.fadhel.waeiplatform.model.*;
import org.fadhel.waeiplatform.repository.TrainingRequestWhatsAppNotificationRepository;
import org.fadhel.waeiplatform.security.CurrentUser;
import org.fadhel.waeiplatform.service.TrainingRequestService;
import org.fadhel.waeiplatform.service.TrainingRequestWhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:waei-request-whatsapp;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class TrainingRequestWhatsAppIntegrationTests {
    @Autowired private TrainingRequestService requests;
    @Autowired private TrainingRequestWhatsAppService whatsApp;
    @Autowired private TrainingRequestWhatsAppNotificationRepository notifications;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private UltraMsgClient client;
    @MockitoBean private CurrentUser currentUser;
    private TransactionTemplate tx;
    private Long providerId;
    private Long beneficiaryId;
    private Long programId;
    private Long accountId;

    @BeforeEach
    void setup() {
        tx = new TransactionTemplate(transactionManager);
        notifications.deleteAll();
        when(client.isEnabled()).thenReturn(true);
        when(client.send(anyString(), anyString())).thenReturn("provider-message-id");
        tx.executeWithoutResult(status -> {
            var beneficiary = organization("مدرسة الأمل", "0500000001", OrganizationType.BENEFICIARY);
            var provider = organization("مركز التدريب", "+966500000002", OrganizationType.PROVIDER);
            beneficiaryId = beneficiary.getId();
            providerId = provider.getId();
            var account = new Account();
            account.setFullName("Test manager");
            account.setEmail(UUID.randomUUID() + "@example.org");
            account.setPassword("test-only");
            account.setRole("ORG_ADMIN");
            account.setStatus("ACTIVE");
            account.setOrganization(beneficiary);
            em.persist(account);
            accountId = account.getId();
            var program = new TrainingProgram();
            program.setTitle("أساسيات الإسعافات الأولية");
            program.setCategory("Safety");
            program.setDescription("Test program");
            program.setDeliveryMode("IN_PERSON");
            program.setPrice(new BigDecimal("100.00"));
            program.setHoursPerDay(new BigDecimal("2.0"));
            program.setMaxDays(3);
            program.setOrganization(provider);
            em.persist(program);
            programId = program.getId();
        });
        when(currentUser.getOrganizationId()).thenReturn(beneficiaryId);
        when(currentUser.getAccountId()).thenReturn(accountId);
    }

    @Test
    void newRequestNotifiesOnlyProviderThenAcceptanceNotifiesOnlyBeneficiary() {
        Long id = tx.execute(status -> {
            requests.addRequest(input());
            verify(client, never()).send(anyString(), anyString());
            return requestId();
        });
        verify(client).send(eq("+966500000002"), contains("طلب تدريب جديد"));
        verify(client, never()).send(eq("+966500000001"), anyString());
        when(currentUser.getOrganizationId()).thenReturn(providerId);
        requests.acceptRequest(id);
        verify(client).send(eq("+966500000001"), contains("أكملوا عملية الدفع لتأكيد الحجز"));
        assertEquals(2, notifications.count());
        for (var record : notifications.findAll()) {
            assertEquals("ACCEPTED", record.getStatus());
            assertTrue(record.getMessage().contains("رقم الطلب: #" + id));
            assertTrue(record.getMessage().contains("أساسيات الإسعافات الأولية"));
            assertTrue(record.getMessage().contains("\n\n"));
        }
        assertEquals(TrainingRequestStatus.ACCEPTED, tx.execute(status -> em.find(TrainingRequest.class, id).getStatus()));
        assertThrows(RuntimeException.class, () -> requests.acceptRequest(id));
        verify(client, times(2)).send(anyString(), anyString());
    }

    @Test
    void rolledBackCreationOrAcceptanceSendsNothingForThatEvent() {
        tx.executeWithoutResult(status -> {
            requests.addRequest(input());
            status.setRollbackOnly();
        });
        verify(client, never()).send(anyString(), anyString());
        assertEquals(0, notifications.count());
        requests.addRequest(input());
        Long id = tx.execute(status -> requestId());
        clearInvocations(client);
        when(currentUser.getOrganizationId()).thenReturn(providerId);
        tx.executeWithoutResult(status -> {
            requests.acceptRequest(id);
            status.setRollbackOnly();
        });
        verify(client, never()).send(anyString(), anyString());
        assertEquals(1, notifications.count());
        assertEquals(TrainingRequestStatus.PENDING, tx.execute(status -> em.find(TrainingRequest.class, id).getStatus()));
    }

    @Test
    void unauthorizedAcceptanceCannotNotifyBeneficiary() {
        requests.addRequest(input());
        Long id = tx.execute(status -> requestId());
        assertThrows(RuntimeException.class, () -> requests.acceptRequest(id));
        verify(client, never()).send(eq("+966500000001"), anyString());
        assertEquals(1, notifications.count());
    }

    @Test
    void providerFailureDoesNotUndoCreationOrAcceptance() {
        when(client.send(anyString(), anyString())).thenThrow(new IllegalStateException("provider failure"));
        assertDoesNotThrow(() -> requests.addRequest(input()));
        Long id = tx.execute(status -> requestId());
        when(currentUser.getOrganizationId()).thenReturn(providerId);
        assertDoesNotThrow(() -> requests.acceptRequest(id));
        assertEquals(TrainingRequestStatus.ACCEPTED, tx.execute(status -> em.find(TrainingRequest.class, id).getStatus()));
        assertEquals(2, notifications.count());
        assertTrue(notifications.findAll().stream().allMatch(n -> "FAILED".equals(n.getStatus())));
    }

    @Test
    void disabledFeatureDoesNotSend() {
        when(client.isEnabled()).thenReturn(false);
        requests.addRequest(input());
        Long id = tx.execute(status -> requestId());
        when(currentUser.getOrganizationId()).thenReturn(providerId);
        requests.acceptRequest(id);
        verify(client, never()).send(anyString(), anyString());
        assertEquals(0, notifications.count());
    }

    @Test
    void repeatedAndConcurrentCallbacksCannotResendNewRequest() throws Exception {
        requests.addRequest(input());
        Long id = tx.execute(status -> requestId());
        var pool = Executors.newFixedThreadPool(2);
        try {
            Runnable callback = () -> tx.executeWithoutResult(status ->
                    whatsApp.requestCreated(em.find(TrainingRequest.class, id)));
            var first = pool.submit(callback);
            var second = pool.submit(callback);
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        verify(client, times(1)).send(anyString(), anyString());
        assertEquals(1, notifications.count());
    }

    @Test
    void invalidPhoneIsRecordedWithoutBlockingTheRequest() {
        tx.executeWithoutResult(status -> em.find(Organization.class, providerId).setPhoneNumber(null));
        requests.addRequest(input());
        verify(client, never()).send(anyString(), anyString());
        assertEquals("FAILED", notifications.findAll().get(0).getStatus());
    }

    private TrainingRequestRequest input() {
        return TrainingRequestRequest.builder().trainingProgramId(programId).participantCount(10)
                .daysCount(1).scheduledStart(LocalDateTime.now().plusDays(7)).build();
    }

    private Long requestId() {
        return em.createQuery("select r.id from TrainingRequest r where r.trainingProgram.id = :program", Long.class)
                .setParameter("program", programId).getSingleResult();
    }

    private Organization organization(String name, String phone, OrganizationType type) {
        var organization = new Organization();
        organization.setName(name);
        organization.setPhoneNumber(phone);
        organization.setType(type);
        organization.setVerificationStatus(OrganizationVerificationStatus.VERIFIED);
        em.persist(organization);
        return organization;
    }
}

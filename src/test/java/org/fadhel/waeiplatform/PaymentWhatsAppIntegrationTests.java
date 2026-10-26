package org.fadhel.waeiplatform;

import jakarta.persistence.EntityManager;
import org.fadhel.waeiplatform.integration.MoyasarClient;
import org.fadhel.waeiplatform.integration.UltraMsgClient;
import org.fadhel.waeiplatform.model.*;
import org.fadhel.waeiplatform.repository.PaymentWhatsAppNotificationRepository;
import org.fadhel.waeiplatform.service.PaymentService;
import org.fadhel.waeiplatform.service.PaymentWhatsAppService;
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

@SpringBootTest(properties = {
        "moyasar.webhook.secret=whatsapp-test-secret",
        "spring.datasource.url=jdbc:h2:mem:waei-whatsapp-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
class PaymentWhatsAppIntegrationTests {
    @Autowired private PaymentService payments;
    @Autowired private PaymentWhatsAppService whatsApp;
    @Autowired private PaymentWhatsAppNotificationRepository notifications;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager em;
    @MockitoBean private UltraMsgClient client;
    private TransactionTemplate tx;
    private Long paymentId;
    private String invoiceId;
    private String remoteId;

    @BeforeEach
    void setup() {
        tx = new TransactionTemplate(transactionManager);
        notifications.deleteAll();
        when(client.isEnabled()).thenReturn(true);
        when(client.send(anyString(), anyString())).thenReturn("provider-message-id");
        invoiceId = "invoice-" + UUID.randomUUID();
        remoteId = "payment-" + UUID.randomUUID();
        paymentId = tx.execute(status -> {
            var beneficiary = organization("مدرسة المستفيد", "0500000001");
            var provider = organization("مقدم الخدمة", "+966500000002");
            var account = new Account();
            account.setFullName("Test manager");
            account.setEmail(UUID.randomUUID() + "@example.org");
            account.setPassword("test-only");
            account.setRole("ORG_ADMIN");
            account.setStatus("ACTIVE");
            account.setOrganization(beneficiary);
            em.persist(account);
            var program = new TrainingProgram();
            program.setTitle("التوعية والسلامة");
            program.setCategory("Safety");
            program.setDescription("Test program");
            program.setDeliveryMode("IN_PERSON");
            program.setPrice(new BigDecimal("100.00"));
            program.setOrganization(provider);
            em.persist(program);
            var request = new TrainingRequest();
            request.setParticipantCount(10);
            request.setStatus(TrainingRequestStatus.PAYMENT_PENDING);
            request.setRequestedAt(LocalDateTime.now());
            request.setProgramPrice(new BigDecimal("100.00"));
            request.setTrainingProgram(program);
            request.setOrganization(beneficiary);
            request.setAccount(account);
            em.persist(request);
            var payment = new Payment();
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(new BigDecimal("100.00"));
            payment.setCommissionRatePercent(new BigDecimal("10.00"));
            payment.setCommissionAmount(new BigDecimal("10.00"));
            payment.setProviderAmount(new BigDecimal("90.00"));
            payment.setMoyasarInvoiceId(invoiceId);
            payment.setTrainingRequest(request);
            em.persist(payment);
            return payment.getId();
        });
    }

    @Test
    void confirmedPaymentSendsBothMessagesOnlyAfterCommitAndDoesNotRepeat() {
        tx.executeWithoutResult(status -> {
            payments.handleWebhook(webhook("paid", 10000));
            verify(client, never()).send(anyString(), anyString());
        });
        verify(client).send(eq("+966500000001"), contains("تم استلام دفعتكم للبرنامج التالي:"));
        verify(client).send(eq("+966500000002"), contains("مدرسة المستفيد"));
        assertEquals(2, notifications.count());
        assertTrue(notifications.findAll().stream().allMatch(n -> "ACCEPTED".equals(n.getStatus())));
        for (var notification : notifications.findAll()) {
            assertFalse(notification.getMessage().contains("رقم عملية الدفع"));
            assertTrue(notification.getMessage().contains("📋 رقم الطلب: #"));
            assertTrue(notification.getMessage().contains("\n\n"));
            assertTrue(notification.getMessage().endsWith("بواعي 💜")
                    || notification.getMessage().endsWith("مع واعي 💜"));
        }
        assertEquals(TrainingRequestStatus.CONFIRMED, tx.execute(status ->
                em.find(Payment.class, paymentId).getTrainingRequest().getStatus()));
        payments.handleWebhook(webhook("paid", 10000));
        verify(client, times(2)).send(anyString(), contains("100.00 \u20C1"));
    }

    @Test
    void rollbackOrWrongAmountDoesNotSendAnything() {
        tx.executeWithoutResult(status -> {
            payments.handleWebhook(webhook("paid", 10000));
            status.setRollbackOnly();
        });
        assertThrows(RuntimeException.class, () -> payments.handleWebhook(webhook("paid", 9999)));
        verify(client, never()).send(anyString(), anyString());
        assertEquals(0, notifications.count());
        assertEquals(PaymentStatus.PENDING, tx.execute(status -> em.find(Payment.class, paymentId).getStatus()));
    }

    @Test
    void oneRecipientFailureDoesNotUndoPaymentOrBlockTheOther() {
        when(client.send(eq("+966500000001"), anyString())).thenThrow(new IllegalStateException("provider failure"));
        assertDoesNotThrow(() -> payments.handleWebhook(webhook("paid", 10000)));
        verify(client).send(eq("+966500000002"), anyString());
        assertEquals(PaymentStatus.PAID, tx.execute(status -> em.find(Payment.class, paymentId).getStatus()));
        assertEquals(1, notifications.findAll().stream().filter(n -> "FAILED".equals(n.getStatus())).count());
        assertEquals(1, notifications.findAll().stream().filter(n -> "ACCEPTED".equals(n.getStatus())).count());
    }

    @Test
    void disabledFeatureDoesNotSend() {
        when(client.isEnabled()).thenReturn(false);
        payments.handleWebhook(webhook("paid", 10000));
        verify(client, never()).send(anyString(), anyString());
        assertEquals(0, notifications.count());
    }

    @Test
    void failedPaymentDoesNotSend() {
        payments.handleWebhook(webhook("failed", 10000));
        verify(client, never()).send(anyString(), anyString());
        assertEquals(0, notifications.count());
        assertEquals(PaymentStatus.FAILED, tx.execute(status -> em.find(Payment.class, paymentId).getStatus()));
    }

    @Test
    void missingBeneficiaryPhoneStillSendsToProvider() {
        tx.executeWithoutResult(status -> em.find(Payment.class, paymentId).getTrainingRequest()
                .getOrganization().setPhoneNumber(null));
        payments.handleWebhook(webhook("paid", 10000));
        verify(client, times(1)).send(eq("+966500000002"), anyString());
        assertEquals(2, notifications.count());
    }

    @Test
    void concurrentCallbacksCannotClaimTheSameRecipientTwice() throws Exception {
        tx.executeWithoutResult(status -> em.find(Payment.class, paymentId).setStatus(PaymentStatus.PAID));
        var pool = Executors.newFixedThreadPool(2);
        try {
            Runnable callback = () -> tx.executeWithoutResult(status ->
                    whatsApp.paymentConfirmed(em.find(Payment.class, paymentId)));
            var first = pool.submit(callback);
            var second = pool.submit(callback);
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(2, notifications.count());
        verify(client, times(2)).send(anyString(), anyString());
    }

    private Organization organization(String name, String phone) {
        var organization = new Organization();
        organization.setName(name);
        organization.setPhoneNumber(phone);
        em.persist(organization);
        return organization;
    }

    private MoyasarClient.MoyasarWebhook webhook(String status, int amount) {
        var remote = new MoyasarClient.MoyasarPayment(remoteId, status, amount, "SAR", invoiceId,
                null, null, null, null);
        return new MoyasarClient.MoyasarWebhook("event-test", "payment_updated", "whatsapp-test-secret", false, remote);
    }
}

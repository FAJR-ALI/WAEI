package org.fadhel.waeiplatform.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.integration.UltraMsgClient;
import org.fadhel.waeiplatform.model.*;
import org.fadhel.waeiplatform.repository.PaymentWhatsAppNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class PaymentWhatsAppService {
    private final UltraMsgClient client;
    private final PaymentWhatsAppNotificationRepository notifications;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    public PaymentWhatsAppService(UltraMsgClient client, PaymentWhatsAppNotificationRepository notifications,
                                 EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.client = client;
        this.notifications = notifications;
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void paymentConfirmed(Payment payment) {
        if (!client.isEnabled() || payment.getStatus() != PaymentStatus.PAID) return;
        // Capture names and numbers while the payment transaction is still open.
        TrainingRequest request = payment.getTrainingRequest();
        if (request == null || request.getTrainingProgram() == null) return;
        Organization beneficiary = request.getOrganization();
        Organization provider = request.getTrainingProgram().getOrganization();
        String currency = "SAR".equalsIgnoreCase(payment.getCurrency()) ? "\u20C1" : payment.getCurrency();
        String details = "📚 البرنامج: " + shortName(request.getTrainingProgram().getTitle())
                + "\n📋 رقم الطلب: #" + request.getId()
                + "\n💳 المبلغ المدفوع: " + payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                + " " + currency;
        var recipients = List.of(
                new Recipient("BENEFICIARY", beneficiary == null ? null : beneficiary.getPhoneNumber(),
                        "واعي | تم الدفع بنجاح ✅\n\nتم استلام دفعتكم للبرنامج التالي:\n\n"
                                + details + "\n\nشكرًا لثقتكم بواعي 💜"),
                new Recipient("PROVIDER", provider == null ? null : provider.getPhoneNumber(),
                        "واعي | تأكيد الدفع ✅\n\nتم تأكيد دفع الجهة المستفيدة بنجاح.\n\n🏢 الجهة المستفيدة: "
                                + (beneficiary == null ? "" : shortName(beneficiary.getName()))
                                + "\n" + details + "\n\nشكرًا لشراكتكم مع واعي 💜"));
        Long paymentId = payment.getId();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Recipient recipient : recipients) {
                    try {
                        sendOnce(paymentId, recipient);
                    } catch (Exception ex) {
                        // Never expose numbers, credentials or provider responses in logs.
                        log.warn("WhatsApp notification failed for payment {} role {}", paymentId, recipient.role());
                    }
                }
            }
        });
    }

    private void sendOnce(Long paymentId, Recipient recipient) {
        Long notificationId = transactions.execute(tx -> {
            // Serialize concurrent callbacks for the same payment across application instances.
            Payment saved = entityManager.find(Payment.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
            if (saved == null || saved.getStatus() != PaymentStatus.PAID
                    || notifications.existsByPaymentIdAndRecipientRole(paymentId, recipient.role())) return null;
            var notification = new PaymentWhatsAppNotification();
            notification.setPaymentId(paymentId);
            notification.setRecipientRole(recipient.role());
            notification.setRecipient(normalizePhone(recipient.phone()));
            notification.setMessage(recipient.message());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setStatus(notification.getRecipient() == null ? "FAILED" : "ATTEMPTED");
            if (notification.getRecipient() == null) notification.setError("Missing or invalid phone number");
            notifications.saveAndFlush(notification);
            return notification.getRecipient() == null ? null : notification.getId();
        });
        if (notificationId == null) return;
        String providerId = null;
        try {
            providerId = client.send(normalizePhone(recipient.phone()), recipient.message());
        } catch (Exception ex) {
            log.warn("WhatsApp acceptance not confirmed for payment {} role {}", paymentId, recipient.role());
        }
        String acceptedId = providerId;
        transactions.executeWithoutResult(tx -> {
            var notification = notifications.findById(notificationId).orElseThrow();
            notification.setStatus(acceptedId == null ? "FAILED" : "ACCEPTED");
            notification.setProviderMessageId(acceptedId);
            notification.setError(acceptedId == null ? "Check provider logs before retrying: acceptance not confirmed" : null);
            if (acceptedId != null) notification.setAcceptedAt(LocalDateTime.now());
            notifications.save(notification);
        });
    }

    static String normalizePhone(String phone) {
        if (phone == null) return null;
        String value = phone.replaceAll("[\\s()\\-]", "");
        if (value.matches("05[0-9]{8}")) value = "+966" + value.substring(1);
        else if (value.startsWith("00")) value = "+" + value.substring(2);
        else if (value.matches("9665[0-9]{8}")) value = "+" + value;
        return value.matches("\\+[1-9][0-9]{7,14}") ? value : null;
    }

    private static String shortName(String name) {
        if (name == null) return "";
        return name.length() <= 150 ? name : name.substring(0, 150);
    }

    private record Recipient(String role, String phone, String message) {}
}

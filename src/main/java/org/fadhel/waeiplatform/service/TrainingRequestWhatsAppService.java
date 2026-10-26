package org.fadhel.waeiplatform.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.integration.UltraMsgClient;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.model.TrainingRequestWhatsAppNotification;
import org.fadhel.waeiplatform.repository.TrainingRequestWhatsAppNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class TrainingRequestWhatsAppService {
    private final UltraMsgClient client;
    private final TrainingRequestWhatsAppNotificationRepository notifications;
    private final EntityManager em;
    private final TransactionTemplate transactions;

    public TrainingRequestWhatsAppService(UltraMsgClient client, TrainingRequestWhatsAppNotificationRepository notifications,
                                         EntityManager em, PlatformTransactionManager transactionManager) {
        this.client = client;
        this.notifications = notifications;
        this.em = em;
        transactions = new TransactionTemplate(transactionManager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void requestCreated(TrainingRequest request) {
        if (!client.isEnabled() || request.getStatus() != TrainingRequestStatus.PENDING
                || request.getTrainingProgram() == null) return;
        Organization provider = request.getTrainingProgram().getOrganization();
        String message = "واعي | طلب تدريب جديد 📩\n\nوصل طلب جديد على برنامجكم التدريبي.\n\n"
                + "🏢 الجهة المستفيدة: " + name(request.getOrganization()) + "\n"
                + details(request)
                + "\n\nيرجى الدخول إلى منصة واعي لمراجعة الطلب وقبوله أو رفضه."
                + "\n\nشكرًا لشراكتكم مع واعي 💜";
        schedule(request.getId(), "REQUEST_CREATED", provider == null ? null : provider.getPhoneNumber(), message);
    }

    public void requestAccepted(TrainingRequest request) {
        if (!client.isEnabled() || request.getStatus() != TrainingRequestStatus.ACCEPTED
                || request.getTrainingProgram() == null) return;
        Organization beneficiary = request.getOrganization();
        String message = "واعي | تم قبول طلبكم ✅\n\nتم قبول طلبكم من الجهة المقدّمة للخدمة.\n\n"
                + "🏢 مقدّم الخدمة: " + name(request.getTrainingProgram().getOrganization()) + "\n"
                + details(request)
                + "\n\n💳 الخطوة التالية: ادخلوا إلى منصة واعي وأكملوا عملية الدفع لتأكيد الحجز."
                + "\n\nشكرًا لثقتكم بواعي 💜";
        schedule(request.getId(), "REQUEST_ACCEPTED", beneficiary == null ? null : beneficiary.getPhoneNumber(), message);
    }

    private void schedule(Long requestId, String event, String phone, String message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sendOnce(requestId, event, phone, message);
                } catch (Exception ex) {
                    // Notification failure must not undo the request or expose credentials.
                    log.warn("WhatsApp notification failed for request {} event {}", requestId, event);
                }
            }
        });
    }

    private void sendOnce(Long requestId, String event, String phone, String message) {
        String recipient = PaymentWhatsAppService.normalizePhone(phone);
        Long notificationId = transactions.execute(tx -> {
            // The lock and unique key reserve one attempt per request/event across servers.
            if (em.find(TrainingRequest.class, requestId, LockModeType.PESSIMISTIC_WRITE) == null
                    || notifications.existsByTrainingRequestIdAndEventType(requestId, event)) return null;
            var notification = new TrainingRequestWhatsAppNotification();
            notification.setTrainingRequestId(requestId);
            notification.setEventType(event);
            notification.setRecipient(recipient);
            notification.setMessage(message);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setStatus(recipient == null ? "FAILED" : "ATTEMPTED");
            if (recipient == null) notification.setError("Missing or invalid phone number");
            notifications.saveAndFlush(notification);
            return recipient == null ? null : notification.getId();
        });
        if (notificationId == null) return;
        String providerId = null;
        try {
            providerId = client.send(recipient, message);
        } catch (Exception ex) {
            log.warn("WhatsApp acceptance not confirmed for request {} event {}", requestId, event);
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

    private static String details(TrainingRequest request) {
        String title = request.getTrainingProgram().getTitle();
        String date = request.getScheduledStart() == null ? "" : "\n🗓️ موعد البداية: "
                + request.getScheduledStart().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + " (بتوقيت الرياض)";
        return "📚 البرنامج: " + shorten(title) + "\n📋 رقم الطلب: #" + request.getId() + date;
    }

    private static String name(Organization organization) {
        return organization == null ? "" : shorten(organization.getName());
    }

    private static String shorten(String text) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), 150));
    }
}

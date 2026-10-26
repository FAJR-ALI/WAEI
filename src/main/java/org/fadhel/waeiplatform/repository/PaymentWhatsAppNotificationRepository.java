package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.PaymentWhatsAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWhatsAppNotificationRepository extends JpaRepository<PaymentWhatsAppNotification, Long> {
    boolean existsByPaymentIdAndRecipientRole(Long paymentId, String recipientRole);
}

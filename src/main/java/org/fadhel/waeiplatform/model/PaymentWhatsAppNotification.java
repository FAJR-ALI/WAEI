package org.fadhel.waeiplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_whatsapp_notifications", uniqueConstraints =
        @UniqueConstraint(columnNames = {"payment_id", "recipient_role"}))
@Getter
@Setter
@NoArgsConstructor
public class PaymentWhatsAppNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "recipient_role", nullable = false, length = 20)
    private String recipientRole;
    private String recipient;
    @Column(length = 1000)
    private String message;
    // Saved before sending so repeated callbacks cannot send duplicate messages.
    private String status;
    private String providerMessageId;
    private String error;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
}

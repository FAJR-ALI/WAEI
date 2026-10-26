package org.fadhel.waeiplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_request_whatsapp_notifications", uniqueConstraints =
        @UniqueConstraint(columnNames = {"training_request_id", "event_type"}))
@Getter
@Setter
@NoArgsConstructor
public class TrainingRequestWhatsAppNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "training_request_id", nullable = false)
    private Long trainingRequestId;
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;
    private String recipient;
    @Column(length = 1000)
    private String message;
    private String status;
    private String providerMessageId;
    private String error;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
}

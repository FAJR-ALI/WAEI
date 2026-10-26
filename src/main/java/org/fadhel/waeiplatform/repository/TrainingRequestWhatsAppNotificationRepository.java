package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.TrainingRequestWhatsAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingRequestWhatsAppNotificationRepository extends JpaRepository<TrainingRequestWhatsAppNotification, Long> {
    boolean existsByTrainingRequestIdAndEventType(Long trainingRequestId, String eventType);
}

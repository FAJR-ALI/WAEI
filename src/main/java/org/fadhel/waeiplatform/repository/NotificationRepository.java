package org.fadhel.waeiplatform.repository;

import org.fadhel.waeiplatform.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

}

package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.fadhel.waeiplatform.dto.in.NotificationDTOIn;
import org.fadhel.waeiplatform.dto.out.NotificationDTOOut;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Notification;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    // CONVERT ENTITY TO DTO OUT
    private NotificationDTOOut convertToDTO(Notification notification) {

        NotificationDTOOut dto = new NotificationDTOOut();

        dto.setId(notification.getId());
        dto.setChannel(notification.getChannel());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRecipient(notification.getRecipient());
        dto.setStatus(notification.getStatus());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setSentAt(notification.getSentAt());

        if (notification.getAccount() != null) {
            dto.setAccountId(notification.getAccount().getId());
        }
        return dto;
    }

    // GET ALL
    public List<NotificationDTOOut> getAllNotifications() {
        return notificationRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    // GET BY ID
    public NotificationDTOOut getNotificationById(Integer id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ApiException("Notification not found"));

        return convertToDTO(notification);
    }

    // ADD
    public void addNotification(NotificationDTOIn dto) {

        Notification notification = new Notification();

        notification.setChannel(dto.getChannel());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setRecipient(dto.getRecipient());
        notification.setStatus("PENDING");

        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId()).orElseThrow(() -> new ApiException("Account not found"));
            notification.setAccount(account);
        }
        notificationRepository.save(notification);
    }

    // UPDATE
    public void updateNotification(Integer id, NotificationDTOIn dto) {

        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ApiException("Notification not found"));
        if (!notification.getStatus().equals("PENDING")) {
            throw new ApiException("Only pending notifications can be updated");
        }

        notification.setChannel(dto.getChannel());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setRecipient(dto.getRecipient());

        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId()).orElseThrow(() -> new ApiException("Account not found"));
            notification.setAccount(account);
        } else {
            notification.setAccount(null);
        }
        notificationRepository.save(notification);
    }

    // DELETE
    public void deleteNotification(Integer id) {

        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ApiException("Notification not found"));
        notificationRepository.delete(notification);
    }
}

package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class OrganizationEmailService {
    private final EmailService emailService;

    public void sendRegistration(Organization organization, Account manager) {
        if (manager == null) return;
        sendAfterCommit(manager.getEmail(), "واعي | تم استلام تسجيل جهتكم",
                "مرحبًا " + manager.getFullName() + "،\n\nتم استلام تسجيل جهتكم «" + organization.getName()
                        + "» في منصة واعي.\n\nحسابكم بانتظار موافقة إدارة المنصة.\nسنرسل لكم إشعارًا عند اكتمال المراجعة وتوثيق الحساب.\n\nفريق واعي");
    }

    public void sendVerification(Organization organization, Account manager) {
        if (manager == null) return;
        sendAfterCommit(manager.getEmail(), "واعي | تم توثيق حساب جهتكم",
                "مرحبًا " + manager.getFullName() + "،\n\nتم توثيق حساب جهتكم «" + organization.getName()
                        + "» في منصة واعي بنجاح.\n\nيمكنكم الآن تسجيل الدخول والاستفادة من خدمات المنصة.\n\nشكرًا لانضمامكم إلى واعي.\nفريق واعي");
    }

    public void sendRejection(Organization organization, Account manager, String reason) {
        if (manager == null) return;
        sendAfterCommit(manager.getEmail(), "واعي | تحديث بشأن طلب تسجيل جهتكم",
                "مرحبًا " + manager.getFullName() + "،\n\nنود إبلاغكم بأنه تم رفض طلب تسجيل جهتكم «"
                        + organization.getName() + "» في منصة واعي.\n\nسبب الرفض: " + reason
                        + "\n\nيمكنكم تحديث بيانات الطلب والمستندات وإعادة تقديمه للمراجعة.\n\nفريق واعي");
    }

    private void sendAfterCommit(String to, String subject, String text) {
        if (!emailService.isEnabled()) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailService.send(to, subject, text);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // An SMTP failure must not undo a successful registration or approval.
                emailService.send(to, subject, text);
            }
        });
    }
}

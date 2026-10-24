package org.fadhel.waeiplatform.service;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.model.Account;
import org.fadhel.waeiplatform.model.Organization;
import org.fadhel.waeiplatform.model.TrainingRequest;
import org.fadhel.waeiplatform.model.TrainingRequestStatus;
import org.fadhel.waeiplatform.repository.AccountRepository;
import org.fadhel.waeiplatform.repository.TrainingRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrainingReminderService {
    private final TrainingRequestRepository requests;
    private final AccountRepository accounts;
    private final EmailService emailService;

    @Transactional
    public Result sendDueReminders() {
        if (!emailService.isEnabled()) return new Result(0, 0, 0);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
        // Lock due requests so overlapping n8n calls cannot send the same saved reminder twice.
        var due = requests.findDueReminders(TrainingRequestStatus.CONFIRMED.name(), now, now.plusHours(24));
        int sent = 0;
        int failed = 0;
        for (TrainingRequest request : due) {
            LocalDateTime start = request.getScheduledStart();
            if (request.getStatus() != TrainingRequestStatus.CONFIRMED || start == null
                    || !start.isAfter(now) || start.isAfter(now.plusHours(24))) continue;
            Organization provider = request.getTrainingProgram().getOrganization();
            Organization beneficiary = request.getOrganization();
            if (!Objects.equals(start, request.getProviderReminderForStart())) {
                if (sendReminder(request, provider, beneficiary, true)) {
                    request.setProviderReminderForStart(start);
                    sent++;
                } else failed++;
            }
            if (!Objects.equals(start, request.getBeneficiaryReminderForStart())) {
                if (sendReminder(request, beneficiary, provider, false)) {
                    request.setBeneficiaryReminderForStart(start);
                    sent++;
                } else failed++;
            }
        }
        return new Result(due.size(), sent, failed);
    }

    private boolean sendReminder(TrainingRequest request, Organization recipientOrganization,
                                 Organization otherOrganization, boolean provider) {
        if (recipientOrganization == null || otherOrganization == null) return false;
        Account manager = accounts.findAccountsByOrganization_Id(recipientOrganization.getId()).stream()
                .filter(account -> "ORG_ADMIN".equals(account.getRole()))
                .filter(account -> "ACTIVE".equals(account.getStatus()))
                .findFirst().orElse(null);
        if (manager == null) return false;
        String time = request.getScheduledStart().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
        String title = request.getTrainingProgram().getTitle();
        String text = "مرحبًا " + manager.getFullName() + "،\n"
                + (provider ? "نذكّركم بموعد تقديم برنامج «" : "نذكّركم بموعد حضور برنامج «") + title + "» "
                + (provider ? "للجهة المستفيدة «" : "المقدّم من الجهة «") + otherOrganization.getName()
                + "».\nالموعد: " + time + " بتوقيت السعودية.\nفريق واعي";
        return emailService.send(manager.getEmail(), "واعي | تذكير بموعد برنامج " + title, text);
    }

    public record Result(int checkedRequests, int sentEmails, int failedEmails) { }
}

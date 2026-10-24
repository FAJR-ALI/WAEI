package org.fadhel.waeiplatform.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.enabled:false}") boolean enabled,
                        @Value("${app.email.from:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() { return enabled && !from.isBlank(); }

    public boolean send(String to, String subject, String text) {
        if (!isEnabled() || to == null || to.isBlank()) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "واعي");
            helper.setTo(to);
            helper.setSubject(subject);
            // Keep a plain-text alternative for email clients that do not display HTML.
            helper.setText(text, renderHtml(subject, text));
            helper.addInline("waei-logo", new ClassPathResource("static/img/logo.png"), "image/png");
            mailSender.send(message);
            return true;
        } catch (MailException | MessagingException | IOException ex) {
            // Do not expose SMTP passwords, message bodies or recipients in application logs.
            log.warn("Email send failed ({})", ex.getClass().getSimpleName());
            return false;
        }
    }

    private String renderHtml(String subject, String text) throws IOException {
        String title = subject.startsWith("واعي | ") ? subject.substring("واعي | ".length()) : subject;
        String paragraphs = Arrays.stream(text.split("\\R"))
                .filter(line -> !line.isBlank())
                .map(line -> "<p style=\"margin:0 0 18px;color:#403b50;font-family:Tahoma,Arial,sans-serif;"
                        + "font-size:16px;line-height:2;text-align:right;\">"
                        + HtmlUtils.htmlEscape(line) + "</p>")
                .collect(Collectors.joining("\n"));
        try (var input = new ClassPathResource("templates/email/branded-message.html").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{subject}}", HtmlUtils.htmlEscape(title))
                    .replace("{{content}}", paragraphs);
        }
    }
}

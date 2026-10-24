package org.fadhel.waeiplatform.service;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);

    private void prepareMessage() {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsBrandedArabicHtmlWithPlainTextAndEmbeddedLogo() throws Exception {
        prepareMessage();
        assertTrue(new EmailService(sender, true, "waei@example.org").send("manager@example.org", "واعي | تم التوثيق", "مرحبًا،\nتم التوثيق"));
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captured.capture());
        MimeMessage message = captured.getValue();
        message.saveChanges();
        assertEquals("waei@example.org", ((InternetAddress) message.getFrom()[0]).getAddress());
        assertEquals("واعي", ((InternetAddress) message.getFrom()[0]).getPersonal());
        assertEquals("manager@example.org", message.getAllRecipients()[0].toString());
        assertEquals("واعي | تم التوثيق", message.getSubject());
        assertEquals("مرحبًا،\nتم التوثيق", content(message, "text/plain"));
        String html = content(message, "text/html");
        assertTrue(html.contains("dir=\"rtl\""));
        assertTrue(html.contains("cid:waei-logo"));
        assertTrue(html.contains("مرحبًا،"));
        assertTrue(html.contains("تم التوثيق"));
        Part logo = findLogo(message);
        assertNotNull(logo);
        assertTrue(logo.isMimeType("image/png"));
        assertTrue(logo.getInputStream().readAllBytes().length > 0);
    }

    @Test
    void escapesDynamicTextRatherThanAllowingInjectedHtml() throws Exception {
        prepareMessage();
        new EmailService(sender, true, "waei@example.org").send("manager@example.org", "<script>title</script>", "<img src=x onerror=alert(1)>");
        var captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captured.capture());
        captured.getValue().saveChanges();
        String html = content(captured.getValue(), "text/html");
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&lt;img"));
        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<img src=x"));
    }

    @Test
    void disabledMailNeverConnectsToSmtp() {
        assertFalse(new EmailService(sender, false, "waei@example.org").send("manager@example.org", "Test", "Test"));
        verifyNoInteractions(sender);
    }

    @Test
    void smtpFailureReturnsFalseInsteadOfFailingRegistration() {
        prepareMessage();
        doThrow(new MailSendException("Simulated SMTP failure")).when(sender).send(any(MimeMessage.class));
        assertFalse(new EmailService(sender, true, "waei@example.org").send("manager@example.org", "Test", "Test"));
    }

    private String content(Part part, String type) throws Exception {
        if (part.isMimeType(type)) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = content(multipart.getBodyPart(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Part findLogo(Part part) throws Exception {
        String[] ids = part.getHeader("Content-ID");
        if (ids != null && "<waei-logo>".equals(ids[0])) return part;
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                Part found = findLogo(multipart.getBodyPart(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}

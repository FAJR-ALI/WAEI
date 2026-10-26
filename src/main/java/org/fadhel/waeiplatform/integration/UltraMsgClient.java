package org.fadhel.waeiplatform.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
public class UltraMsgClient {
    private final boolean enabled;
    private final String instanceId;
    private final String token;
    private final RestClient client;

    @Autowired
    public UltraMsgClient(@Value("${app.whatsapp.enabled:false}") boolean enabled,
                          @Value("${ultramsg.instance-id:}") String instanceId,
                          @Value("${ultramsg.token:}") String token) {
        this(enabled, instanceId, token, createClient());
    }

    UltraMsgClient(boolean enabled, String instanceId, String token, RestClient client) {
        this.enabled = enabled;
        this.instanceId = instanceId;
        this.token = token;
        this.client = client;
    }

    public boolean isEnabled() {
        return enabled && instanceId.matches("instance[0-9]+") && !token.isBlank();
    }

    // Acceptance can mean the message is queued; it does not confirm delivery.
    public String send(String to, String message) {
        if (!isEnabled()) throw new IllegalStateException("WhatsApp is not configured");
        var form = new LinkedMultiValueMap<String, String>();
        form.add("token", token);
        form.add("to", to);
        if (message.length() > 1024) throw new IllegalArgumentException("WhatsApp caption is too long");
        try (var logo = new ClassPathResource("static/img/logo.png").getInputStream()) {
            form.add("image", Base64.getEncoder().encodeToString(logo.readAllBytes()));
        } catch (IOException ex) {
            throw new IllegalStateException("Waei logo could not be loaded");
        }
        form.add("caption", message);
        Map<?, ?> response = client.post()
                .uri("https://api.ultramsg.com/{instance}/messages/image", instanceId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(Map.class);
        if (response == null || !"true".equals(String.valueOf(response.get("sent")))
                || response.get("id") == null || String.valueOf(response.get("id")).isBlank()) {
            throw new IllegalStateException("WhatsApp provider did not accept the message");
        }
        return String.valueOf(response.get("id"));
    }

    private static RestClient createClient() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(7));
        return RestClient.builder().requestFactory(factory).build();
    }
}

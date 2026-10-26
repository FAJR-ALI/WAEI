package org.fadhel.waeiplatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UltraMsgClientTest {
    @Test
    void sendsActualLogoAndArabicCaptionAsEncodedForm() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new UltraMsgClient(true, "instance123", "test-token", builder.build());
        String caption = "واعي | تم الدفع بنجاح";
        String image;
        try (var stream = new ClassPathResource("static/img/logo.png").getInputStream()) {
            image = Base64.getEncoder().encodeToString(stream.readAllBytes());
        }
        server.expect(requestTo("https://api.ultramsg.com/instance123/messages/image"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(allOf(containsString("token=test-token"),
                        containsString("to=%2B966500000001"),
                        containsString("caption=" + URLEncoder.encode(caption, StandardCharsets.UTF_8)),
                        containsString("image=" + URLEncoder.encode(image, StandardCharsets.UTF_8)))))
                .andRespond(withSuccess("{\"sent\":\"true\",\"id\":42,\"message\":\"ok\"}", MediaType.APPLICATION_JSON));
        assertEquals("42", client.send("+966500000001", caption));
        server.verify();
    }

    @Test
    void rejectsProviderErrorEvenWhenHttpStatusIsSuccessful() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new UltraMsgClient(true, "instance123", "test-token", builder.build());
        server.expect(requestTo("https://api.ultramsg.com/instance123/messages/image"))
                .andRespond(withSuccess("{\"error\":\"invalid token\"}", MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> client.send("+966500000001", "test"));
        server.verify();
    }

    @Test
    void disabledOrIncompleteConfigurationCannotSend() {
        assertFalse(new UltraMsgClient(false, "instance123", "fake", RestClient.create()).isEnabled());
        assertFalse(new UltraMsgClient(true, "123", "fake", RestClient.create()).isEnabled());
        var client = new UltraMsgClient(true, "instance123", "", RestClient.create());
        assertThrows(IllegalStateException.class, () -> client.send("+966500000001", "test"));
    }
}

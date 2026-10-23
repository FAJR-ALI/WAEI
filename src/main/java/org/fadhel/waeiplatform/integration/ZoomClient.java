package org.fadhel.waeiplatform.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.fadhel.waeiplatform.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class ZoomClient {

    private final RestClient restClient;
    private final String tokenUrl;
    private final String accountId;
    private final String clientId;
    private final String clientSecret;
    private final String hostEmail;

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public ZoomClient(@Value("${zoom.api.base-url:https://api.zoom.us/v2}") String baseUrl,
                      @Value("${zoom.oauth.token-url:https://zoom.us/oauth/token}") String tokenUrl,
                      @Value("${zoom.account-id:}") String accountId,
                      @Value("${zoom.client-id:}") String clientId,
                      @Value("${zoom.client-secret:}") String clientSecret,
                      @Value("${zoom.host-email:}") String hostEmail) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.tokenUrl = tokenUrl;
        this.accountId = accountId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.hostEmail = hostEmail;
    }

    public boolean isConfigured() {
        return hasText(accountId) && hasText(clientId) && hasText(clientSecret) && hasText(hostEmail);
    }

    public ZoomMeeting createMeeting(MeetingSpec spec) {
        requireConfigured();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", spec.topic());
        body.put("type", spec.type());
        if (spec.startTime() != null) {
            body.put("start_time", spec.startTime());
        }
        if (spec.durationMinutes() != null) {
            body.put("duration", spec.durationMinutes());
        }
        body.put("timezone", spec.timezone());
        body.put("settings", meetingSettings());
        if (spec.recurringOccurrences() != null) {
            Map<String, Object> recurrence = new LinkedHashMap<>();
            recurrence.put("type", 1);
            recurrence.put("repeat_interval", 1);
            recurrence.put("end_times", spec.recurringOccurrences());
            body.put("recurrence", recurrence);
        }

        try {
            ZoomMeeting meeting = restClient.post()
                    .uri("/users/{userId}/meetings", hostEmail)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ZoomMeeting.class);

            if (meeting == null || !hasText(meeting.id()) || !hasText(meeting.joinUrl())) {
                throw new ApiException("Zoom did not return a meeting link");
            }
            return meeting;
        } catch (RestClientResponseException e) {
            throw zoomError("create the meeting", e);
        } catch (RestClientException e) {
            throw new ApiException("Could not reach Zoom: " + e.getMessage());
        }
    }

    public String fetchHostStartUrl(String meetingId) {
        requireConfigured();

        try {
            ZoomMeeting meeting = restClient.get()
                    .uri("/meetings/{id}", meetingId)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(ZoomMeeting.class);

            if (meeting == null || !hasText(meeting.startUrl())) {
                throw new ApiException("Zoom did not return a host start link");
            }
            return meeting.startUrl();
        } catch (RestClientResponseException e) {
            throw zoomError("fetch the meeting", e);
        } catch (RestClientException e) {
            throw new ApiException("Could not reach Zoom: " + e.getMessage());
        }
    }

    public void deleteMeeting(String meetingId) {
        requireConfigured();

        try {
            restClient.delete()
                    .uri("/meetings/{id}", meetingId)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw zoomError("delete the meeting", e);
        } catch (RestClientException e) {
            throw new ApiException("Could not reach Zoom: " + e.getMessage());
        }
    }

    private Map<String, Object> meetingSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("host_video", true);
        settings.put("participant_video", false);
        settings.put("join_before_host", false);
        settings.put("waiting_room", true);
        settings.put("mute_upon_entry", true);
        settings.put("auto_recording", "none");
        settings.put("approval_type", 2);
        return settings;
    }

    private String accessToken() {
        String token = accessToken;
        if (token != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
            return token;
        }

        synchronized (this) {
            if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
                return accessToken;
            }

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "account_credentials");
            form.add("account_id", accountId);

            try {
                ZoomToken response = restClient.post()
                        .uri(tokenUrl)
                        .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(ZoomToken.class);

                if (response == null || !hasText(response.accessToken())) {
                    throw new ApiException("Zoom did not return an access token");
                }

                int expiresIn = response.expiresIn() != null ? response.expiresIn() : 3600;
                accessToken = response.accessToken();
                accessTokenExpiresAt = Instant.now().plusSeconds(expiresIn);
                return accessToken;
            } catch (RestClientResponseException e) {
                throw zoomError("authenticate", e);
            } catch (RestClientException e) {
                throw new ApiException("Could not reach Zoom: " + e.getMessage());
            }
        }
    }

    private ApiException zoomError(String action, RestClientResponseException e) {
        String message = "Zoom could not " + action + " (HTTP " + e.getStatusCode().value() + ")";
        String body = e.getResponseBodyAsString();
        if (hasText(body)) {
            message += ": " + (body.length() > 400 ? body.substring(0, 400) : body);
        }
        log.warn(message);
        return new ApiException(message);
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException("Zoom is not configured. Set ZOOM_ACCOUNT_ID, ZOOM_CLIENT_ID, "
                    + "ZOOM_CLIENT_SECRET, and ZOOM_HOST_EMAIL.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record MeetingSpec(
            String topic,
            int type,
            String startTime,
            Integer durationMinutes,
            String timezone,
            Integer recurringOccurrences) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ZoomMeeting(
            String id,
            @JsonProperty("join_url") String joinUrl,
            @JsonProperty("start_url") String startUrl,
            String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ZoomToken(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Integer expiresIn) {
    }
}

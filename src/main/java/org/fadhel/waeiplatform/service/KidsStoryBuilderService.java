package org.fadhel.waeiplatform.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.fadhel.waeiplatform.dto.ai.AiStoryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class KidsStoryBuilderService {
    private static final Map<String, String> CHARACTERS = Map.of("layan", "Layan, a girl", "saad", "Saad, a boy", "rabbit", "a little rabbit", "cat", "a little cat");
    private static final Map<String, String> PLACES = Map.of("home", "at home", "school", "at school", "park", "in a park");
    private static final Map<String, String> TOPICS = Map.of("road", "crossing safely with an adult", "medicine", "not touching medicine and telling an adult", "smoke", "staying away from smoke and alerting an adult", "help", "asking a trusted adult for help");
    @Value("${gemini.api.key:}") private String apiKey;
    @Value("${gemini.kids.story-model:gemini-3.5-flash-lite}") private String model;

    public AiStoryResult generate(String age, String character, String place, String topic, String lang) {
        if (!Set.of("3-5", "6-8", "9-12").contains(age)
                || !CHARACTERS.containsKey(character) || !PLACES.containsKey(place) || !TOPICS.containsKey(topic)
                || !("ar".equals(lang) || "en".equals(lang))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose the story ingredients from the available options");
        }
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Story builder is not configured");
        String prompt = "Write a gentle safety story for children aged " + age + ". Character: " + CHARACTERS.get(character)
                + ". Setting: " + PLACES.get(place) + ". Safety lesson: " + TOPICS.get(topic)
                + ". Content must be exactly two short sentences separated by a newline, maximum 240 characters total. "
                + "Give a short title and a one-sentence lesson. Respect Saudi culture. No injuries, frightening detail, violence, or dangerous tasks for children. "
                + "The character stays away from danger and asks a trusted adult for help. Never teach medical treatment, request personal data, or address a real emergency. "
                + (lang.equals("ar") ? "All values must be simple Arabic only, no foreign letters. " : "All values must be simple English only. ")
                + "Reply as JSON only: {\"title\":\"\",\"content\":\"first sentence\\nsecond sentence\",\"lesson\":\"\"}.";
        try (Client client = Client.builder().apiKey(apiKey).httpOptions(HttpOptions.builder().timeout(20000)
                .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            String json = client.models.generateContent(model, prompt, GenerateContentConfig.builder()
                    .responseMimeType("application/json").maxOutputTokens(2048)
                    .thinkingConfig(ThinkingConfig.builder().thinkingLevel("MINIMAL").build()).build()).text();
            AiStoryResult result = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(json, AiStoryResult.class);
            validateStory(result, lang);
            // A private, temporary result only. It is never inserted into the published story library.
            return result;
        } catch (com.google.genai.errors.ApiException e) {
            throw new ResponseStatusException(e.code() == 429 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY, "Story generation is unavailable");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not prepare a clear story, please try again");
        }
    }

    static void validateStory(AiStoryResult story, String lang) {
        if (story == null || !validText(story.getTitle(), 100, lang) || !validText(story.getContent(), 300, lang)
                || !validText(story.getLesson(), 160, lang) || story.getContent().lines().filter(s -> !s.isBlank()).count() > 2) {
            throw new IllegalArgumentException("Invalid generated story");
        }
    }

    private static boolean validText(String text, int max, String lang) {
        if (text == null || text.isBlank() || text.length() > max) return false;
        Character.UnicodeScript script = lang.equals("ar") ? Character.UnicodeScript.ARABIC : Character.UnicodeScript.LATIN;
        return text.codePoints().anyMatch(Character::isLetter)
                && text.codePoints().filter(Character::isLetter).allMatch(c -> Character.UnicodeScript.of(c) == script);
    }
}

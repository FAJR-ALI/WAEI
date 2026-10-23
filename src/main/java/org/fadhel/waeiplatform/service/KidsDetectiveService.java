package org.fadhel.waeiplatform.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.ThinkingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class KidsDetectiveService {
    private static final Logger log = LoggerFactory.getLogger(KidsDetectiveService.class);
    // The room and correct answers are fixed. AI only explains the selected item.
    private static final Map<String, String> ITEMS = Map.of(
            "cup", "A hot cup at the edge of a table. Stay away and tell an adult.",
            "medicine", "Loose medicine on the floor. Do not touch or taste it; tell an adult.",
            "wire", "A damaged electrical wire. Stay away; do not touch or repair it; tell an adult.",
            "book", "A closed book stored safely on a shelf. It is safe in this scene.",
            "teddy", "A teddy bear sitting safely on the sofa. It is safe in this scene.",
            "ball", "A ball safely stored in a basket away from the walkway. It is safe in this scene.");
    private static final Map<String, String> ARABIC_FEEDBACK = Map.of(
            "cup", "أحسنت! الكوب الساخن قريب من حافة الطاولة. ابتعد عنه وأخبر شخصًا كبيرًا.",
            "medicine", "أحسنت! حبوب الدواء ليست حلوى. لا تلمسها وأخبر شخصًا كبيرًا.",
            "wire", "اكتشفت خطرًا! لا تلمس السلك التالف، واطلب مساعدة شخص كبير.",
            "book", "الكتاب في مكان آمن على الرف. ابحث عن شيء آخر يحتاج انتباهك.",
            "teddy", "الدب في مكان آمن على الأريكة. ابحث عن شيء آخر يحتاج انتباهك.",
            "ball", "الكرة محفوظة في السلة بعيدًا عن الممر. ابحث عن شيء آخر يحتاج انتباهك.");

    @Value("${gemini.api.key:}")
    private String apiKey;
    @Value("${gemini.detective.model:gemini-3.5-flash-lite}")
    private String model;
    // Only 6 predefined items x 2 languages; no child information is stored.
    private final Map<String, Feedback> feedbackCache = new ConcurrentHashMap<>();

    public record Feedback(boolean danger, String message, boolean aiGenerated) {
        public Feedback(boolean danger, String message) {
            this(danger, message, true);
        }
    }

    public Feedback check(String item, String language) {
        if (!ITEMS.containsKey(item) || !("ar".equals(language) || "en".equals(language))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown room item or language");
        }
        String key = item + ":" + language;
        if (feedbackCache.containsKey(key)) return feedbackCache.get(key);
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Safety detective is not configured");
        }
        boolean danger = item.equals("cup") || item.equals("medicine") || item.equals("wire");
        String prompt = "You are Waei, a friendly safety learning character for children aged 6-8. "
                + "The child selected this object in a fixed cartoon room: " + ITEMS.get(item)
                + " The correct answer is: " + (danger ? "DANGEROUS" : "SAFE") + ". "
                + "Write at most two short encouraging sentences in " + (language.equals("ar") ? "simple Arabic" : "simple English")
                + ". Explain only this object and the safe behavior given above. "
                + "For a safe object, invite them to look for another object, without revealing other hazards. "
                + "Never ask them to handle hazards, give medical instructions, or ask for personal information. "
                + "Return plain text only, no markdown. "
                + (language.equals("ar")
                ? "اكتب الرد كله بالعربية الفصحى المبسطة فقط، حتى أسماء الأشياء. لا تستخدم أي كلمات أو حروف أجنبية. مثال: الكتاب في مكان آمن على الرف. ابحث عن خطر آخر."
                : "Use English words only; do not mix in other languages.");
        try (Client client = Client.builder().apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(10000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build()) {
            String message = client.models.generateContent(model, prompt,
                    GenerateContentConfig.builder().maxOutputTokens(1024)
                            .thinkingConfig(ThinkingConfig.builder().thinkingLevel("MINIMAL").build()).build()).text();
            if (message == null || message.isBlank() || message.length() > 700) {
                throw new IllegalStateException("Invalid feedback");
            }
            Feedback feedback = languageCheckedFeedback(item, language, message);
            // Do not cache mixed-language output or a basic replacement as an AI response.
            if (feedback.aiGenerated()) feedbackCache.put(key, feedback);
            return feedback;
        } catch (com.google.genai.errors.ApiException e) {
            log.warn("Safety detective provider failure: HTTP {}", e.code());
            if (e.code() == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Safety detective quota exceeded");
            }
            if (e.code() == 404) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Safety detective model is unavailable");
            }
            if (e.code() == 401 || e.code() == 403) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Safety detective access is denied");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Safety detective is unavailable, please try again");
        } catch (Exception e) {
            log.warn("Safety detective failed: {}", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Safety detective is unavailable, please try again");
        }
    }

    static Feedback languageCheckedFeedback(String item, String language, String message) {
        Character.UnicodeScript expected = language.equals("ar")
                ? Character.UnicodeScript.ARABIC : Character.UnicodeScript.LATIN;
        boolean correctLanguage = message.codePoints().anyMatch(Character::isLetter)
                && message.codePoints().filter(Character::isLetter)
                .allMatch(c -> Character.UnicodeScript.of(c) == expected);
        boolean danger = item.equals("cup") || item.equals("medicine") || item.equals("wire");
        return correctLanguage ? new Feedback(danger, message.trim(), true)
                : new Feedback(danger, language.equals("ar") ? ARABIC_FEEDBACK.get(item) : ITEMS.get(item), false);
    }
}

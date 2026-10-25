package org.fadhel.waeiplatform.config;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.model.KidsStory;
import org.fadhel.waeiplatform.repository.KidsStoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class KidsStoryDataSeeder implements CommandLineRunner {

    private final KidsStoryRepository kidsStoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> existing = new HashSet<>();
        for (KidsStory story : kidsStoryRepository.findAll()) {
            existing.add(key(story.getAgeGroup(), story.getTitle()));
        }

        add(existing, "3-5", "ليان والكوب الساخن", "السلامة في المنزل",
                "رأت ليان كوبًا ساخنًا، فابتعدت عنه.\nنادت أمها لتضعه في مكان آمن.",
                "ابتعد عن الأشياء الساخنة واطلب مساعدة شخص كبير.");
        add(existing, "3-5", "سعد والحبوب الملونة", "السلامة مع الأدوية",
                "وجد سعد حبوبًا ملونة، فلم يلمسها.\nأخبر أباه، فحفظها بعيدًا عن الأطفال.",
                "الدواء ليس حلوى، لا تلمسه وحدك.");
        add(existing, "3-5", "نورة عند المسبح", "السلامة قرب الماء",
                "وصلت كرة نورة إلى المسبح، فتوقفت بعيدًا عنه.\nطلبت من أمها إحضار الكرة.",
                "لا تقترب من المسبح دون شخص كبير.");

        add(existing, "6-8", "عمر وممر المشاة", "السلامة في الطريق",
                "أراد عمر عبور الشارع، فانتظر عند ممر المشاة.\nأمسك يد أبيه وعبرا بعد التأكد من توقف السيارات.",
                "اعبر مع شخص كبير من المكان المخصص.");
        add(existing, "6-8", "ريم ورائحة الدخان", "السلامة من الحريق",
                "لاحظت ريم دخانًا، فابتعدت ونادت أمها.\nخرجتا إلى مكان آمن ولم تعودا لأخذ الألعاب.",
                "ابتعد عن الدخان، ونبّه شخصًا كبيرًا.");
        add(existing, "6-8", "فهد وصديقه", "طلب المساعدة",
                "سقط صديق فهد في الملعب ولم يستطع الوقوف.\nنادى فهد المعلم ولم يحاول تحريك صديقه.",
                "اطلب مساعدة المعلم عند إصابة صديقك.");

        add(existing, "9-12", "جود والسلك المكشوف", "السلامة الكهربائية",
                "رأت جود سلكًا مكشوفًا، فابتعدت ونبّهت إخوتها.\nأخبرت أباها دون لمس السلك أو محاولة إصلاحه.",
                "ابتعد عن الأسلاك التالفة واترك إصلاحها للكبار.");
        add(existing, "9-12", "راشد والمكالمة الواضحة", "التواصل وقت الطوارئ",
                "تدرّب راشد مع أمه على وصف الموقف والعنوان بهدوء.\nتعلّم أن مكالمة الطوارئ للمساعدة الحقيقية وليست للعب.",
                "اعرف عنوانك واستمع لتعليمات موظف الطوارئ.");
        add(existing, "9-12", "سارة والطريق المغمور", "السلامة وقت المطر",
                "رأت سارة مياهًا تغمر الطريق، فتوقفت في مكان آمن.\nأخبرت أهلها واختارت معهم طريقًا بعيدًا عن المياه.",
                "لا تعبر المياه المتجمعة ولا تلعب قرب مجاري السيول.");
    }

    private void add(Set<String> existing, String ageGroup, String title, String topic,
                     String content, String lesson) {
        if (!existing.add(key(ageGroup, title))) {
            return;
        }
        KidsStory story = new KidsStory();
        story.setAgeGroup(ageGroup);
        story.setTitle(title);
        story.setTopic(topic);
        story.setContent(content);
        story.setLesson(lesson);
        // Publish predefined library stories; child-created stories are temporary and stored separately from this library.
        story.setStatus("APPROVED");
        story.setPublishedAt(LocalDateTime.now());
        kidsStoryRepository.save(story);
    }

    private String key(String ageGroup, String title) {
        return ageGroup + ":" + title;
    }
}

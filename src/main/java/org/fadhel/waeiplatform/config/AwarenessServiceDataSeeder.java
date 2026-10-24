package org.fadhel.waeiplatform.config;

import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.model.AwarenessService;
import org.fadhel.waeiplatform.repository.AwarenessServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Fills the trainings (awareness services) with 5 basic trainings the first time the project runs
// (only when the table is empty), so the trainings page is never empty in the demo.
// The admin can edit, add or delete them later from the awareness-service endpoints.
// The description holds the correct steps: the AI uses them as the answer key when it evaluates the video.
@Component
@RequiredArgsConstructor
public class AwarenessServiceDataSeeder implements CommandLineRunner {

    private final AwarenessServiceRepository awarenessServiceRepository;

    @Override
    public void run(String... args) {
        if (awarenessServiceRepository.count() > 0) {
            return;
        }

        add("الإنعاش القلبي الرئوي للبالغ", "إسعافات أولية",
                "الخطوات الصحيحة:\n"
                        + "1. تأكد إن المكان آمن لك وللمصاب.\n"
                        + "2. اهزه من كتفه وكلّمه بصوت عالي، إذا ما استجاب وما يتنفس طبيعي اتصل على 997.\n"
                        + "3. اطلب جهاز الصدمات الكهربائية (AED) إذا متوفر.\n"
                        + "4. حط كعب يدك في منتصف الصدر واليد الثانية فوقها، وخل يديك مستقيمة وكتوفك فوق الصدر.\n"
                        + "5. اضغط بقوة وبسرعة: عمق 5 إلى 6 سم، و100 إلى 120 ضغطة بالدقيقة.\n"
                        + "6. خل الصدر يرتفع بالكامل بعد كل ضغطة.\n"
                        + "7. كمّل الضغطات بدون توقف لين يوصل الإسعاف أو يبدأ يتنفس طبيعي.");

        add("الاختناق عند البالغ (مناورة هيمليك)", "إسعافات أولية",
                "الخطوات الصحيحة:\n"
                        + "1. اسأله: \"أنت مختنق؟\" إذا يقدر يكح بقوة شجّعه يكمل الكحة.\n"
                        + "2. إذا ما يقدر يتكلم ولا يكح، اطلب من أحد يتصل على 997.\n"
                        + "3. قف خلفه، ميّله لقدام، واضرب بين كتوفه 5 ضربات بكعب يدك.\n"
                        + "4. إذا ما طلع الجسم: حط قبضتك فوق السرة وتحت عظمة الصدر، ومسكها بيدك الثانية.\n"
                        + "5. اضغط لداخل وفوق بقوة 5 مرات.\n"
                        + "6. كرر 5 ضربات و5 ضغطات لين يطلع الجسم أو يوصل الإسعاف.\n"
                        + "7. إذا فقد وعيه نزّله على الأرض بهدوء وابدأ الإنعاش القلبي.");

        add("وضعية الإفاقة الجانبية", "إسعافات أولية",
                "الخطوات الصحيحة (لشخص فاقد الوعي ويتنفس طبيعي):\n"
                        + "1. تأكد إنه يتنفس طبيعي واتصل على 997.\n"
                        + "2. اركع جنبه، ومد يده القريبة منك بزاوية قائمة مع جسمه.\n"
                        + "3. حط ظهر يده البعيدة على خده القريب منك وثبّتها.\n"
                        + "4. ارفع ركبته البعيدة لين تصير رجله مثنية والقدم على الأرض.\n"
                        + "5. اسحب الركبة البعيدة باتجاهك لين ينقلب على جنبه.\n"
                        + "6. ميّل رأسه شوي لورا عشان مجرى الهواء يبقى مفتوح.\n"
                        + "7. ابقَ معه وراقب تنفسه لين يوصل الإسعاف.");

        add("إيقاف النزيف الشديد", "إسعافات أولية",
                "الخطوات الصحيحة:\n"
                        + "1. اتصل على 997 والبس قفازات إذا متوفرة.\n"
                        + "2. اضغط على الجرح مباشرة بقطعة قماش نظيفة أو شاش.\n"
                        + "3. اضغط بقوة وبشكل مستمر بيدك الثنتين.\n"
                        + "4. إذا تشربت القطعة بالدم، حط فوقها قطعة ثانية وكمّل الضغط بدون ما تشيل الأولى.\n"
                        + "5. لف ضمادة بإحكام فوق الشاش إذا متوفرة.\n"
                        + "6. كمّل الضغط بدون توقف لين يوصل الإسعاف.\n"
                        + "7. لا تطلّع أي جسم مغروس في الجرح.");

        add("استخدام طفاية الحريق", "سلامة",
                "الخطوات الصحيحة (طريقة PASS):\n"
                        + "1. نبّه الموجودين واتصل على 998، وتأكد إن عندك طريق خروج وراك.\n"
                        + "2. قف على بعد 2 إلى 3 متر من النار وظهرك لطريق الخروج.\n"
                        + "3. اسحب مسمار الأمان من الطفاية.\n"
                        + "4. وجّه فوهة الطفاية لقاعدة النار مو للهب.\n"
                        + "5. اضغط على المقبض بثبات.\n"
                        + "6. حرّك الفوهة يمين ويسار على قاعدة النار لين تنطفي.\n"
                        + "7. إذا كبرت النار أو خلصت الطفاية اطلع فورًا وسكّر الباب وراك.");
    }

    private void add(String title, String category, String description) {
        AwarenessService service = new AwarenessService();
        service.setTitle(title);
        service.setCategory(category);
        service.setDescription(description);
        service.setIsActive(true);
        awarenessServiceRepository.save(service);
    }
}

const objects = document.querySelectorAll(".room-object");
const feedback = document.getElementById("detectiveFeedback");
const found = new Set();
let checking = false;

objects.forEach(function (button) {
    button.addEventListener("click", async function () {
        if (checking) return;
        if (found.has(button.dataset.item)) {
            feedback.textContent = tr("اكتشفت هذا الخطر! دوّر على شيء ثاني", "You already found this hazard! Look for another object");
            return;
        }
        checking = true;
        objects.forEach(b => b.disabled = true);
        document.getElementById("detectiveRestart").disabled = true;
        feedback.classList.remove("good");
        feedback.setAttribute("aria-busy", "true");
        feedback.textContent = tr("واعي يفكّر باختيارك…", "Waei is thinking about your choice…");
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 12000);
        try {
            const response = await fetch("/api/v1/kids/detective/check/" + button.dataset.item + "?lang=" + LANG,
                { signal: controller.signal });
            if (!response.ok) {
                const error = new Error("Request failed");
                error.status = response.status;
                throw error;
            }
            const result = await response.json();
            if (typeof result.danger !== "boolean" || typeof result.message !== "string") throw new Error("Invalid result");
            // AI text is displayed as text, never as HTML.
            feedback.textContent = (result.aiGenerated === false ? tr("شرح أساسي: ", "Basic explanation: ") : "") + result.message;
            if (result.danger) {
                found.add(button.dataset.item);
                button.classList.add("found");
                feedback.classList.add("good");
                document.getElementById("detectiveScore").textContent = found.size + " / 3 ⭐";
                document.getElementById("detectiveFinished").classList.toggle("hidden", found.size !== 3);
            }
        } catch (e) {
            if (e.status === 429) {
                feedback.textContent = tr("وصلنا لحد استخدام الذكاء الاصطناعي، جرّب لاحقًا", "The AI usage limit was reached. Please try later");
            } else if (e.status === 503) {
                feedback.textContent = tr("محقق السلامة غير متاح حاليًا، يلزم التحقق من إعدادات الخدمة", "Safety detective is unavailable. The service settings need checking");
            } else if (e.name === "AbortError") {
                feedback.textContent = tr("الرد تأخر، جرّب مرة ثانية", "The reply took too long. Please try again");
            } else {
                feedback.textContent = tr("واعي ما قدر يرد الآن، جرّب مرة ثانية", "Waei couldn't reply just now. Please try again");
            }
        } finally {
            clearTimeout(timeout);
            checking = false;
            objects.forEach(b => b.disabled = false);
            document.getElementById("detectiveRestart").disabled = false;
            feedback.removeAttribute("aria-busy");
            button.focus();
        }
    });
});

document.getElementById("detectiveRestart").addEventListener("click", function () {
    found.clear();
    objects.forEach(b => b.classList.remove("found"));
    document.getElementById("detectiveScore").textContent = "0 / 3 ⭐";
    document.getElementById("detectiveFinished").classList.add("hidden");
    feedback.classList.remove("good");
    feedback.textContent = tr("اختر شيئًا وخلّنا نحقق!", "Choose an object and let's investigate!");
});

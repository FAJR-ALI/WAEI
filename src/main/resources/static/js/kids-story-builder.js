const builderForm = document.getElementById("builderForm");
const builderStatus = document.getElementById("builderStatus");
const builderResult = document.getElementById("builderResult");
let builderRequest = null;

document.getElementById("builderListenChoices").addEventListener("click", function () {
    let summary = document.getElementById("builderChoiceSummary");
    if (!summary) {
        summary = document.createElement("p"); summary.id = "builderChoiceSummary";
        builderForm.insertBefore(summary, document.getElementById("builderListenChoices"));
    }
    summary.textContent = Array.from(builderForm.querySelectorAll("select")).map(select => select.selectedOptions[0].textContent).join(". ");
    // The shared reader sees exactly the selected options, not every option in the menu.
    this.setAttribute("data-kids-speak", "#builderChoiceSummary");
});

builderForm.addEventListener("submit", async function (event) {
    event.preventDefault();
    if (builderRequest) return;
    window.dispatchEvent(new Event("kids-story-audio-start"));
    const request = new AbortController(); builderRequest = request;
    const button = document.getElementById("builderSubmit");
    builderForm.querySelectorAll("select,button").forEach(el => el.disabled = true);
    builderResult.classList.add("hidden");
    builderStatus.textContent = tr("واعي يكتب قصتك…", "Waei is writing your story…");
    builderForm.setAttribute("aria-busy", "true");
    const timeout = setTimeout(() => request.abort(), 25000);
    try {
        const response = await fetch("/api/v1/kids/stories/create", {
            method: "POST", headers: { "Content-Type": "application/json" }, signal: request.signal,
            body: JSON.stringify({ ageGroup: document.getElementById("builderAge").value,
                character: document.getElementById("builderCharacter").value, place: document.getElementById("builderPlace").value,
                topic: document.getElementById("builderTopic").value, lang: LANG })
        });
        if (!response.ok) { const error = new Error("Generation failed"); error.status = response.status; throw error; }
        const story = await response.json();
        if (![story.title, story.content, story.lesson].every(value => typeof value === "string" && value.trim())) throw new Error("Invalid story");
        document.getElementById("builderTitle").textContent = story.title;
        document.getElementById("builderContent").textContent = story.content;
        document.getElementById("builderLesson").textContent = tr("تذكّر: ", "Remember: ") + story.lesson;
        builderResult.classList.remove("hidden");
        builderStatus.textContent = tr("قصتك جاهزة! تقدر تقرأها أو تسمعها.", "Your story is ready! Read it or listen.");
    } catch (error) {
        builderStatus.textContent = error.status === 429
            ? tr("وصلنا لحد الاستخدام. جرّب لاحقًا.", "The usage limit was reached. Please try later.")
            : tr("ما قدرنا نجهّز قصة واضحة الآن. اختياراتك موجودة، جرّب مرة ثانية.", "We couldn't prepare a clear story. Your choices are still here; please try again.");
    } finally {
        clearTimeout(timeout); builderRequest = null;
        builderForm.querySelectorAll("select,button").forEach(el => el.disabled = false);
        builderForm.removeAttribute("aria-busy");
    }
});
window.addEventListener("pagehide", function () { if (builderRequest) builderRequest.abort(); });

// Shared AI reader. Read current visible text, including feedback that changes during play.
(function () {
    const cache = new Map();
    let audio = null;
    let activeButton = null;
    let activeKey = null;
    let request = null;
    let version = 0;
    const status = document.createElement("p");
    status.className = "k-voice-status hidden";
    status.setAttribute("role", "status");
    status.setAttribute("aria-live", "polite");
    document.body.appendChild(status);

    function announce(ar, en) {
        status.textContent = tr(ar, en);
        status.classList.toggle("hidden", !status.textContent);
    }
    function resetButton() {
        if (activeButton) {
            activeButton.removeAttribute("aria-busy");
            activeButton.setAttribute("aria-pressed", "false");
        }
    }
    function stop() {
        version++;
        if (request) request.abort();
        request = null;
        if (audio) audio.pause();
        resetButton();
        audio = null;
        activeButton = null;
        activeKey = null;
    }

    document.addEventListener("click", async function (event) {
        const button = event.target.closest("[data-kids-speak]");
        if (!button || button.disabled) return;
        const nodes = Array.from(document.querySelectorAll(button.dataset.kidsSpeak));
        if (nodes.some(node => node.getAttribute("aria-busy") === "true")) {
            announce("انتظر حتى تجهز النتيجة ثم اضغط الاستماع.", "Wait for the result, then press listen.");
            return;
        }
        const text = nodes.filter(node => !node.closest(".hidden"))
            .map(node => (node.innerText || node.textContent || "").trim()).filter(Boolean).join(". ");
        if (!text || text.length > 2000) {
            announce("اختر جزءًا أقصر فيه كلام للاستماع.", "Choose a shorter section with text to hear.");
            return;
        }
        const lang = button.dataset.voiceLang || document.documentElement.lang || "ar";
        const ageGroup = document.getElementById("builderAge")?.value || "6-8";
        const key = lang + ":" + ageGroup + ":" + text;
        if (activeButton === button && activeKey === key && audio) {
            if (!audio.paused) {
                audio.pause();
                button.setAttribute("aria-pressed", "false");
                announce("توقّف الصوت مؤقتًا. اضغط للاستكمال.", "Paused. Press again to resume.");
            } else {
                if (audio.ended) audio.currentTime = 0;
                try { await audio.play(); button.setAttribute("aria-pressed", "true"); announce("", ""); }
                catch (e) { announce("اضغط الاستماع مرة ثانية لتشغيل الصوت.", "Press listen again to play."); }
            }
            return;
        }
        if (activeButton === button && request) {
            stop(); announce("تم إلغاء تجهيز الصوت.", "Audio preparation cancelled."); return;
        }
        stop();
        window.dispatchEvent(new Event("kids-voice-start"));
        const ticket = version;
        activeButton = button;
        activeKey = key;
        button.setAttribute("aria-busy", "true");
        announce("واعي يجهّز الصوت… اضغط نفس الزر للإلغاء.", "Waei is preparing audio… Press the same button to cancel.");
        let timeout;
        try {
            let url = cache.get(key);
            if (!url) {
                const controller = new AbortController();
                request = controller;
                timeout = setTimeout(() => controller.abort(), 50000);
                const response = await fetch("/api/v1/kids/voice", {
                    method: "POST", headers: { "Content-Type": "application/json", Accept: "audio/wav, application/json" },
                    body: JSON.stringify({ text, lang, ageGroup }), signal: controller.signal
                });
                if (!response.ok) {
                    const error = new Error("Voice failed"); error.status = response.status;
                    const details = await response.json().catch(() => ({}));
                    error.reason = details.message;
                    throw error;
                }
                if (!(response.headers.get("Content-Type") || "").startsWith("audio/")) throw new Error("Invalid audio");
                const blob = await response.blob();
                if (ticket !== version) return;
                if (!blob.size) throw new Error("Empty audio");
                url = URL.createObjectURL(blob);
                if (cache.size >= 6) { const oldest = cache.keys().next().value; URL.revokeObjectURL(cache.get(oldest)); cache.delete(oldest); }
                cache.set(key, url);
            }
            if (ticket !== version) return;
            audio = new Audio(url);
            audio.addEventListener("ended", function () { resetButton(); announce("انتهت القراءة. تقدر تسمع مرة ثانية.", "Finished. You can listen again."); });
            audio.addEventListener("error", function () {
                cache.delete(key); URL.revokeObjectURL(url); stop();
                announce("تعذّر تشغيل الصوت. اضغط الاستماع للمحاولة مجددًا.", "Playback failed. Press listen to retry.");
            });
            try { await audio.play(); button.setAttribute("aria-pressed", "true"); announce("", ""); }
            catch (e) { announce("الصوت جاهز. اضغط نفس الزر للاستماع.", "Audio is ready. Press the same button to listen."); }
        } catch (error) {
            if (ticket !== version) return;
            if (error.status === 429) announce("وصلنا لحد استخدام الصوت. جرّب لاحقًا.", "The audio usage limit was reached. Please try later.");
            else if (error.reason === "Speech provider denied access") announce("خدمة الصوت رفضت الوصول. يلزم التحقق من صلاحيات المشروع.", "Audio access was denied. Check the project permissions.");
            else if (error.reason === "Speech model is unavailable") announce("نموذج الصوت غير متاح للمشروع حاليًا.", "The speech model is unavailable for this project.");
            else if (error.reason === "Speech provider rejected the narration request") announce("خدمة الصوت رفضت صيغة طلب القراءة. يلزم مراجعة الطلب.", "The speech provider rejected the narration request format.");
            else if (error.status === 503) announce("راوي الصوت غير مفعّل حاليًا.", "The voice reader is not configured.");
            else announce("تعذّر تجهيز الصوت الآن. جرّب مرة ثانية.", "Audio could not be prepared. Please try again.");
            resetButton(); activeButton = null; activeKey = null;
        } finally {
            clearTimeout(timeout);
            if (ticket === version) { request = null; button.removeAttribute("aria-busy"); }
        }
    });
    window.addEventListener("kids-story-audio-start", stop);
    window.addEventListener("pagehide", function () { stop(); cache.forEach(url => URL.revokeObjectURL(url)); cache.clear(); });
})();

// Only a published story ID is sent; no child text, microphone, or credentials.
function enableStoryNarration(storyId) {
    const panel = document.getElementById("narratorPanel");
    const button = document.getElementById("narrateStory");
    const shortcut = document.getElementById("speakStory");
    const replay = document.getElementById("replayStory");
    const status = document.getElementById("narratorStatus");
    const audio = document.getElementById("storyAudio");
    window.addEventListener("kids-voice-start", function () { audio.pause(); });
    let objectUrl = null;
    let request = null;
    panel.classList.remove("hidden");
    shortcut.disabled = false;

    function label(ar, en) {
        button.textContent = tr(ar, en);
        button.dataset.en = en;
        shortcut.setAttribute("aria-label", tr(ar, en));
        shortcut.dataset.enAria = en;
    }

    async function play() {
        if (request) return;
        if (!audio.paused) {
            audio.pause();
            return;
        }
        if (!objectUrl) {
            request = new AbortController();
            const timeout = setTimeout(() => request && request.abort(), 90000);
            button.disabled = shortcut.disabled = true;
            panel.setAttribute("aria-busy", "true");
            status.textContent = tr("واعي يجهّز صوت القصة…", "Waei is preparing the narration…");
            try {
                const response = await fetch("/api/v1/kids/stories/" + encodeURIComponent(storyId) + "/narration", {
                    signal: request.signal,
                    headers: { Accept: "audio/wav, application/json" }
                });
                if (!response.ok || !(response.headers.get("Content-Type") || "").startsWith("audio/")) {
                    throw new Error("Narration unavailable");
                }
                const blob = await response.blob();
                if (!blob.size) throw new Error("Empty audio");
                objectUrl = URL.createObjectURL(blob);
                audio.src = objectUrl;
                audio.classList.remove("hidden");
                replay.classList.remove("hidden");
            } catch (error) {
                status.textContent = tr("ما قدرنا نجهّز الصوت الآن. جرّب مرة ثانية.", "We couldn't prepare the audio. Please try again.");
                return;
            } finally {
                clearTimeout(timeout);
                request = null;
                button.disabled = shortcut.disabled = false;
                panel.removeAttribute("aria-busy");
            }
        }
        try {
            if (audio.ended) audio.currentTime = 0;
            window.dispatchEvent(new Event("kids-story-audio-start"));
            await audio.play();
        } catch (error) {
            status.textContent = tr("الصوت جاهز، اضغط تشغيل للاستماع.", "Audio is ready. Press play to listen.");
        }
    }

    button.addEventListener("click", play);
    shortcut.addEventListener("click", play);
    replay.addEventListener("click", function () {
        audio.pause();
        audio.currentTime = 0;
        play();
    });
    audio.addEventListener("play", function () {
        label("إيقاف مؤقت", "Pause narration");
        status.textContent = "";
    });
    audio.addEventListener("pause", function () {
        label("كمّل القصة", "Resume narration");
    });
    audio.addEventListener("ended", function () {
        label("اسمع مرة ثانية", "Listen again");
    });
    audio.addEventListener("error", function () {
        status.textContent = tr("تعذّر تشغيل الصوت. أعد تحميل الصفحة للمحاولة مجددًا.", "Audio playback failed. Reload the page to retry.");
    });
    window.addEventListener("pagehide", function () {
        audio.pause();
        if (request) request.abort();
        if (objectUrl) URL.revokeObjectURL(objectUrl);
    }, { once: true });
}

const canvas = document.getElementById("drawingCanvas");
const ctx = canvas.getContext("2d");
const drawingFeedback = document.getElementById("drawingFeedback");
const reviewButton = document.getElementById("drawingReview");
const clearButton = document.getElementById("drawingClear");
const eraserButton = document.getElementById("drawingEraser");
const topicSelect = document.getElementById("drawingTopic");
const colors = document.querySelectorAll(".drawing-color");
let color = "#2E1065";
let erasing = false;
let activePointer = null;
let hasDrawing = false;
let busy = false;
let keyboardPoint = { x: 320, y: 200 };
let drawingRequest = null;

function clearDrawing() {
    ctx.fillStyle = "#FFFFFF";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    hasDrawing = false;
    drawingFeedback.textContent = tr("ارسم تصرفًا آمنًا وورّه لواعي", "Draw a safe action and show Waei");
}
clearDrawing();

function point(event) {
    const box = canvas.getBoundingClientRect();
    return { x: (event.clientX - box.left) * canvas.width / box.width,
        y: (event.clientY - box.top) * canvas.height / box.height };
}
function startLine(p) {
    ctx.beginPath();
    ctx.moveTo(p.x, p.y);
    ctx.strokeStyle = erasing ? "#FFFFFF" : color;
    ctx.lineWidth = erasing ? 22 : 6;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
}
function drawTo(p) {
    ctx.lineTo(p.x, p.y);
    ctx.stroke();
    if (!erasing) hasDrawing = true;
}
canvas.addEventListener("pointerdown", function (event) {
    if (busy || activePointer !== null || event.button !== 0) return;
    activePointer = event.pointerId;
    canvas.setPointerCapture(event.pointerId);
    const p = point(event);
    startLine(p);
    drawTo({ x: p.x + 0.1, y: p.y });
});
canvas.addEventListener("pointermove", function (event) {
    if (busy || event.pointerId !== activePointer) return;
    drawTo(point(event));
});
["pointerup", "pointercancel", "lostpointercapture"].forEach(function (name) {
    canvas.addEventListener(name, function (event) {
        if (activePointer === event.pointerId) activePointer = null;
    });
});
canvas.addEventListener("keydown", function (event) {
    const directions = { ArrowUp: [0, -10], ArrowDown: [0, 10], ArrowLeft: [-10, 0], ArrowRight: [10, 0] };
    const direction = directions[event.key];
    if (!direction || busy) return;
    event.preventDefault();
    const next = { x: Math.max(0, Math.min(640, keyboardPoint.x + direction[0])),
        y: Math.max(0, Math.min(400, keyboardPoint.y + direction[1])) };
    if (event.shiftKey) { startLine(keyboardPoint); drawTo(next); }
    keyboardPoint = next;
});
colors.forEach(function (button) {
    button.addEventListener("click", function () {
        color = button.dataset.color;
        erasing = false;
        eraserButton.setAttribute("aria-pressed", "false");
        colors.forEach(b => b.setAttribute("aria-pressed", String(b === button)));
    });
});
eraserButton.addEventListener("click", function () {
    erasing = !erasing;
    eraserButton.setAttribute("aria-pressed", String(erasing));
    colors.forEach(b => b.setAttribute("aria-pressed", String(!erasing && b.dataset.color === color)));
});
clearButton.addEventListener("click", clearDrawing);
topicSelect.addEventListener("change", function () {
    drawingFeedback.textContent = tr("الموضوع تغيّر، تقدر تعدّل رسمتك ثم ترسلها", "The topic changed. You can edit your drawing and submit it");
});

reviewButton.addEventListener("click", async function () {
    if (busy) return;
    if (!hasDrawing) {
        drawingFeedback.textContent = tr("ارسم شيئًا أولًا يا بطل!", "Draw something first, hero!");
        return;
    }
    if (!document.getElementById("drawingConsent").checked) {
        drawingFeedback.textContent = tr("اطلب إذن ولي أمرك وفعّل الموافقة أولًا", "Ask your parent's permission and check the consent box first");
        return;
    }
    busy = true;
    activePointer = null;
    [reviewButton, clearButton, eraserButton, topicSelect, ...colors].forEach(b => b.disabled = true);
    drawingFeedback.setAttribute("aria-busy", "true");
    drawingFeedback.textContent = tr("واعي يشوف رسمتك…", "Waei is looking at your drawing…");
    drawingRequest = new AbortController();
    const timeout = setTimeout(() => drawingRequest && drawingRequest.abort(), 18000);
    try {
        const blob = await new Promise(resolve => canvas.toBlob(resolve, "image/png"));
        if (!blob) throw new Error("Canvas unavailable");
        const body = new FormData();
        body.append("drawing", blob, "drawing.png");
        body.append("topic", topicSelect.value);
        body.append("lang", LANG);
        body.append("consent", "true");
        const response = await fetch("/api/v1/kids/drawing/review", { method: "POST", body, signal: drawingRequest.signal });
        if (!response.ok) { const error = new Error("Request failed"); error.status = response.status; throw error; }
        const result = await response.json();
        if (typeof result.message !== "string") throw new Error("Invalid response");
        drawingFeedback.textContent = result.message;
    } catch (error) {
        if (error.status === 429) drawingFeedback.textContent = tr("وصلنا لحد استخدام الذكاء الاصطناعي، جرّب لاحقًا", "The AI usage limit was reached. Please try later");
        else if (error.status === 400) drawingFeedback.textContent = tr("أضف تفاصيل لرسمتك وحاول مرة ثانية", "Add details to your drawing and try again");
        else if (error.status === 503) drawingFeedback.textContent = tr("واعي غير متاح حاليًا. يلزم التحقق من إعدادات الخدمة", "Waei is unavailable. The service settings need checking");
        else drawingFeedback.textContent = tr("ما قدرنا نراجع الرسمة الآن. رسمتك موجودة، جرّب مرة ثانية", "We couldn't review the drawing. It is still here; please try again");
    } finally {
        clearTimeout(timeout);
        drawingRequest = null;
        busy = false;
        [reviewButton, clearButton, eraserButton, topicSelect, ...colors].forEach(b => b.disabled = false);
        drawingFeedback.removeAttribute("aria-busy");
    }
});
window.addEventListener("pagehide", function () { if (drawingRequest) drawingRequest.abort(); });

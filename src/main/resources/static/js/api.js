// ===== Waei — shared helpers for talking to the Spring Boot backend =====

// Needs js/i18n.js loaded before this file (for tr() and LANG)

// The pages are served by Spring Boot itself (src/main/resources/static),
// so the API is on the same server: no need to write http://localhost:8080
const API_BASE = "/api/v1";

// Backend messages are in English: show the common ones in Arabic
const ARABIC_MESSAGES = {
    "Awareness service not found": "التدريب غير موجود",
    "This training is not active right now": "هذا التدريب غير متاح حاليًا",
    "Video evaluation is only available for users 18 years or older": "تقييم الفيديو متاح لمن عمره 18 سنة أو أكثر",
    "You must agree to let the AI analyze your video": "لازم توافق على تحليل الفيديو بالذكاء الاصطناعي",
    "Please upload a video": "اختر فيديو أول",
    "Video is too large, maximum size is 20 MB": "الفيديو كبير، الحد الأقصى 20 ميجابايت",
    "The uploaded file must be a video": "الملف لازم يكون فيديو",
    "AI could not evaluate the video, please try again": "ما قدر الذكاء الاصطناعي يحلل الفيديو، جرّب مرة ثانية",
    "Session not found": "المحاولة غير موجودة",
    "This session has no quiz": "هذي المحاولة ما لها اختبار",
    "You already answered this quiz": "جاوبت على هذا الاختبار من قبل",
    "Story not found": "القصة غير موجودة",
    "Invalid email or password": "البريد أو كلمة المرور غير صحيحة",
    "This account is not active yet. Please contact the platform administrator.": "الحساب غير مفعّل بعد، تواصل مع إدارة المنصة",
    "Authentication required": "لازم تسجّل دخول",
    "Emergency case not found": "الحالة غير موجودة",
    "The emergency guide is empty right now": "دليل الطوارئ فاضي حاليًا",
    "Smart search is not available right now, please browse the cases": "البحث الذكي غير متاح الحين، تصفح الحالات تحت",
    "Your organization registration is awaiting platform administrator verification.": "طلب تسجيل جهتك قيد المراجعة من إدارة المنصة",
    "Your organization is not verified yet. Please contact the platform administrator.": "جهتك ما توثقت بعد، تواصل مع إدارة المنصة",
    "Access denied": "ما عندك صلاحية"
};

// In English mode the backend message is shown as it is
function toArabic(message) {
    if (!message) {
        return tr("صار خطأ غير متوقع، جرّب مرة ثانية", "Something went wrong, please try again");
    }
    if (LANG === "en") {
        return message;
    }
    return ARABIC_MESSAGES[message] || message;
}

// Reads the response and throws an Error with the backend message when status is not 2xx
async function handleResponse(response) {
    let data = null;
    const text = await response.text();
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = { message: text };
        }
    }
    if (!response.ok) {
        const message = data && data.message ? data.message : null;
        const error = new Error(toArabic(message));
        error.status = response.status;
        throw error;
    }
    return data;
}

function authHeaders() {
    const token = getToken();
    if (token) {
        return { "Authorization": "Bearer " + token };
    }
    return {};
}

async function apiGet(path) {
    try {
        const response = await fetch(API_BASE + path, { headers: authHeaders() });
        return await handleResponse(response);
    } catch (e) {
        throw networkError(e);
    }
}

async function apiPost(path, body) {
    try {
        const response = await fetch(API_BASE + path, {
            method: "POST",
            headers: Object.assign({ "Content-Type": "application/json" }, authHeaders()),
            body: JSON.stringify(body)
        });
        return await handleResponse(response);
    } catch (e) {
        throw networkError(e);
    }
}

// For uploading files (multipart/form-data): the browser sets the Content-Type itself
async function apiPostForm(path, formData) {
    try {
        const response = await fetch(API_BASE + path, {
            method: "POST",
            headers: authHeaders(),
            body: formData
        });
        return await handleResponse(response);
    } catch (e) {
        throw networkError(e);
    }
}

// fetch() itself fails when the server is off
function networkError(e) {
    if (e instanceof TypeError) {
        return new Error(tr("ما قدرنا نوصل للسيرفر، تأكد إنه شغال", "Could not reach the server, make sure it is running"));
    }
    return e;
}

// ===== Login token (saved in this browser only) =====

function saveLogin(authResponse) {
    try {
        localStorage.setItem("waeiToken", authResponse.token);
        localStorage.setItem("waeiUser", JSON.stringify({
            fullName: authResponse.fullName,
            email: authResponse.email,
            role: authResponse.role,
            organizationName: authResponse.organizationName
        }));
    } catch (e) {
        // Storage can be blocked (private window): the login still works for this page
    }
}

function getToken() {
    try {
        return localStorage.getItem("waeiToken");
    } catch (e) {
        return null;
    }
}

function getUser() {
    try {
        return JSON.parse(localStorage.getItem("waeiUser"));
    } catch (e) {
        return null;
    }
}

function logout() {
    try {
        localStorage.removeItem("waeiToken");
        localStorage.removeItem("waeiUser");
    } catch (e) {
        // ignore
    }
    window.location.href = "/";
}

// ===== Small helpers =====

// Always use this before putting text from the API inside innerHTML
function escapeHtml(text) {
    if (text === null || text === undefined) {
        return "";
    }
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function getQueryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

function showLoading(element, text) {
    element.innerHTML = '<div class="loading-state"><div class="spinner"></div>' + escapeHtml(text || tr("جاري التحميل...", "Loading...")) + "</div>";
}

function showError(element, message) {
    element.innerHTML = '<div class="error-state" role="alert">' + escapeHtml(message) + "</div>";
}

function showEmpty(element, message) {
    element.innerHTML = '<div class="empty-state">' + escapeHtml(message) + "</div>";
}

// Header: if the user is logged in, "دخول الجهات" becomes their name
document.addEventListener("DOMContentLoaded", function () {
    const loginLink = document.getElementById("headerLogin");
    const user = getUser();
    if (loginLink && user && getToken()) {
        loginLink.textContent = user.fullName || tr("حسابي", "My account");
        loginLink.href = "/login";
    }
});

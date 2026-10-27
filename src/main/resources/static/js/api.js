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
    "This account is not active yet. Please contact the platform administrator": "الحساب غير مفعّل بعد، تواصل مع إدارة المنصة",
    "Your organization registration is awaiting platform administrator verification": "طلب تسجيل جهتك بانتظار مراجعة إدارة المنصة",
    "Your organization is not verified yet. Please contact the platform administrator": "جهتك غير موثقة بعد، تواصل مع إدارة المنصة",
    "The organization must be verified before it can perform this action": "لازم تكون الجهة موثقة قبل تنفيذ هذا الإجراء",
    "Authentication required": "لازم تسجّل دخول",
    "Emergency case not found": "الحالة غير موجودة",
    "The emergency guide is empty right now": "دليل الطوارئ فاضي حاليًا",
    "Smart search is not available right now, please browse the cases": "البحث الذكي غير متاح الحين، تصفح الحالات تحت",
    "Your organization registration is awaiting platform administrator verification": "طلب تسجيل جهتك قيد المراجعة من إدارة المنصة",
    "Your organization is not verified yet. Please contact the platform administrator": "جهتك ما توثقت بعد، تواصل مع إدارة المنصة",
    "Access denied": "ما عندك صلاحية",
    "Organization email already exists": "بريد الجهة مسجل من قبل",
    "Organization administrator email already exists": "بريد مدير الجهة مسجل من قبل",
    "Registration type must be BENEFICIARY or PROVIDER": "نوع الجهة لازم يكون مستفيدة أو مقدمة خدمة",
    "Only rejected registrations can be resubmitted": "ما يمكن إعادة تقديم إلا الطلبات المرفوضة",
    "Invalid organization administrator credentials": "بيانات مدير الجهة غير صحيحة",
    "Organization name is required": "اسم الجهة مطلوب",
    "Organization type is required": "نوع الجهة مطلوب",
    "Sector is required": "القطاع مطلوب",
    "Organization email is required": "بريد الجهة مطلوب",
    "Invalid organization email format": "صيغة بريد الجهة غير صحيحة",
    "Phone number is required": "رقم الجوال مطلوب",
    "Phone number must be a valid Saudi number": "رقم الجوال لازم يكون سعودي صحيح يبدأ بـ 05",
    "Organization administrator full name is required": "اسم مدير الجهة مطلوب",
    "Organization administrator full name is too long": "اسم مدير الجهة طويل جدًا",
    "Organization administrator email is required": "بريد مدير الجهة مطلوب",
    "Invalid organization administrator email format": "صيغة بريد مدير الجهة غير صحيحة",
    "Organization administrator password is required": "كلمة مرور مدير الجهة مطلوبة",
    "Password must be at least 8 characters": "كلمة المرور لازم تكون 8 أحرف على الأقل",
    "Rejection reason is required": "سبب الرفض مطلوب",
    "Rejection reason is too long": "سبب الرفض طويل جدًا",
    "No document was uploaded for this organization": "ما فيه مستند مرفوع لهذي الجهة",
    "A payment can only be started for an accepted training request": "الدفع يبدأ فقط بعد قبول طلب التدريب",
    "A payment is already pending or paid for this training request": "فيه دفعة قيد المعالجة أو مدفوعة لهذا الطلب",
    "Moyasar is not configured. Set MOYASAR_SECRET_KEY to a test or live secret key": "بوابة ميسر غير مضبوطة حاليًا، تواصل مع إدارة المنصة",
    "This training program is not active right now": "هذا البرنامج غير متاح حاليًا",
    "The training date and time are required": "تاريخ ووقت التدريب مطلوبان",
    "The training start time must be in the future": "وقت بداية التدريب لازم يكون في المستقبل",
    "The training end time must be after the start time": "وقت نهاية التدريب لازم يكون بعد وقت البداية",
    "The selected date and time is already booked for this program": "الوقت المحدد محجوز لهذا البرنامج",
    "A training request can only be edited while it is pending": "طلب التدريب يُعدل فقط وهو قيد الانتظار",
    "Only pending or rejected requests can be deleted": "تُحذف فقط الطلبات قيد الانتظار أو المرفوضة",
    "A training program with this title already exists for this organization": "فيه برنامج بنفس الاسم لهذي الجهة",
    "Cannot delete a training program that has training requests": "لا يمكن حذف برنامج له طلبات تدريب",
    "Invalid training program category": "تصنيف البرنامج غير صالح",
    "Price must be at least 1 SAR": "السعر لازم يكون 1 ريال على الأقل",
    "Program hours per day must be at least 0.5": "عدد الساعات في اليوم لازم يكون 0.5 على الأقل",
    "Program hours per day must not exceed 24": "عدد الساعات في اليوم لازم يكون 24 أو أقل",
    "Maximum days must be at least 1": "الحد الأقصى للأيام لازم يكون 1 على الأقل",
    "Maximum days must not exceed 365": "الحد الأقصى للأيام لازم يكون 365 أو أقل",
    "The training start date must be in the future": "تاريخ بداية التدريب لازم يكون في المستقبل",
    "Program image is required": "اختر صورة للبرنامج",
    "Program image must not exceed 5 MB": "حجم صورة البرنامج لازم يكون 5 ميجابايت أو أقل",
    "Could not read the uploaded program image": "ما قدرنا نقرأ صورة البرنامج المرفوعة",
    "Could not save the uploaded program image": "ما قدرنا نحفظ صورة البرنامج المرفوعة",
    "Program image must be a JPEG, PNG or WebP file": "صورة البرنامج لازم تكون JPEG أو PNG أو WebP",
    "Program image not found": "صورة البرنامج غير موجودة",
    "Invalid program image reference": "مرجع صورة البرنامج غير صالح",
    "This training program does not have a fixed daily duration yet": "هذا البرنامج ما له مدة يومية محددة بعد",
    "This training program does not have a maximum duration yet": "هذا البرنامج ما له حد أقصى للأيام بعد",
    "The selected number of training days exceeds the program maximum": "عدد الأيام المحدد يتجاوز الحد الأقصى للبرنامج",
    "The number of training days must be at least 1": "عدد أيام التدريب لازم يكون 1 على الأقل",
    "The number of training days must not exceed 365": "عدد أيام التدريب لازم يكون 365 أو أقل",
    "Only a pending request can be accepted": "يمكن قبول الطلبات قيد الانتظار فقط",
    "Only a pending request can be rejected": "يمكن رفض الطلبات قيد الانتظار فقط",
    "A training request cannot be cancelled or refunded after the training has started": "ما يمكن إلغاء أو استرداد طلب بعد بدء التدريب",
    "You do not have access to this training request": "ما عندك صلاحية على طلب التدريب هذا",
    "Only the beneficiary organization can perform this action": "الإجراء هذا متاح للجهة المستفيدة فقط",
    "Only the training provider can perform this action": "الإجراء هذا متاح لمقدم الخدمة فقط",
    "Only the provider organization that owns this program can manage it": "إدارة البرنامج متاحة للجهة المقدمة له فقط",
    "Built-in awareness days cannot be deleted. Unpublish it instead": "أيام التقويم الأساسية ما يمكن حذفها، أخفها بإلغاء النشر بدلًا من ذلك",
    "A date rule can only be used with ANNUAL recurrence": "قاعدة التاريخ تُستخدم فقط مع التكرار السنوي",
    "Date rule must look like FIXED:11-14, NTH_WEEKDAY:09:SATURDAY:2, or LAST_WEEKDAY:01:SUNDAY": "صيغة قاعدة التاريخ غير صحيحة",
    "Year must be between 2000 and 2100": "السنة لازم تكون بين 2000 و2100",
    "Awareness day not found": "اليوم التوعوي غير موجود",
    "Resource not found": "المسار المطلوب غير موجود",
    "Only the training provider can use the weekly summary assistant": "ملخص الأسبوع متاح لمقدم الخدمة فقط",
    "Only the beneficiary organization can use the weekly summary assistant": "ملخص الأسبوع متاح للجهة المستفيدة فقط",
    "Only the platform administrator can use the weekly summary assistant": "ملخص الأسبوع متاح لإدارة المنصة فقط",
    "Weekly summary period must be between 1 and 30 days": "فترة ملخص الأسبوع لازم تكون بين 1 و30 يوم",
    "AI weekly summary is not configured": "ملخص الذكاء الاصطناعي غير مضبوط حاليًا",
    "AI weekly summary is unavailable, please try again": "ملخص الذكاء الاصطناعي غير متاح حاليًا، جرّب مرة ثانية",
    "Only the beneficiary organization can use the announcement assistant": "الإعلان الداخلي متاح للجهة المستفيدة فقط",
    "AI announcement is not configured": "الإعلان الذكي غير مضبوط حاليًا",
    "AI announcement is unavailable, please try again": "الإعلان الذكي غير متاح حاليًا، جرّب مرة ثانية",
    "The training date is not set for this request": "تاريخ التدريب غير محدد لهذا الطلب",
    "This training request is not ready for an internal announcement": "طلب التدريب غير جاهز حاليًا لإعلان داخلي",
    "Training request is required": "معرّف طلب التدريب مطلوب",
    "Unsupported announcement channel": "قناة الإعلان غير مدعومة",
    "Only the training provider can use the session prep assistant": "تجهيز الجلسة متاح لمقدم الخدمة فقط",
    "Session prep is available after you accept the request": "تجهيز الجلسة متاح بعد ما تقبل الطلب",
    "AI session prep is not configured": "تجهيز الجلسة بالذكاء الاصطناعي غير مضبوط حاليًا",
    "AI session prep is unavailable, please try again": "تجهيز الجلسة غير متاح حاليًا، جرّب مرة ثانية"
};

// In English mode the backend message is shown as it is
function toArabic(message) {
    if (!message) {
        return tr("صار خطأ غير متوقع، جرّب مرة ثانية", "Something went wrong, please try again");
    }
    if (LANG === "en") {
        return message;
    }
    if (message.startsWith("Your organization registration was rejected")) {
        const reason = message.split(": ").slice(1).join(": ")
            .replace(/\. Please update your registration and resubmit it\.$/, "");
        return "تم رفض طلب تسجيل جهتك"
            + (reason ? ": " + reason : "")
            + ". حدّث بيانات طلبك وتواصل مع إدارة المنصة";
    }
    return ARABIC_MESSAGES[message] || message;
}

function errorMessageFrom(data) {
    if (data && data.message) {
        return data.message;
    }
    if (data && typeof data === "object") {
        const metadataKeys = ["timestamp", "status", "error", "path", "trace", "exception"];
        const firstError = Object.entries(data).find(function (entry) {
            return metadataKeys.indexOf(entry[0]) === -1
                && typeof entry[1] === "string"
                && entry[1];
        });
        return firstError ? firstError[1] : null;
    }
    return null;
}

function clearSession() {
    try {
        localStorage.removeItem("waeiToken");
        localStorage.removeItem("waeiUser");
    } catch (e) {
        // Storage can be blocked
    }
}

function redirectToLogin() {
    if (window.location.pathname === "/login") {
        return;
    }
    const next = window.location.pathname + window.location.search;
    window.location.href = "/login?next=" + encodeURIComponent(next);
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
        if (response.status === 401 && getToken()) {
            clearSession();
            redirectToLogin();
        }
        const message = errorMessageFrom(data);
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
    const options = {
        method: "POST",
        headers: authHeaders()
    };
    if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    try {
        const response = await fetch(API_BASE + path, options);
        return await handleResponse(response);
    } catch (e) {
        throw networkError(e);
    }
}

async function apiPut(path, body) {
    const options = {
        method: "PUT",
        headers: authHeaders()
    };
    if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    try {
        const response = await fetch(API_BASE + path, options);
        return await handleResponse(response);
    } catch (e) {
        throw networkError(e);
    }
}

async function apiDelete(path) {
    try {
        const response = await fetch(API_BASE + path, {
            method: "DELETE",
            headers: authHeaders()
        });
        return await handleResponse(response);
    } catch (e) {
        throw networkError(e);
    }
}

async function apiDownload(path, fileName) {
    try {
        const response = await fetch(API_BASE + path, { headers: authHeaders() });
        if (!response.ok) {
            let data = null;
            const text = await response.text();
            if (text) {
                try {
                    data = JSON.parse(text);
                } catch (e) {
                    data = { message: text };
                }
            }
            if (response.status === 401 && getToken()) {
                clearSession();
                redirectToLogin();
            }
            throw new Error(toArabic(errorMessageFrom(data)));
        }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = fileName || "document";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    } catch (e) {
        throw networkError(e);
    }
}

// For uploading files (multipart/form-data): the browser sets the Content-Type itself.
// Pass withAuth = false for public endpoints such as organization registration.
async function apiPostForm(path, formData, withAuth = true) {
    try {
        const response = await fetch(API_BASE + path, {
            method: "POST",
            headers: withAuth ? authHeaders() : {},
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
        if (authResponse.token) {
            localStorage.setItem("waeiToken", authResponse.token);
        }
        localStorage.setItem("waeiUser", JSON.stringify({
            accountId: authResponse.accountId,
            fullName: authResponse.fullName,
            email: authResponse.email,
            role: authResponse.role,
            status: authResponse.status,
            organizationId: authResponse.organizationId,
            organizationName: authResponse.organizationName,
            organizationType: authResponse.organizationType
        }));
    } catch (e) {
        // Storage can be blocked (private window): the login still works for this page
    }
}

function dashboardPath(user) {
    if (!user) {
        return "/login";
    }
    if (user.role === "PLATFORM_ADMIN") {
        return "/dashboard/admin";
    }
    if (user.role === "ORG_ADMIN") {
        if (user.organizationType === "PROVIDER" || user.organizationType === "BOTH") {
            return "/dashboard/provider";
        }
        if (user.organizationType === "BENEFICIARY") {
            return "/dashboard/beneficiary";
        }
    }
    return "/login";
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

function logout(redirectTo) {
    clearSession();
    window.location.href = redirectTo || "/";
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

// Header: if the user is logged in, "تسجيل الدخول" becomes their name and "انضم لواعي" is hidden
document.addEventListener("DOMContentLoaded", function () {
    const loginLink = document.getElementById("headerLogin");
    const user = getUser();
    if (loginLink && user && getToken()) {
        loginLink.textContent = user.fullName || tr("حسابي", "My account");
        loginLink.href = dashboardPath(user);
        const joinLink = document.getElementById("headerJoin");
        if (joinLink) joinLink.style.display = "none";
    }
});

// ===== Training cards (shared by the home page and /trainings) =====

// Each category gets a color that matches its meaning
function categoryTagClass(category) {
    const c = (category || "").toLowerCase();
    if (c.includes("إسعاف") || c.includes("اسعاف") || c.includes("first aid")) return "tag-firstaid";
    if (c.includes("سلامة") || c.includes("حريق") || c.includes("safety") || c.includes("fire")) return "tag-safety";
    if (c.includes("كوارث") || c.includes("زلزال") || c.includes("سيول") || c.includes("disaster")) return "tag-disaster";
    if (c.includes("طفل") || c.includes("أطفال") || c.includes("child") || c.includes("kids")) return "tag-kids";
    if (c.includes("نفس") || c.includes("mental")) return "tag-mental";
    if (c.includes("تغذية") || c.includes("nutrition")) return "tag-nutrition";
    if (c.includes("لياقة") || c.includes("fitness")) return "tag-fitness";
    if (c.includes("صحة") || c.includes("health")) return "tag-health";
    return "tag-default";
}

// Shows the correct steps one per line (first few only, the rest are on the training page)
function stepsPreviewHtml(description, maxSteps) {
    const lines = (description || "").split("\n").map(function (l) { return l.trim(); }).filter(Boolean);
    const steps = lines.filter(function (l) { return /^\d+[.)\-]/.test(l); });
    if (steps.length === 0) {
        return '<p class="steps-plain">' + escapeHtml(description || "") + "</p>";
    }
    let html = '<ol class="steps-preview">';
    steps.forEach(function (step, i) {
        const number = step.match(/^\d+/)[0];
        const text = step.replace(/^\d+[.)\-]\s*/, "");
        html += '<li' + (i >= maxSteps ? ' class="step-extra" hidden' : "") + '><span class="step-num">' + number + '</span><span class="step-text">' + escapeHtml(text) + "</span></li>";
    });
    html += "</ol>";
    const rest = steps.length - maxSteps;
    if (rest > 0) {
        html += '<button type="button" class="steps-more" aria-expanded="false" data-more="' + rest + '">'
            + tr("+ " + rest + " خطوات ثانية", "+ " + rest + " more steps") + "</button>";
    }
    return html;
}

function trainingCardHtml(t) {
    // Same instruction on every card; the official video is shown as a separate small tag
    const meta = tr("ارفع فيديو حتى دقيقة", "Upload a video up to 1 minute");
    const videoTag = t.officialVideoUrl
        ? '<span class="video-tag"><svg aria-hidden="true" width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"></path></svg>' + tr("فيه فيديو رسمي", "Official video") + "</span>"
        : "";
    return '<article class="training-card ' + categoryTagClass(t.category).replace("tag-", "tone-") + '">'
        + '<div class="training-tags"><span class="tag ' + categoryTagClass(t.category) + '">' + escapeHtml(t.category) + "</span>" + videoTag + "</div>"
        + "<h3>" + escapeHtml(t.title) + "</h3>"
        + '<div class="steps-wrap">' + stepsPreviewHtml(t.description, 3) + "</div>"
        + '<div class="card-footer">'
        + '<span class="meta">' + meta + "</span>"
        + '<a class="btn btn-primary" style="min-height: 44px; font-size: 15px" href="/train?id=' + t.id + '">' + tr("ابدأ", "Start") + "</a>"
        + "</div></article>";
}

// Icon for each category tone (same icons as the provider's add-program form)
const CATEGORY_ICON_PATHS = {
    "tag-health": '<path d="M6 3v6a6 6 0 0 0 12 0V3"></path><path d="M12 15v2a4 4 0 0 0 8 0v-1"></path><circle cx="20" cy="14" r="2"></circle>',
    "tag-safety": '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6l-8-3z"></path><path d="m9 12 2 2 4-4"></path>',
    "tag-firstaid": '<path d="M19.5 13.5 12 21l-7.5-7.5A5 5 0 0 1 12 6.5a5 5 0 0 1 7.5 7z"></path><path d="M8 12h2l1-2 2 4 1-2h2"></path>',
    "tag-mental": '<circle cx="12" cy="12" r="9"></circle><path d="M8.5 14.5c1 1.2 2.2 1.8 3.5 1.8s2.5-.6 3.5-1.8"></path><path d="M9 9.5h.01M15 9.5h.01"></path>',
    "tag-nutrition": '<path d="M12 7c-1.5-1.2-5-1.5-6.5 1.5S5 17 8 20c1.3 1.3 2.7 1 4 .2 1.3.8 2.7 1.1 4-.2 3-3 4-8.5 2.5-11.5S13.5 5.8 12 7z"></path><path d="M12 7c0-2 1-3.5 3-4"></path>',
    "tag-fitness": '<path d="M6 7v10M18 7v10M3 10v4M21 10v4M6 12h12"></path>',
    "tag-disaster": '<path d="M3 15c2 0 2-2 4.5-2s2.5 2 4.5 2 2-2 4.5-2 2.5 2 4.5 2"></path><path d="M3 20c2 0 2-2 4.5-2s2.5 2 4.5 2 2-2 4.5-2 2.5 2 4.5 2"></path><path d="M8 9a4 4 0 0 1 8 0"></path>',
    "tag-kids": '<circle cx="12" cy="7" r="3"></circle><path d="M6 21v-2a6 6 0 0 1 12 0v2"></path>',
    "tag-default": '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V2H6.5A2.5 2.5 0 0 0 4 4.5z"></path>'
};

function categoryIconSvg(tone, size) {
    const path = CATEGORY_ICON_PATHS[tone] || CATEGORY_ICON_PATHS["tag-default"];
    const px = size || 20;
    return '<svg aria-hidden="true" width="' + px + '" height="' + px + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + path + "</svg>";
}

// "+ N more steps" opens the full list inside the card (and closes it again)
document.addEventListener("click", function (event) {
    const button = event.target.closest(".steps-more");
    if (!button) {
        return;
    }
    const card = button.closest(".steps-wrap");
    const open = button.getAttribute("aria-expanded") !== "true";
    card.querySelectorAll(".step-extra").forEach(function (li) { li.hidden = !open; });
    card.classList.toggle("steps-open", open);
    button.setAttribute("aria-expanded", String(open));
    const rest = button.dataset.more;
    button.textContent = open
        ? tr("عرض أقل", "Show less")
        : tr("+ " + rest + " خطوات ثانية", "+ " + rest + " more steps");
});

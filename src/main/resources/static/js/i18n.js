// ===== Waei — Arabic / English switch =====
//
// How it works (no library):
// 1. Arabic text is written normally in the HTML.
// 2. The English version goes in a data-en attribute:
//        <h1 data-en="Login">تسجيل الدخول</h1>
//    also data-en-placeholder, data-en-aria (aria-label) and data-en on <title>.
// 3. Text made by JavaScript uses tr("عربي", "English").
// 4. The chosen language is remembered in this browser (localStorage).
//
// This file is loaded in <head> so the page direction is set before it shows.

function readLanguage() {
    // A link like page.html?lang=en wins, then the saved choice, then Arabic
    const fromUrl = new URLSearchParams(window.location.search).get("lang");
    if (fromUrl === "en" || fromUrl === "ar") {
        try {
            localStorage.setItem("waeiLang", fromUrl);
        } catch (e) {
            // Storage blocked: the URL still decides for this page
        }
        return fromUrl;
    }
    try {
        return localStorage.getItem("waeiLang") === "en" ? "en" : "ar";
    } catch (e) {
        return "ar";
    }
}

const LANG = readLanguage();

// Set the direction right away (before the page is drawn)
document.documentElement.lang = LANG;
document.documentElement.dir = LANG === "en" ? "ltr" : "rtl";

function tr(arabicText, englishText) {
    return LANG === "en" ? englishText : arabicText;
}

function setLanguage(lang) {
    try {
        localStorage.setItem("waeiLang", lang);
    } catch (e) {
        // Storage blocked: the switch still works through the URL below
    }
    const url = new URL(window.location.href);
    url.searchParams.set("lang", lang);
    window.location.href = url.toString();
}

function applyLanguage() {
    // Language buttons: show the OTHER language
    document.querySelectorAll(".lang-toggle").forEach(function (button) {
        button.textContent = LANG === "en" ? "العربية" : "English";
        button.setAttribute("lang", LANG === "en" ? "ar" : "en");
        button.setAttribute("aria-label", LANG === "en" ? "التبديل إلى العربية" : "Switch to English");
        button.addEventListener("click", function () {
            setLanguage(LANG === "en" ? "ar" : "en");
        });
    });

    if (LANG !== "en") {
        return;
    }

    const title = document.querySelector("title");
    if (title && title.dataset.en) {
        document.title = title.dataset.en;
    }
    document.querySelectorAll("[data-en]").forEach(function (el) {
        if (el.tagName !== "TITLE") {
            el.innerHTML = el.dataset.en;
        }
    });
    document.querySelectorAll("[data-en-placeholder]").forEach(function (el) {
        el.placeholder = el.dataset.enPlaceholder;
    });
    document.querySelectorAll("[data-en-aria]").forEach(function (el) {
        el.setAttribute("aria-label", el.dataset.enAria);
    });
    document.querySelectorAll("[data-en-alt]").forEach(function (el) {
        el.setAttribute("alt", el.dataset.enAlt);
    });
}

document.addEventListener("DOMContentLoaded", applyLanguage);

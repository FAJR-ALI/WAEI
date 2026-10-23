// ===== Waei awareness calendar page =====
// Reads /api/v1/awareness-calendar/upcoming and /year, so all content comes from MySQL.

(function () {
    const MONTHS = tr(
        ["يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"],
        ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    );

    const CATEGORY_LABELS = {
        FIRST_AID: ["إسعافات أولية", "First aid"],
        EMERGENCY: ["طوارئ وحرائق", "Emergency & fire"],
        CHRONIC: ["أمراض مزمنة", "Chronic conditions"],
        MENTAL_HEALTH: ["صحة نفسية", "Mental health"],
        PUBLIC_HEALTH: ["صحة عامة", "Public health"],
        MATERNAL_CHILD: ["أطفال وأمهات", "Mothers & children"]
    };
    const CATEGORY_ORDER = ["FIRST_AID", "EMERGENCY", "CHRONIC", "MENTAL_HEALTH", "PUBLIC_HEALTH", "MATERNAL_CHILD"];

    const now = new Date();
    const state = {
        view: "upcoming",
        year: now.getFullYear(),
        category: "ALL",
        upcoming: null,
        yearEvents: {}
    };

    let requestId = 0;
    let list;
    let filters;
    let yearControls;
    let yearLabel;

    function init() {
        list = document.getElementById("calendarList");
        filters = document.getElementById("categoryFilters");
        yearControls = document.getElementById("yearControls");
        yearLabel = document.getElementById("yearLabel");

        readUrl();
        bindTabs();
        bindYearButtons();
        renderFilters();
        syncControls();
        load();
    }

    function readUrl() {
        const params = new URLSearchParams(window.location.search);
        if (params.get("view") === "year") {
            state.view = "year";
        }
        const year = Number(params.get("year"));
        if (Number.isInteger(year) && year >= 2000 && year <= 2100) {
            state.year = year;
        }
        const category = params.get("category");
        if (category && CATEGORY_LABELS[category]) {
            state.category = category;
        }
    }

    function bindTabs() {
        document.querySelectorAll("[data-view]").forEach(function (button) {
            button.addEventListener("click", function () {
                const view = button.dataset.view;
                if (state.view === view) {
                    return;
                }
                state.view = view;
                syncControls();
                writeUrl();
                load();
            });
        });
    }

    function bindYearButtons() {
        document.getElementById("prevYear").addEventListener("click", function () {
            changeYear(-1);
        });
        document.getElementById("nextYear").addEventListener("click", function () {
            changeYear(1);
        });
    }

    function changeYear(delta) {
        const next = state.year + delta;
        if (next < 2000 || next > 2100) {
            return;
        }
        state.year = next;
        yearLabel.textContent = state.year;
        writeUrl();
        load();
    }

    function renderFilters() {
        let html = chip("ALL", tr("الكل", "All"));
        for (const key of CATEGORY_ORDER) {
            html += chip(key, tr(CATEGORY_LABELS[key][0], CATEGORY_LABELS[key][1]));
        }
        filters.innerHTML = html;
        filters.querySelectorAll(".chip").forEach(function (button) {
            button.addEventListener("click", function () {
                state.category = button.dataset.category;
                renderFilters();
                writeUrl();
                const loaded = state.view === "year" ? !!state.yearEvents[state.year] : !!state.upcoming;
                if (loaded) {
                    renderCurrent();
                } else {
                    load();
                }
            });
        });
    }

    function chip(value, label) {
        return '<button type="button" class="chip" data-category="' + escapeHtml(value) + '" aria-pressed="'
            + (state.category === value) + '">' + escapeHtml(label) + "</button>";
    }

    function syncControls() {
        document.querySelectorAll("[data-view]").forEach(function (button) {
            button.setAttribute("aria-pressed", button.dataset.view === state.view ? "true" : "false");
        });
        yearControls.classList.toggle("hidden", state.view !== "year");
        yearLabel.textContent = state.year;
    }

    function writeUrl() {
        const params = new URLSearchParams(window.location.search);
        params.set("view", state.view);
        if (state.view === "year") {
            params.set("year", state.year);
        } else {
            params.delete("year");
        }
        if (state.category === "ALL") {
            params.delete("category");
        } else {
            params.set("category", state.category);
        }
        const query = params.toString();
        history.replaceState(null, "", window.location.pathname + (query ? "?" + query : "") + window.location.hash);
    }

    async function load() {
        const token = ++requestId;
        showLoading(list, state.view === "year"
            ? tr("نجيب تقويم السنة...", "Loading the year...")
            : tr("نجيب الأيام التوعوية...", "Loading awareness days..."));

        try {
            if (state.view === "year") {
                const events = await getYear(state.year);
                if (token === requestId) {
                    renderYear(events);
                }
            } else {
                const events = await getUpcoming();
                if (token === requestId) {
                    renderUpcoming(events);
                }
            }
        } catch (e) {
            if (token === requestId) {
                showError(list, e.message);
            }
        }
    }

    async function getYear(year) {
        if (!state.yearEvents[year]) {
            state.yearEvents[year] = await apiGet("/awareness-calendar/year?year=" + year);
        }
        return state.yearEvents[year];
    }

    async function getUpcoming() {
        if (!state.upcoming) {
            state.upcoming = await apiGet("/awareness-calendar/upcoming?limit=20");
        }
        return state.upcoming;
    }

    function renderCurrent() {
        if (state.view === "year") {
            renderYear(state.yearEvents[state.year] || []);
        } else {
            renderUpcoming(state.upcoming || []);
        }
    }

    function renderUpcoming(events) {
        const visible = filterEvents(events);
        if (visible.length === 0) {
            showEmpty(list, state.category === "ALL"
                ? tr("ما فيه أيام توعوية قريبة.", "No upcoming awareness days.")
                : tr("ما فيه أيام توعوية قريبة لهذا التصنيف.", "No upcoming awareness days in this category."));
            return;
        }

        let html = '<div class="grid grid-3">';
        for (const event of visible) {
            html += upcomingCard(event);
        }
        html += "</div>";
        list.innerHTML = html;
        focusHash();
    }

    function upcomingCard(event) {
        const parts = event.date.split("-");
        const day = Number(parts[2]);
        const month = MONTHS[Number(parts[1]) - 1];
        const title = localized(event.title, event.titleEn);
        const description = localized(event.description, event.descriptionEn);
        const left = daysLeftLabel(event.daysLeft);
        const source = sourceLink(event.sourceUrl);

        return '<article class="event-card calendar-event"' + anchorFor(event) + '>'
            + '<time class="event-date" datetime="' + escapeHtml(event.date) + '">'
            + '<span class="day">' + day + '</span><span class="month">' + month + "</span></time>"
            + '<div class="event-body">'
            + (left ? '<span class="event-left">' + escapeHtml(left) + "</span>" : "")
            + categoryTag(event.category)
            + "<strong>" + escapeHtml(title) + "</strong>"
            + (description ? "<span>" + escapeHtml(description) + "</span>" : "")
            + source
            + "</div></article>";
    }

    function renderYear(events) {
        const visible = filterEvents(events);
        if (visible.length === 0) {
            showEmpty(list, state.category === "ALL"
                ? tr("ما فيه أيام توعوية لهذي السنة.", "No awareness days this year.")
                : tr("ما فيه أيام توعوية لهذا التصنيف في هذي السنة.", "No awareness days in this category this year."));
            return;
        }

        const byMonth = Array.from({ length: 12 }, function () { return []; });
        for (const event of visible) {
            const month = Number(event.date.slice(5, 7)) - 1;
            if (month >= 0 && month < 12) {
                byMonth[month].push(event);
            }
        }

        const today = todayIso();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth();

        let html = '<div class="calendar-months">';
        for (let month = 0; month < 12; month++) {
            const isCurrent = state.year === currentYear && month === currentMonth;
            html += '<section class="month-card' + (isCurrent ? " is-current" : "") + '">'
                + '<h2 class="month-title">' + MONTHS[month] + "</h2>";

            if (byMonth[month].length === 0) {
                html += '<p class="month-empty">' + tr("ما فيه أيام توعوية هذا الشهر.", "No awareness days this month.") + "</p>";
            } else {
                html += '<div class="month-events">';
                for (const event of byMonth[month]) {
                    html += monthRow(event, today);
                }
                html += "</div>";
            }
            html += "</section>";
        }
        html += "</div>";
        list.innerHTML = html;
        focusHash();
    }

    function monthRow(event, today) {
        const day = Number(event.date.slice(8, 10));
        const title = localized(event.title, event.titleEn);
        const isPast = event.date < today;

        return '<article class="month-event' + (isPast ? " is-past" : "") + '"' + anchorFor(event) + '>'
            + '<time class="month-day" datetime="' + escapeHtml(event.date) + '">' + day + "</time>"
            + '<div class="month-event-body">'
            + categoryTag(event.category)
            + "<strong>" + escapeHtml(title) + "</strong>"
            + "</div>"
            + sourceLink(event.sourceUrl, true)
            + "</article>";
    }

    function filterEvents(events) {
        if (state.category === "ALL") {
            return events;
        }
        return events.filter(function (event) {
            return event.category === state.category;
        });
    }

    function categoryTag(category) {
        if (!category) {
            return "";
        }
        const pair = CATEGORY_LABELS[category];
        const label = pair ? tr(pair[0], pair[1]) : category;
        const cssClass = pair ? " cat-" + category : "";
        return '<span class="event-tag' + cssClass + '">' + escapeHtml(label) + "</span>";
    }

    function localized(arabicText, englishText) {
        return LANG === "en" && englishText ? englishText : arabicText;
    }

    function daysLeftLabel(daysLeft) {
        if (daysLeft === null || daysLeft === undefined) {
            return "";
        }
        const days = Number(daysLeft);
        if (days === 0) {
            return tr("اليوم", "Today");
        }
        if (days === 1) {
            return tr("بكرة", "Tomorrow");
        }
        if (days > 1) {
            return tr("باقي " + days + " يوم", "In " + days + " days");
        }
        return tr("مضى", "Passed");
    }

    function sourceLink(url, compact) {
        if (!url) {
            return "";
        }
        return '<a class="event-source' + (compact ? " event-source-compact" : "") + '" href="'
            + escapeHtml(url) + '" target="_blank" rel="noopener">' + tr("المصدر", "Source") + "</a>";
    }

    function anchorFor(event) {
        const key = event.slug || event.id;
        return key ? ' id="event-' + escapeHtml(key) + '"' : "";
    }

    function todayIso() {
        const date = new Date();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return date.getFullYear() + "-" + month + "-" + day;
    }

    function focusHash() {
        let id = window.location.hash.replace(/^#/, "");
        try {
            id = decodeURIComponent(id);
        } catch (e) {
            // Keep the raw hash if it is not valid percent-encoding
        }
        if (!id) {
            return;
        }
        const target = document.getElementById(id);
        if (target) {
            window.setTimeout(function () {
                target.scrollIntoView({ behavior: "smooth", block: "center" });
            }, 60);
        }
    }

    document.addEventListener("DOMContentLoaded", init);
})();

// ===== Waei dashboard shell, guards and shared helpers =====

const WaeiDashboard = (function () {
    const ICONS = {
        grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"></rect><rect x="14" y="3" width="7" height="7" rx="1.5"></rect><rect x="3" y="14" width="7" height="7" rx="1.5"></rect><rect x="14" y="14" width="7" height="7" rx="1.5"></rect>',
        building: '<path d="M3 21h18"></path><path d="M5 21V5a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v16"></path><path d="M9 7h3M9 11h3M9 15h3M16 21v-8h3v8"></path>',
        clipboard: '<rect x="5" y="4" width="14" height="17" rx="2"></rect><path d="M9 4.5V3h6v1.5M9 10h6M9 14h6M9 18h4"></path>',
        users: '<path d="M17 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9.5" cy="7" r="4"></circle><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"></path>',
        book: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>',
        calendar: '<rect x="3" y="4" width="18" height="18" rx="2"></rect><path d="M16 2v4M8 2v4M3 10h18"></path>',
        card: '<rect x="2" y="5" width="20" height="14" rx="2"></rect><path d="M2 10h20"></path>',
        play: '<rect x="2" y="5" width="20" height="14" rx="2"></rect><path d="m10 9 5 3-5 3z"></path>',
        shield: '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6l-8-3z"></path><path d="M12 9v6M9 12h6"></path>',
        home: '<path d="M3 10.5 12 3l9 7.5"></path><path d="M5 9.5V21h14V9.5"></path><path d="M9 21v-6h6v6"></path>',
        external: '<path d="M14 3h7v7M10 14 21 3"></path><path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5"></path>'
    };

    const STATUS = {
        organization: {
            PENDING: ['قيد المراجعة', 'Pending review', 'warn'],
            VERIFIED: ['موثقة', 'Verified', 'ok'],
            REJECTED: ['مرفوضة', 'Rejected', 'bad']
        },
        request: {
            PENDING: ['قيد الانتظار', 'Pending', 'warn'],
            ACCEPTED: ['بانتظار الدفع', 'Awaiting payment', 'orange'],
            REJECTED: ['مرفوض', 'Rejected', 'bad'],
            PAYMENT_PENDING: ['الدفع قيد المعالجة', 'Payment in progress', 'blue'],
            CONFIRMED: ['مؤكد', 'Confirmed', 'teal'],
            IN_PROGRESS: ['قيد التنفيذ', 'In progress', 'purple'],
            COMPLETED: ['مكتمل', 'Completed', 'ok'],
            CANCELLED: ['ملغي', 'Cancelled', 'muted']
        },
        payment: {
            PENDING: ['قيد المعالجة', 'Pending', 'warn'],
            PAID: ['مدفوع', 'Paid', 'ok'],
            FAILED: ['فشل', 'Failed', 'bad'],
            REFUNDED: ['مسترد', 'Refunded', 'blue'],
            CANCELLED: ['ملغي', 'Cancelled', 'muted']
        }
    };

    const TYPES = {
        BENEFICIARY: ['جهة مستفيدة', 'Beneficiary organization'],
        PROVIDER: ['جهة مقدمة خدمة', 'Provider organization'],
        BOTH: ['مستفيدة ومقدمة خدمة', 'Beneficiary and provider']
    };

    const MODES = {
        IN_PERSON: ['حضوري', 'In person'],
        ONLINE: ['عن بعد', 'Online']
    };

    let state = { user: null, config: null };

    function icon(name, size) {
        const path = ICONS[name] || ICONS.grid;
        return '<svg aria-hidden="true" width="' + (size || 20) + '" height="' + (size || 20)
            + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" '
            + 'stroke-linecap="round" stroke-linejoin="round">' + path + '</svg>';
    }

    function locale() {
        return LANG === "en" ? "en-US" : "ar-SA-u-ca-gregory";
    }

    function formatNumber(value) {
        if (value === null || value === undefined || value === "") {
            return "—";
        }
        return Number(value).toLocaleString(locale());
    }

    function formatMoney(value) {
        if (value === null || value === undefined || value === "") {
            return "—";
        }
        return Number(value).toLocaleString(locale(), {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }) + " " + riyalSymbol();
    }

    // New Saudi Riyal symbol (HTML, so set it with innerHTML not textContent)
    function riyalSymbol() {
        return '<span class="sar" role="img" aria-label="' + tr("ريال سعودي", "Saudi riyal") + '"></span>';
    }

    function formatDate(value) {
        if (!value) {
            return "—";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return escapeHtml(value);
        }
        return new Intl.DateTimeFormat(locale(), { dateStyle: "medium" }).format(date);
    }

    function formatDateTime(value) {
        if (!value) {
            return "—";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return escapeHtml(value);
        }
        return new Intl.DateTimeFormat(locale(), {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(date);
    }

    function formatFileSize(bytes) {
        if (!bytes && bytes !== 0) {
            return "—";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024).toFixed(1) + " KB";
        }
        return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    }

    function toInputDateTime(value) {
        return value ? String(value).slice(0, 16) : "";
    }

    function toApiDateTime(value) {
        if (!value) {
            return null;
        }
        return value.length === 16 ? value + ":00" : value;
    }

    function statusLabel(kind, status) {
        const entry = STATUS[kind] && STATUS[kind][status];
        return entry ? tr(entry[0], entry[1]) : (status || "—");
    }

    function statusBadge(kind, status) {
        const entry = STATUS[kind] && STATUS[kind][status];
        const tone = entry ? entry[2] : "muted";
        return '<span class="badge badge-status badge-' + tone + '">' + escapeHtml(statusLabel(kind, status)) + "</span>";
    }

    function typeLabel(type) {
        const entry = TYPES[type];
        return entry ? tr(entry[0], entry[1]) : (type || "—");
    }

    function modeLabel(mode) {
        const entry = MODES[mode];
        return entry ? tr(entry[0], entry[1]) : (mode || "—");
    }

    function roleLabel(user) {
        if (!user) {
            return "";
        }
        if (user.role === "PLATFORM_ADMIN") {
            return tr("إدارة المنصة", "Platform admin");
        }
        return typeLabel(user.organizationType);
    }

    function notify(message, tone) {
        let root = document.getElementById("toastRoot");
        if (!root) {
            root = document.createElement("div");
            root.id = "toastRoot";
            root.className = "toast-root";
            root.setAttribute("aria-live", "polite");
            document.body.appendChild(root);
        }
        const toast = document.createElement("div");
        toast.className = "toast" + (tone ? " " + tone : "");
        toast.textContent = message;
        root.appendChild(toast);
        window.setTimeout(function () {
            toast.remove();
        }, 4200);
    }

    function copyText(text) {
        return new Promise(function (resolve, reject) {
            if (navigator.clipboard && window.isSecureContext) {
                navigator.clipboard.writeText(text).then(resolve).catch(reject);
            } else {
                const input = document.createElement("textarea");
                input.value = text;
                input.style.position = "fixed";
                input.style.opacity = 0;
                document.body.appendChild(input);
                input.select();
                try {
                    document.execCommand("copy");
                    resolve();
                } catch (e) {
                    reject(e);
                } finally {
                    input.remove();
                }
            }
        });
    }

    function openDialog(id) {
        const dialog = document.getElementById(id);
        if (dialog && typeof dialog.showModal === "function") {
            dialog.showModal();
        }
    }

    function closeDialog(id) {
        const dialog = document.getElementById(id);
        if (dialog) {
            dialog.close();
        }
    }

    function confirmAction(arabicText, englishText) {
        return window.confirm(tr(arabicText, englishText));
    }

    function setNavBadge(sectionId, count) {
        const badge = document.querySelector('[data-nav-badge="' + sectionId + '"]');
        if (!badge) {
            return;
        }
        badge.textContent = count > 99 ? "99+" : String(count);
        badge.classList.toggle("hidden", !count);
    }

    function setPanel(panelId) {
        const panels = document.querySelectorAll("[data-panel]");
        let found = false;
        panels.forEach(function (panel) {
            const match = panel.dataset.panel === panelId;
            panel.classList.toggle("hidden", !match);
            if (match) {
                found = true;
            }
        });
        if (!found) {
            return;
        }
        document.querySelectorAll(".app-nav a[data-nav]").forEach(function (link) {
            const active = link.dataset.nav === panelId;
            link.classList.toggle("active", active);
            if (active) {
                link.setAttribute("aria-current", "page");
            } else {
                link.removeAttribute("aria-current");
            }
        });
        if (window.location.hash !== "#" + panelId) {
            history.replaceState(null, "", "#" + panelId);
        }
        document.getElementById("appShell").classList.remove("menu-open");
        document.body.classList.remove("menu-open");
    }

    function buildNav(nav) {
        const navRoot = document.getElementById("appNav");
        let html = "";
        nav.forEach(function (item) {
            html += '<a href="#' + item.id + '" data-nav="' + item.id + '">'
                + icon(item.icon)
                + "<span>" + escapeHtml(tr(item.ar, item.en)) + "</span>"
                + (item.badge ? '<span class="nav-badge hidden" data-nav-badge="' + item.id + '">0</span>' : "")
                + "</a>";
        });
        navRoot.innerHTML = html;

        nav.forEach(function (item) {
            const link = navRoot.querySelector('[data-nav="' + item.id + '"]');
            link.addEventListener("click", function (event) {
                event.preventDefault();
                setPanel(item.id);
            });
        });
    }

    function addSidebarLink(href, arabicText, englishText) {
        const footer = document.getElementById("sidebarExtra");
        if (!footer) {
            return;
        }
        const link = document.createElement("a");
        link.className = "sidebar-link";
        link.href = href;
        link.innerHTML = icon("external", 17) + "<span>" + escapeHtml(tr(arabicText, englishText)) + "</span>";
        footer.appendChild(link);
    }

    function fillIdentity(user) {
        document.querySelectorAll("[data-user-name]").forEach(function (el) {
            el.textContent = user.fullName || user.email || "";
        });
        document.querySelectorAll("[data-user-email]").forEach(function (el) {
            el.textContent = user.email || "";
        });
        document.querySelectorAll("[data-org-name]").forEach(function (el) {
            el.textContent = user.organizationName || tr("إدارة المنصة", "Platform administration");
        });
        document.querySelectorAll("[data-user-initials]").forEach(function (el) {
            const source = (user.fullName || user.email || "?").trim();
            const parts = source.split(/\s+/).filter(Boolean).slice(0, 2);
            el.textContent = parts.map(function (part) {
                return part.charAt(0).toUpperCase();
            }).join("");
        });
        document.querySelectorAll("[data-role-label]").forEach(function (el) {
            el.textContent = roleLabel(user);
        });
    }

    function showGuardError(message) {
        const shell = document.getElementById("appShell");
        if (shell) {
            shell.classList.add("hidden");
        }
        let stateRoot = document.getElementById("guardState");
        if (!stateRoot) {
            stateRoot = document.createElement("div");
            stateRoot.id = "guardState";
            stateRoot.className = "guard-state";
            document.body.appendChild(stateRoot);
        }
        stateRoot.innerHTML = '<div class="card">'
            + '<h1 style="font-size: 25px">' + tr("تعذر فتح لوحة التحكم", "Could not open the dashboard") + "</h1>"
            + '<p style="color: var(--muted); margin: 8px 0 20px">' + escapeHtml(message || tr("جرّب تحديث الصفحة.", "Try refreshing the page.")) + "</p>"
            + '<button type="button" class="btn btn-primary" onclick="window.location.reload()">'
            + tr("تحديث الصفحة", "Refresh page") + "</button> "
            + '<a href="/login" class="btn btn-outline">' + tr("تسجيل الدخول", "Log in") + "</a>"
            + "</div>";
    }

    function bindShell() {
        const shell = document.getElementById("appShell");
        const toggle = document.getElementById("menuToggle");
        const overlay = document.getElementById("appOverlay");
        if (toggle) {
            toggle.addEventListener("click", function () {
                const open = shell.classList.toggle("menu-open");
                document.body.classList.toggle("menu-open", open);
            });
        }
        if (overlay) {
            overlay.addEventListener("click", function () {
                shell.classList.remove("menu-open");
                document.body.classList.remove("menu-open");
            });
        }
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                shell.classList.remove("menu-open");
                document.body.classList.remove("menu-open");
            }
        });
        document.querySelectorAll("[data-logout]").forEach(function (button) {
            button.addEventListener("click", function () {
                logout("/login");
            });
        });
        document.querySelectorAll("[data-close-dialog]").forEach(function (button) {
            button.addEventListener("click", function () {
                const dialog = button.closest("dialog");
                if (dialog) {
                    dialog.close();
                }
            });
        });
        document.querySelectorAll("dialog").forEach(function (dialog) {
            dialog.addEventListener("click", function (event) {
                if (event.target === dialog) {
                    dialog.close();
                }
            });
        });
    }

    // ===== Notifications bell =====
    // Each dashboard builds its notifications from the data it already loads and passes them here:
    // { id, title, text, time, panel, tone }. "Seen" ids are remembered in this browser per account,
    // so a notification counts as new until the bell is opened. When the status changes the id changes,
    // so the change shows up as a new notification.
    let notifItems = [];

    function notifStorageKey() {
        const user = state.user || {};
        return "waeiSeenNotifs:" + (user.email || user.id || "me");
    }

    function readSeen() {
        try {
            return new Set(JSON.parse(localStorage.getItem(notifStorageKey()) || "[]"));
        } catch (e) {
            return new Set();
        }
    }

    function saveSeen(ids) {
        try {
            localStorage.setItem(notifStorageKey(), JSON.stringify(Array.from(ids).slice(-300)));
        } catch (e) {
            // Storage blocked: the bell still works for this visit
        }
    }

    function timeAgo(value) {
        if (!value) return "";
        const diff = Date.now() - new Date(value).getTime();
        if (isNaN(diff)) return "";
        const future = diff < 0;
        const minutes = Math.round(Math.abs(diff) / 60000);
        let ar, en;
        if (minutes < 60) { ar = minutes + " د"; en = minutes + " min"; }
        else if (minutes < 1440) { const h = Math.round(minutes / 60); ar = h + " س"; en = h + " h"; }
        else { const d = Math.round(minutes / 1440); ar = d + " يوم"; en = d + (d === 1 ? " day" : " days"); }
        if (minutes < 1) return tr("الحين", "now");
        return future ? tr("بعد " + ar, "in " + en) : tr("قبل " + ar, en + " ago");
    }

    function ensureBell() {
        let wrap = document.getElementById("notifWrap");
        if (wrap) return wrap;
        const actions = document.querySelector(".topbar-actions");
        if (!actions) return null;
        wrap = document.createElement("div");
        wrap.id = "notifWrap";
        wrap.className = "notif-wrap";
        wrap.innerHTML = '<button type="button" class="icon-btn notif-btn" id="notifBtn" aria-haspopup="true" aria-expanded="false" aria-controls="notifPanel">'
            + '<svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"></path><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"></path></svg>'
            + '<span class="notif-count hidden" id="notifCount"></span></button>'
            + '<div class="notif-panel hidden" id="notifPanel" role="region">'
            + '<div class="notif-head"><strong>' + tr("الإشعارات", "Notifications") + '</strong></div>'
            + '<div class="notif-list" id="notifList"></div></div>';
        const lang = actions.querySelector(".lang-toggle");
        actions.insertBefore(wrap, lang || actions.firstChild);

        const button = wrap.querySelector("#notifBtn");
        const panel = wrap.querySelector("#notifPanel");
        panel.setAttribute("aria-label", tr("الإشعارات", "Notifications"));
        button.addEventListener("click", function (event) {
            event.stopPropagation();
            const open = panel.classList.contains("hidden");
            panel.classList.toggle("hidden", !open);
            button.setAttribute("aria-expanded", open ? "true" : "false");
            if (open) {
                renderNotifList(readSeen());
                // Opening the bell marks everything as seen
                const seen = readSeen();
                notifItems.forEach(function (item) { seen.add(item.id); });
                saveSeen(seen);
                renderNotifCount();
            }
        });
        document.addEventListener("click", function (event) {
            if (!wrap.contains(event.target)) closeNotifPanel();
        });
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") closeNotifPanel();
        });
        return wrap;
    }

    function closeNotifPanel() {
        const panel = document.getElementById("notifPanel");
        const button = document.getElementById("notifBtn");
        if (!panel || panel.classList.contains("hidden")) return;
        panel.classList.add("hidden");
        button.setAttribute("aria-expanded", "false");
    }

    function renderNotifCount() {
        const seen = readSeen();
        const unread = notifItems.filter(function (item) { return !seen.has(item.id); }).length;
        const count = document.getElementById("notifCount");
        const button = document.getElementById("notifBtn");
        if (!count) return;
        count.textContent = unread > 9 ? "9+" : String(unread);
        count.classList.toggle("hidden", !unread);
        button.setAttribute("aria-label", unread
            ? tr("الإشعارات، " + unread + " جديدة", "Notifications, " + unread + " new")
            : tr("الإشعارات", "Notifications"));
    }

    function renderNotifList(seen) {
        const list = document.getElementById("notifList");
        if (!list) return;
        if (!notifItems.length) {
            list.innerHTML = '<p class="notif-empty">' + tr("ما فيه إشعارات حاليًا.", "No notifications right now.") + "</p>";
            return;
        }
        list.innerHTML = notifItems.map(function (item, i) {
            const isNew = !seen.has(item.id);
            return '<button type="button" class="notif-item tone-' + (item.tone || "info") + (isNew ? " is-new" : "") + '" data-notif="' + i + '">'
                + '<span class="notif-dot" aria-hidden="true"></span>'
                + '<span class="notif-body"><strong>' + escapeHtml(item.title) + "</strong>"
                // textHtml is trusted markup we build ourselves (e.g. the riyal symbol from formatMoney)
                + (item.textHtml ? "<span>" + item.textHtml + "</span>" : (item.text ? "<span>" + escapeHtml(item.text) + "</span>" : ""))
                + (item.time ? '<small>' + timeAgo(item.time) + "</small>" : "")
                + "</span>"
                + (isNew ? '<span class="sr-only">' + tr("جديد", "New") + "</span>" : "")
                + "</button>";
        }).join("");
        list.querySelectorAll("[data-notif]").forEach(function (button) {
            button.addEventListener("click", function () {
                const item = notifItems[Number(button.dataset.notif)];
                closeNotifPanel();
                if (item && item.panel) {
                    window.location.hash = item.panel;
                    setPanel(item.panel);
                }
                if (item && typeof item.onClick === "function") item.onClick();
            });
        });
    }

    function setNotifications(items) {
        notifItems = (items || []).filter(Boolean).sort(function (a, b) {
            return new Date(b.time || 0) - new Date(a.time || 0);
        }).slice(0, 25);
        if (!ensureBell()) return;
        renderNotifCount();
        const panel = document.getElementById("notifPanel");
        if (panel && !panel.classList.contains("hidden")) renderNotifList(readSeen());
    }

    // Small date helpers for the dashboards' notification rules
    function daysSince(value) {
        if (!value) return Infinity;
        return (Date.now() - new Date(value).getTime()) / 86400000;
    }

    function daysUntil(value) {
        if (!value) return Infinity;
        return (new Date(value).getTime() - Date.now()) / 86400000;
    }

    async function init(config) {
        state.config = config;
        if (!getToken()) {
            redirectToLogin();
            return null;
        }

        let user;
        try {
            user = await apiGet("/auth/me");
        } catch (e) {
            if (e.status === 401) {
                return null;
            }
            showGuardError(e.message);
            return null;
        }

        if (config.role && user.role !== config.role) {
            window.location.replace(dashboardPath(user));
            return null;
        }
        if (config.orgTypes && config.orgTypes.indexOf(user.organizationType) === -1) {
            window.location.replace(dashboardPath(user));
            return null;
        }

        state.user = user;
        saveLogin(user);
        fillIdentity(user);
        buildNav(config.nav || []);
        bindShell();

        if (user.role === "ORG_ADMIN" && user.organizationType === "BOTH" && config.workspaceSwitch) {
            addSidebarLink(config.workspaceSwitch.href, config.workspaceSwitch.ar, config.workspaceSwitch.en);
        }

        const shell = document.getElementById("appShell");
        const guard = document.getElementById("guardState");
        if (guard) {
            guard.classList.add("hidden");
        }
        shell.classList.remove("hidden");

        const hash = window.location.hash.replace("#", "");
        const validSection = (config.nav || []).some(function (item) {
            return item.id === hash;
        });
        setPanel(validSection ? hash : (config.nav && config.nav[0] ? config.nav[0].id : ""));

        window.addEventListener("hashchange", function () {
            const next = window.location.hash.replace("#", "");
            if ((config.nav || []).some(function (item) { return item.id === next; })) {
                setPanel(next);
            }
        });

        return user;
    }

    return {
        init: init,
        state: state,
        icon: icon,
        setPanel: setPanel,
        setNavBadge: setNavBadge,
        setNotifications: setNotifications,
        daysSince: daysSince,
        daysUntil: daysUntil,
        notify: notify,
        copyText: copyText,
        openDialog: openDialog,
        closeDialog: closeDialog,
        confirmAction: confirmAction,
        statusLabel: statusLabel,
        statusBadge: statusBadge,
        typeLabel: typeLabel,
        modeLabel: modeLabel,
        roleLabel: roleLabel,
        formatNumber: formatNumber,
        formatMoney: formatMoney,
        riyalSymbol: riyalSymbol,
        formatDate: formatDate,
        formatDateTime: formatDateTime,
        formatFileSize: formatFileSize,
        toInputDateTime: toInputDateTime,
        toApiDateTime: toApiDateTime,
        searchIcon: function () {
            return '<svg aria-hidden="true" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>';
        }
    };
})();

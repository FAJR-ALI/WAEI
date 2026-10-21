// ===== Emergency guide: shared helpers for guide.html and guide-detail.html =====
// Needs js/i18n.js and js/api.js before this file

const GUIDE_CATEGORIES = {
    FIRST_AID: tr("إسعافات أولية", "First aid"),
    KIDS: tr("أطفال", "Kids"),
    FIRE: tr("حرائق", "Fire"),
    CHRONIC: tr("أمراض مزمنة", "Chronic conditions"),
    WEATHER: tr("بيئة وطقس", "Weather")
};

// Icon drawing (svg path) + its colors
const GUIDE_ICONS = {
    HEART: { bg: "#FEE2E2", fg: "#B91C1C", path: "M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z" },
    THROAT: { bg: "#FEF3C7", fg: "#B45309", path: "M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM8 14h8l-1 7H9z" },
    BRAIN: { bg: "#EDE9FE", fg: "#6D28D9", path: "M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2zM9 21h6" },
    FLAME: { bg: "#FFEDD5", fg: "#C2410C", path: "M12 2c1 4 5 5 5 10a5 5 0 0 1-10 0c0-3 2-4 2-7 2 1 3 3 3 5" },
    PULSE: { bg: "#DBEAFE", fg: "#1D4ED8", path: "M4 12h3l2-5 4 10 2-5h5" },
    SUN: { bg: "#DCFCE7", fg: "#15803D", path: "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" },
    DROP: { bg: "#FEE2E2", fg: "#B91C1C", path: "M12 2.7l5.7 5.6a8 8 0 1 1-11.3 0z" },
    BABY: { bg: "#FCE7F3", fg: "#BE185D", path: "M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM6 21v-3a6 6 0 0 1 12 0v3" }
};

function guideIconHtml(iconName, size) {
    const icon = GUIDE_ICONS[iconName] || GUIDE_ICONS.HEART;
    return '<span class="guide-icon" style="background: ' + icon.bg + "; color: " + icon.fg + '">'
        + '<svg aria-hidden="true" width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
        + '<path d="' + icon.path + '"></path></svg></span>';
}

function categoryLabel(category) {
    return GUIDE_CATEGORIES[category] || category;
}

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

// ===== Each case has its own color =====
// Seeded cases get a fixed color by their Arabic title; any new case gets one from the palette by its id.
const GUIDE_PALETTE = [
    { bg: "#FEE2E2", fg: "#B91C1C" }, // red
    { bg: "#FEF3C7", fg: "#B45309" }, // amber
    { bg: "#FCE7F3", fg: "#BE185D" }, // pink
    { bg: "#EDE9FE", fg: "#6D28D9" }, // violet
    { bg: "#FFE4E6", fg: "#9F1239" }, // rose
    { bg: "#FFEDD5", fg: "#C2410C" }, // orange
    { bg: "#DBEAFE", fg: "#1D4ED8" }, // blue
    { bg: "#CCFBF1", fg: "#0F766E" }, // teal
    { bg: "#FEF9C3", fg: "#A16207" }, // yellow
    { bg: "#E5E7EB", fg: "#374151" }, // slate
    { bg: "#DCFCE7", fg: "#15803D" }, // green
    { bg: "#E0F2FE", fg: "#0369A1" }  // sky
];

const GUIDE_COLOR_BY_TITLE = {
    "توقف القلب": 0,
    "الاختناق عند البالغ": 1,
    "الاختناق عند الرضيع": 2,
    "الجلطة الدماغية": 3,
    "النزيف الشديد": 4,
    "الحروق": 5,
    "نوبة الصرع": 6,
    "هبوط السكر": 7,
    "ضربة الشمس": 8,
    "حريق في المطبخ": 9
};

// g.arTitle is set by localizeGuide (the original Arabic title)
function guideColor(g) {
    const key = g.arTitle || g.title;
    if (key in GUIDE_COLOR_BY_TITLE) return GUIDE_PALETTE[GUIDE_COLOR_BY_TITLE[key]];
    const n = Number(g.id || g.guideId || 0);
    return GUIDE_PALETTE[Math.abs(n) % GUIDE_PALETTE.length];
}

// Icon drawn in the case's own color
function guideCaseIconHtml(g, size) {
    const icon = GUIDE_ICONS[g.icon] || GUIDE_ICONS.HEART;
    const color = guideColor(g);
    return '<span class="guide-icon" style="background: ' + color.bg + "; color: " + color.fg + '">'
        + '<svg aria-hidden="true" width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
        + '<path d="' + icon.path + '"></path></svg></span>';
}

// ===== English version of the seeded cases =====
// The seeder (GuideInitializer) writes the cases in Arabic only. When the page is in English,
// the seeded cases are shown from this map (matched by their Arabic title).
// Cases the admin adds later are shown as they were written.
const GUIDE_EN = {
    "توقف القلب": {
        title: "Cardiac arrest",
        summary: "Someone collapsed, isn't responding and isn't breathing normally. Start CPR right away.",
        signs: ["Doesn't respond when you talk to them or tap their shoulder", "Not breathing, or only gasping", "Collapsed suddenly"],
        steps: ["Make sure the area is safe for you and for them",
            "Talk to them and tap their shoulder; if they don't respond, call 997 or ask someone to call",
            "Ask for an AED (defibrillator) if one is available",
            "Place the heel of your hand in the center of the chest and your other hand on top",
            "Push hard and fast: 5 to 6 cm deep, 100 to 120 compressions per minute",
            "Let the chest rise fully after each compression",
            "Keep going until the ambulance arrives or they start breathing normally"],
        donts: ["Don't stop compressions to check for a pulse", "Don't leave them alone except to call for help"],
        whenToCall: ["Always: call 997 before you start, or ask someone to call"]
    },
    "الاختناق عند البالغ": {
        title: "Choking (adult)",
        summary: "Can't talk or cough, and is holding their throat.",
        signs: ["Holding their throat", "Can't talk or cough", "Wheezing or no sound at all", "Face turning blue"],
        steps: ["Ask: \"Are you choking?\" If they can cough forcefully, encourage them to keep coughing",
            "If they can't talk or cough, ask someone to call 997",
            "Stand behind them, lean them forward, and give 5 back blows between the shoulder blades with the heel of your hand",
            "If the object doesn't come out: place your fist just above the navel, grab it with your other hand, and give 5 inward and upward thrusts",
            "Repeat the back blows and thrusts until the object comes out or the ambulance arrives",
            "If they become unconscious: lower them to the ground and start CPR"],
        donts: ["Don't put your finger in their mouth unless you can clearly see the object",
            "Don't give them water or food to \"push it down\"",
            "Don't hit their back while they're coughing forcefully; let them cough"],
        whenToCall: ["Can't breathe or talk", "Became unconscious", "The object came out but they still have trouble breathing"]
    },
    "الاختناق عند الرضيع": {
        title: "Choking (infant)",
        summary: "An infant (under 1 year) isn't crying or breathing after swallowing something.",
        signs: ["Not crying or coughing", "Trouble breathing or a wheezing sound", "Turning blue"],
        steps: ["Ask someone to call 997",
            "Lay the baby face down along your forearm, head lower than the body, supporting the head with your hand",
            "Give 5 back blows between the shoulder blades with the heel of your hand",
            "Turn the baby face up, place two fingers in the center of the chest just below the nipple line, and give 5 chest thrusts",
            "Repeat the back blows and chest thrusts until the object comes out or the ambulance arrives",
            "If the baby becomes unconscious, start infant CPR"],
        donts: ["Don't do abdominal thrusts on an infant",
            "Don't put your finger in the baby's mouth unless you can see the object",
            "Don't hold the baby by the legs and shake them"],
        whenToCall: ["Always, when an infant is choking"]
    },
    "الجلطة الدماغية": {
        title: "Stroke",
        summary: "A drooping face, a weak arm, or sudden trouble speaking.",
        signs: ["Face drooping or an uneven smile", "Weakness or numbness in an arm or leg on one side",
            "Slurred speech or trouble understanding", "Sudden severe headache or dizziness"],
        steps: ["Call 997 right away; every minute counts", "Note the time the symptoms started",
            "Let them sit or lie down comfortably", "If they lose consciousness but are breathing, place them on their side",
            "Stay with them until the ambulance arrives"],
        donts: ["Don't give them food, water or medicine", "Don't wait for the symptoms to go away"],
        whenToCall: ["Immediately, at any of these signs"]
    },
    "النزيف الشديد": {
        title: "Severe bleeding",
        summary: "A lot of blood that won't stop. Press directly on the wound.",
        signs: ["Blood pouring or spurting out", "Bleeding doesn't stop after a few minutes", "Pale skin and dizziness"],
        steps: ["Call 997", "Put on gloves if available", "Press directly on the wound with a clean cloth",
            "If the cloth soaks through, add another on top and keep pressing",
            "Keep pressing without stopping until the ambulance arrives"],
        donts: ["Don't remove the first cloth to check the wound", "Don't pull out an object stuck in the wound"],
        whenToCall: ["Bleeding doesn't stop with pressure", "The wound is large or deep", "The injured person feels dizzy or loses consciousness"]
    },
    "الحروق": {
        title: "Burns",
        summary: "A burn from oil, hot water or fire. Cool it under running water.",
        signs: ["Red, painful skin", "Blisters", "White or charred skin in deep burns"],
        steps: ["Move them away from the heat source",
            "Cool the burn under cool running water (not ice-cold) for 20 minutes",
            "Remove rings and watches before the area swells",
            "Cover the burn with a clean non-stick cloth or plastic wrap"],
        donts: ["Don't put ice, toothpaste or butter on it", "Don't pop the blisters", "Don't remove clothing stuck to the skin"],
        whenToCall: ["The burn is large or deep", "The burn is on the face, hands or joints",
            "The injured person is a child or elderly", "Electrical or chemical burn"]
    },
    "نوبة الصرع": {
        title: "Seizure",
        summary: "Sudden convulsions and loss of consciousness. Protect the head and move dangerous objects away.",
        signs: ["Convulsions and involuntary movements", "Loss of consciousness or a fixed stare", "Body stiffening"],
        steps: ["Stay calm and time the seizure", "Move dangerous objects away from them",
            "Put something soft under their head", "After the convulsions stop, place them on their side",
            "Stay with them until they are fully awake"],
        donts: ["Don't hold them down or stop their movements", "Don't put anything in their mouth",
            "Don't give food or water until they are fully awake"],
        whenToCall: ["The seizure lasts more than 5 minutes", "A second seizure follows the first",
            "They got hurt or the seizure happened in water", "It's their first seizure or they don't wake up afterwards"]
    },
    "هبوط السكر": {
        title: "Low blood sugar",
        summary: "Shaking, sweating and dizziness in a person with diabetes. Give fast sugar if they can swallow.",
        signs: ["Shaking and sweating", "Dizziness or extreme hunger", "Confusion or strange behavior", "Pale skin"],
        steps: ["If they are awake and can swallow: give juice, a sugary drink or sugar cubes",
            "Let them sit and rest", "If they don't improve after 15 minutes, give sugar again",
            "Once they feel better, they should eat a light snack"],
        donts: ["Don't give anything by mouth if they are not awake", "Don't leave them alone"],
        whenToCall: ["Lost consciousness", "Didn't improve after sugar", "Can't swallow"]
    },
    "ضربة الشمس": {
        title: "Heat stroke",
        summary: "High body temperature and hot skin after exposure to the sun or heat.",
        signs: ["Very high body temperature", "Hot, red skin", "Headache, dizziness and confusion", "May lose consciousness"],
        steps: ["Call 997", "Move them to a cool or shaded place", "Loosen tight clothing",
            "Cool them quickly: spray water and fan them, and put cold packs on the neck and armpits",
            "If they are awake and can swallow, give water in small sips"],
        donts: ["Don't give water if they are not awake", "Don't leave them in the sun"],
        whenToCall: ["Always, in heat stroke"]
    },
    "حريق في المطبخ": {
        title: "Kitchen fire",
        summary: "A pot of oil on fire. Never use water; cover it and turn off the heat.",
        signs: ["Flames coming from a pot or pan", "Thick smoke from the kitchen"],
        steps: ["Turn off the heat under the pot if you can do it safely",
            "Cover the pot with a metal lid or a fire blanket",
            "Leave the lid on until the pot cools down",
            "If the fire grows, get everyone out of the house and call 998"],
        donts: ["Never put water on burning oil", "Don't move the pot while it's on fire", "Don't go back into a house full of smoke"],
        whenToCall: ["The fire didn't go out or got bigger", "There is thick smoke"]
    }
};

// Returns the case in the page language (keeps the Arabic title in arTitle for the color)
function localizeGuide(g) {
    if (!g) return g;
    const copy = Object.assign({}, g, { arTitle: g.title });
    if (LANG !== "en") return copy;
    const en = GUIDE_EN[g.title];
    if (!en) return copy;
    for (const key of Object.keys(en)) {
        // Only replace a field the API actually sent (cards don't send the lists)
        if (key in g) copy[key] = en[key];
    }
    return copy;
}

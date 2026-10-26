// ===== English version of the seeded trainings and kids stories =====
// AwarenessServiceDataSeeder and KidsStoryDataSeeder write their content in Arabic only.
// When the page is in English, the seeded items are shown from these maps (matched by their Arabic title).
// Items the admin adds later, or AI stories, are shown as they were written.
// Needs js/i18n.js before this file.

const TRAINING_EN = {
    "الإنعاش القلبي الرئوي للبالغ": {
        title: "Adult CPR",
        category: "First aid",
        description: "Correct steps:\n"
            + "1. Make sure the area is safe for you and the injured person.\n"
            + "2. Tap their shoulder and talk loudly; if they don't respond and aren't breathing normally, call 997.\n"
            + "3. Ask for an AED (defibrillator) if one is available.\n"
            + "4. Place the heel of your hand in the center of the chest and your other hand on top, keeping your arms straight and your shoulders over the chest.\n"
            + "5. Push hard and fast: 5 to 6 cm deep, 100 to 120 compressions per minute.\n"
            + "6. Let the chest rise fully after each compression.\n"
            + "7. Keep compressing without stopping until the ambulance arrives or they start breathing normally."
    },
    "الاختناق عند البالغ (مناورة هيمليك)": {
        title: "Adult choking (Heimlich maneuver)",
        category: "First aid",
        description: "Correct steps:\n"
            + "1. Ask: \"Are you choking?\" If they can cough forcefully, encourage them to keep coughing.\n"
            + "2. If they can't talk or cough, ask someone to call 997.\n"
            + "3. Stand behind them, lean them forward, and give 5 back blows between the shoulder blades with the heel of your hand.\n"
            + "4. If the object doesn't come out: place your fist above the navel and below the breastbone, and grab it with your other hand.\n"
            + "5. Press inward and upward forcefully 5 times.\n"
            + "6. Repeat 5 back blows and 5 thrusts until the object comes out or the ambulance arrives.\n"
            + "7. If they become unconscious, lower them gently to the ground and start CPR."
    },
    "وضعية الإفاقة الجانبية": {
        title: "Recovery position",
        category: "First aid",
        description: "Correct steps (for an unconscious person who is breathing normally):\n"
            + "1. Make sure they are breathing normally and call 997.\n"
            + "2. Kneel beside them and place their nearest arm at a right angle to their body.\n"
            + "3. Place the back of their far hand against their near cheek and hold it there.\n"
            + "4. Lift their far knee so the leg is bent with the foot flat on the ground.\n"
            + "5. Pull the far knee toward you until they roll onto their side.\n"
            + "6. Tilt their head back slightly to keep the airway open.\n"
            + "7. Stay with them and watch their breathing until the ambulance arrives."
    },
    "إيقاف النزيف الشديد": {
        title: "Stopping severe bleeding",
        category: "First aid",
        description: "Correct steps:\n"
            + "1. Call 997 and put on gloves if available.\n"
            + "2. Press directly on the wound with a clean cloth or gauze.\n"
            + "3. Press firmly and continuously with both hands.\n"
            + "4. If the cloth soaks through, add another on top and keep pressing without removing the first.\n"
            + "5. Wrap a bandage tightly over the gauze if available.\n"
            + "6. Keep pressing without stopping until the ambulance arrives.\n"
            + "7. Don't pull out any object stuck in the wound."
    },
    "استخدام طفاية الحريق": {
        title: "Using a fire extinguisher",
        category: "Safety",
        description: "Correct steps (PASS method):\n"
            + "1. Alert the people around you, call 998, and make sure you have an exit behind you.\n"
            + "2. Stand 2 to 3 meters from the fire with your back to the exit.\n"
            + "3. Pull the safety pin from the extinguisher.\n"
            + "4. Aim the nozzle at the base of the fire, not the flames.\n"
            + "5. Squeeze the handle steadily.\n"
            + "6. Sweep the nozzle side to side at the base of the fire until it goes out.\n"
            + "7. If the fire grows or the extinguisher runs out, leave immediately and close the door behind you."
    }
};

// Arabic category names used by trainings the admin adds, so the filter chips read in English too
const TRAINING_CATEGORY_EN = {
    "إسعافات أولية": "First aid",
    "سلامة": "Safety",
    "صحة": "Health",
    "صحة نفسية": "Mental health",
    "تغذية": "Nutrition",
    "لياقة": "Fitness",
    "كوارث": "Disasters",
    "أطفال": "Kids"
};

const STORY_EN = {
    "ليان والكوب الساخن": {
        title: "Layan and the Hot Cup",
        topic: "Safety at home",
        content: "Layan saw a hot cup, so she stayed away from it.\nShe called her mom to put it somewhere safe.",
        lesson: "Stay away from hot things and ask a grown-up for help."
    },
    "سعد والحبوب الملونة": {
        title: "Saad and the Colorful Pills",
        topic: "Medicine safety",
        content: "Saad found some colorful pills, but he didn't touch them.\nHe told his dad, who put them away from children.",
        lesson: "Medicine is not candy. Don't touch it on your own."
    },
    "نورة عند المسبح": {
        title: "Noura at the Pool",
        topic: "Safety near water",
        content: "Noura's ball rolled into the pool, so she stopped far from the edge.\nShe asked her mom to get the ball.",
        lesson: "Don't go near the pool without a grown-up."
    },
    "عمر وممر المشاة": {
        title: "Omar and the Crosswalk",
        topic: "Road safety",
        content: "Omar wanted to cross the street, so he waited at the crosswalk.\nHe held his dad's hand, and they crossed after making sure the cars had stopped.",
        lesson: "Cross with a grown-up at the right place."
    },
    "ريم ورائحة الدخان": {
        title: "Reem and the Smell of Smoke",
        topic: "Fire safety",
        content: "Reem noticed smoke, so she moved away and called her mom.\nThey went out to a safe place and didn't go back for the toys.",
        lesson: "Stay away from smoke and tell a grown-up."
    },
    "فهد وصديقه": {
        title: "Fahad and His Friend",
        topic: "Asking for help",
        content: "Fahad's friend fell on the playground and couldn't stand up.\nFahad called the teacher and didn't try to move his friend.",
        lesson: "Ask the teacher for help when your friend gets hurt."
    },
    "جود والسلك المكشوف": {
        title: "Joud and the Exposed Wire",
        topic: "Electrical safety",
        content: "Joud saw an exposed wire, so she moved away and warned her brothers and sisters.\nShe told her dad without touching the wire or trying to fix it.",
        lesson: "Stay away from damaged wires and leave repairs to grown-ups."
    },
    "راشد والمكالمة الواضحة": {
        title: "Rashed and the Clear Call",
        topic: "Communicating in an emergency",
        content: "Rashed practiced with his mom how to describe the situation and the address calmly.\nHe learned that an emergency call is for real help, not for fun.",
        lesson: "Know your address and listen to the emergency operator's instructions."
    },
    "سارة والطريق المغمور": {
        title: "Sara and the Flooded Road",
        topic: "Safety in the rain",
        content: "Sara saw water covering the road, so she stopped in a safe place.\nShe told her family, and together they chose a road away from the water.",
        lesson: "Don't cross standing water or play near flood channels."
    }
};

// Copy of the item with the fields from the map, only for fields the API actually sent
function applySeedTranslation(item, map) {
    if (!item) return item;
    const copy = Object.assign({}, item);
    if (LANG !== "en") return copy;
    const en = map[item.title];
    if (!en) return copy;
    for (const key of Object.keys(en)) {
        if (key in item) copy[key] = en[key];
    }
    copy.isTranslated = true;
    return copy;
}

function localizeTraining(t) {
    const copy = applySeedTranslation(t, TRAINING_EN);
    if (copy && LANG === "en" && copy.category in TRAINING_CATEGORY_EN) {
        copy.category = TRAINING_CATEGORY_EN[copy.category];
    }
    return copy;
}

function localizeStory(s) {
    return applySeedTranslation(s, STORY_EN);
}

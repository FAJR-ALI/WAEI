package org.fadhel.waeiplatform.service;

import java.util.List;

public final class TrainingProgramCategories {

    private static final List<String> CATEGORIES = List.of(
            "صحة",
            "سلامة",
            "إسعافات أولية",
            "صحة نفسية",
            "تغذية",
            "لياقة بدنية"
    );

    private TrainingProgramCategories() {
    }

    public static List<String> all() {
        return CATEGORIES;
    }

    public static boolean isSupported(String category) {
        return category != null && CATEGORIES.contains(category.trim());
    }
}

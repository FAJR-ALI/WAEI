package org.fadhel.waeiplatform.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

// Awareness days are usually yearly, but some of them move:
// World First Aid Day is the second Saturday of September, World Kidney Day is the second Thursday of March, ...
// A rule keeps those dates correct without editing the database every year.
public final class AwarenessDateRule {

    public static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");

    private AwarenessDateRule() {
    }

    public static LocalDate today() {
        return LocalDate.now(RIYADH);
    }

    public static boolean isValid(String rule) {
        if (rule == null || rule.isBlank()) {
            return false;
        }

        String[] parts = rule.trim().toUpperCase(Locale.ROOT).split(":");
        try {
            switch (parts[0]) {
                case "FIXED": {
                    if (parts.length != 2) {
                        return false;
                    }
                    String[] date = parts[1].split("-");
                    if (date.length != 2 || !isValidMonth(date[0])) {
                        return false;
                    }
                    return isValidDay(Integer.parseInt(date[0]), Integer.parseInt(date[1]));
                }
                case "NTH_WEEKDAY":
                    return parts.length == 4
                            && isValidMonth(parts[1])
                            && isValidWeekday(parts[2])
                            && Integer.parseInt(parts[3]) >= 1
                            && Integer.parseInt(parts[3]) <= 5;
                case "LAST_WEEKDAY":
                    return parts.length == 3
                            && isValidMonth(parts[1])
                            && isValidWeekday(parts[2]);
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Returns the date of the rule in a specific year, or null when the rule has no occurrence that year
    public static LocalDate resolve(String rule, int year) {
        if (!isValid(rule)) {
            return null;
        }

        String[] parts = rule.trim().toUpperCase(Locale.ROOT).split(":");
        try {
            switch (parts[0]) {
                case "FIXED": {
                    String[] date = parts[1].split("-");
                    int month = Integer.parseInt(date[0]);
                    int day = Integer.parseInt(date[1]);
                    return LocalDate.of(year, month, day);
                }
                case "NTH_WEEKDAY": {
                    int month = Integer.parseInt(parts[1]);
                    DayOfWeek weekday = DayOfWeek.valueOf(parts[2]);
                    int nth = Integer.parseInt(parts[3]);
                    LocalDate first = LocalDate.of(year, month, 1);
                    int offset = Math.floorMod(weekday.getValue() - first.getDayOfWeek().getValue(), 7);
                    LocalDate date = first.plusDays(offset + 7L * (nth - 1));
                    return date.getMonthValue() == month ? date : null;
                }
                case "LAST_WEEKDAY": {
                    int month = Integer.parseInt(parts[1]);
                    DayOfWeek weekday = DayOfWeek.valueOf(parts[2]);
                    LocalDate last = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth());
                    int back = Math.floorMod(last.getDayOfWeek().getValue() - weekday.getValue(), 7);
                    return last.minusDays(back);
                }
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    // The next time the rule happens on or after today.
    // Look a few years ahead because a 5th weekday or February 29 can skip a year.
    public static LocalDate nextDate(String rule, LocalDate today) {
        for (int year = today.getYear(); year <= today.getYear() + 8; year++) {
            LocalDate date = resolve(rule, year);
            if (date != null && !date.isBefore(today)) {
                return date;
            }
        }
        return null;
    }

    private static boolean isValidMonth(String value) {
        int month = Integer.parseInt(value);
        return month >= 1 && month <= 12;
    }

    private static boolean isValidDay(int month, int day) {
        if (day < 1) {
            return false;
        }
        // Use a leap year so a FIXED:02-29 rule is accepted
        return day <= Month.of(month).length(true);
    }

    private static boolean isValidWeekday(String value) {
        DayOfWeek.valueOf(value);
        return true;
    }
}

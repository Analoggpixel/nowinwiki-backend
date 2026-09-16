package com.analoggpixel.nowinwiki.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class SyncTimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static LocalDateTime parse(String value) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("invalid updatedAt: " + value);
        }
    }

    public static String format(LocalDateTime value) {
        return value.format(FORMATTER);
    }

    public static String nowString() {
        return format(LocalDateTime.now());
    }

    private SyncTimeUtils() {
    }
}

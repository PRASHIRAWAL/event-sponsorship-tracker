package com.eventtracker.repository;

import java.util.Locale;

/** Builds case-insensitive "contains" patterns for SQL LIKE searches. */
final class SearchPatterns {

    static final char ESCAPE = '\\';

    private SearchPatterns() {
    }

    static boolean isBlank(String keyword) {
        return keyword == null || keyword.isBlank();
    }

    /** Escapes LIKE wildcards so characters such as % and _ in the keyword are matched literally. */
    static String contains(String keyword) {
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}

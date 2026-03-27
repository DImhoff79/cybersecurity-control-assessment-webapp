package com.cyberassessment.util;

/**
 * Display names from first/last with fallbacks for legacy {@code displayName} and email.
 */
public final class UserNameFormatting {

    private UserNameFormatting() {}

    public static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static String formatDisplayName(String firstName, String lastName) {
        String f = blankToNull(firstName);
        String l = blankToNull(lastName);
        if (f == null && l == null) {
            return null;
        }
        if (f == null) {
            return l;
        }
        if (l == null) {
            return f;
        }
        return f + " " + l;
    }

    /**
     * Split "First Last" or "First Middle Last" into [first, remainder] for seeding.
     */
    public static String[] splitLegacyDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return new String[] { null, null };
        }
        String t = displayName.trim();
        int sp = t.indexOf(' ');
        if (sp < 0) {
            return new String[] { t, null };
        }
        return new String[] { t.substring(0, sp), t.substring(sp + 1).trim() };
    }

    public static String publicDisplayName(String firstName, String lastName, String displayName, String email) {
        String fromParts = formatDisplayName(firstName, lastName);
        if (fromParts != null) {
            return fromParts;
        }
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        return email != null ? email : "";
    }
}

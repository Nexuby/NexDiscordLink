package net.nex.discordlink.utils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DoctorChecks {

    private static final Pattern DISCORD_ID = Pattern.compile("\\d{17,20}");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_]+)}");

    private DoctorChecks() {
    }

    public static boolean isDiscordId(String value) {
        return value != null && DISCORD_ID.matcher(value.trim()).matches();
    }

    public static boolean looksLikeExampleDiscordId(String value) {
        if (value == null) return false;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("YOUR_") || normalized.startsWith("123456");
    }

    public static String replacePlaceholdersForValidation(String value) {
        return value == null ? "" : PLACEHOLDER.matcher(value).replaceAll("sample");
    }

    public static Set<String> placeholders(Object value) {
        Set<String> result = new LinkedHashSet<>();
        if (value instanceof String string) {
            collectPlaceholders(string, result);
        } else if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof String string) collectPlaceholders(string, result);
            }
        }
        return result;
    }

    public static String valueKind(Object value) {
        if (value instanceof Collection<?>) return "list";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Number) return "number";
        if (value instanceof String) return "text";
        return value == null ? "missing" : "section";
    }

    public static String examples(Collection<String> paths, int limit) {
        List<String> sorted = paths.stream().sorted().limit(limit).toList();
        return String.join(", ", sorted);
    }

    private static void collectPlaceholders(String value, Set<String> result) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) result.add(matcher.group(1));
    }
}

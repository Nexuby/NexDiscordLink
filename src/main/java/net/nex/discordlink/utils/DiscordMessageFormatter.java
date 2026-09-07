package net.nex.discordlink.utils;

import java.awt.Color;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class DiscordMessageFormatter {

    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private DiscordMessageFormatter() {
    }

    public static String format(String value, Map<String, String> placeholders, int maxLength) {
        if (value == null) return "";

        String result = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        result = LEGACY_COLOR.matcher(result).replaceAll("");
        return truncate(result, maxLength);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        if (maxLength <= 1) return value.substring(0, Math.max(0, maxLength));
        return value.substring(0, maxLength - 1) + "…";
    }

    public static Color parseColor(String value, Color fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "CYAN" -> Color.CYAN;
            case "DARK_GRAY", "DARK_GREY" -> Color.DARK_GRAY;
            case "GRAY", "GREY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LIGHT_GRAY", "LIGHT_GREY" -> Color.LIGHT_GRAY;
            case "MAGENTA", "PURPLE" -> Color.MAGENTA;
            case "ORANGE" -> Color.ORANGE;
            case "PINK" -> Color.PINK;
            case "RED" -> Color.RED;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            default -> parseHexOrRgb(normalized, fallback);
        };
    }

    public static boolean isValidColor(String value) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("#?[0-9A-F]{6}")) return true;
        if (normalized.matches("\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}")) {
            String[] parts = normalized.split(",");
            try {
                return Integer.parseInt(parts[0].trim()) <= 255
                        && Integer.parseInt(parts[1].trim()) <= 255
                        && Integer.parseInt(parts[2].trim()) <= 255;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return switch (normalized) {
            case "BLACK", "BLUE", "CYAN", "DARK_GRAY", "DARK_GREY", "GRAY", "GREY", "GREEN",
                    "LIGHT_GRAY", "LIGHT_GREY", "MAGENTA", "PURPLE", "ORANGE", "PINK", "RED", "WHITE", "YELLOW" -> true;
            default -> false;
        };
    }

    private static Color parseHexOrRgb(String value, Color fallback) {
        try {
            if (value.matches("#?[0-9A-F]{6}")) {
                String hex = value.startsWith("#") ? value.substring(1) : value;
                return new Color(Integer.parseInt(hex, 16));
            }
            if (value.matches("\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}")) {
                String[] parts = value.split(",");
                int red = Integer.parseInt(parts[0].trim());
                int green = Integer.parseInt(parts[1].trim());
                int blue = Integer.parseInt(parts[2].trim());
                if (red <= 255 && green <= 255 && blue <= 255) return new Color(red, green, blue);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid configuration falls back to the event's built-in color.
        }
        return fallback;
    }

    public static String safeHttpUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) return null;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return null;
            return uri.toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

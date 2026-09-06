package net.nex.discordlink.utils;

import java.util.regex.Pattern;

public final class AuditLogSanitizer {

    private static final Pattern NAMED_SECRET = Pattern.compile(
            "(?i)\\b(secret|token|key|ip)\\s*[=:]\\s*\\S+"
    );
    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\d.])(?:\\d{1,3}\\.){3}\\d{1,3}(?![\\d.])"
    );

    private AuditLogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) return "-";
        String redacted = NAMED_SECRET.matcher(value).replaceAll("$1=[REDACTED]");
        return IPV4.matcher(redacted).replaceAll("[REDACTED_IP]");
    }
}

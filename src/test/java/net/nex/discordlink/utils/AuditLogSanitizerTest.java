package net.nex.discordlink.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogSanitizerTest {

    @Test
    void redactsNamedSecretsAndIpv4Addresses() {
        String sanitized = AuditLogSanitizer.sanitize(
                "ip=203.0.113.42 token=abc123 secret:xyz player=Steve"
        );

        assertFalse(sanitized.contains("203.0.113.42"));
        assertFalse(sanitized.contains("abc123"));
        assertFalse(sanitized.contains("xyz"));
        assertTrue(sanitized.contains("player=Steve"));
    }
}

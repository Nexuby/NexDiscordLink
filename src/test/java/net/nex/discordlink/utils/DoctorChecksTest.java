package net.nex.discordlink.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorChecksTest {

    @Test
    void validatesDiscordSnowflakesAndRecognizesExamples() {
        assertTrue(DoctorChecks.isDiscordId("123456789012345678"));
        assertFalse(DoctorChecks.isDiscordId("YOUR_CHANNEL_ID"));
        assertFalse(DoctorChecks.isDiscordId("123"));
        assertTrue(DoctorChecks.looksLikeExampleDiscordId("123456789012345678"));
        assertTrue(DoctorChecks.looksLikeExampleDiscordId("YOUR_ROLE_ID"));
        assertFalse(DoctorChecks.looksLikeExampleDiscordId("987654321098765432"));
    }

    @Test
    void extractsPlaceholdersFromTextAndLists() {
        assertEquals(Set.of("player", "message"), DoctorChecks.placeholders("{player}: {message}"));
        assertEquals(Set.of("player", "count"), DoctorChecks.placeholders(List.of("{player}", "{count}")));
    }

    @Test
    void replacesTemplatePlaceholdersBeforeUrlValidation() {
        assertEquals(
                "https://mc-heads.net/avatar/sample",
                DoctorChecks.replacePlaceholdersForValidation("https://mc-heads.net/avatar/{player}")
        );
    }

    @Test
    void classifiesYamlValueKinds() {
        assertEquals("text", DoctorChecks.valueKind("value"));
        assertEquals("number", DoctorChecks.valueKind(5));
        assertEquals("boolean", DoctorChecks.valueKind(true));
        assertEquals("list", DoctorChecks.valueKind(List.of("value")));
    }
}

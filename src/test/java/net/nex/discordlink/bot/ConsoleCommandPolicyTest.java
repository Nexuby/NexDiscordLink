package net.nex.discordlink.bot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleCommandPolicyTest {

    @Test
    void parsesAndNormalizesSafeCommands() {
        ConsoleCommandPolicy.ParsedCommand parsed = ConsoleCommandPolicy.parse("  TPS compact  ").orElseThrow();

        assertEquals("TPS compact", parsed.command());
        assertEquals("tps", parsed.commandName());
    }

    @Test
    void rejectsSlashPrefixedMultilineAndEmptyCommands() {
        assertTrue(ConsoleCommandPolicy.parse("/stop").isEmpty());
        assertTrue(ConsoleCommandPolicy.parse("say hello\nstop").isEmpty());
        assertTrue(ConsoleCommandPolicy.parse("   ").isEmpty());
    }

    @Test
    void requiresAnExplicitWhitelistMatch() {
        assertTrue(ConsoleCommandPolicy.isAllowed("list", List.of("say", " LIST ")));
        assertFalse(ConsoleCommandPolicy.isAllowed("stop", List.of("say", "list")));
        assertFalse(ConsoleCommandPolicy.isAllowed("list", List.of()));
    }
}

package net.nex.discordlink.utils;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DiscordMessageFormatterTest {

    @Test
    void replacesPlaceholdersAndRemovesMinecraftFormatting() {
        assertEquals(
                "Player Muro joined",
                DiscordMessageFormatter.format("§aPlayer {player} joined", Map.of("player", "Muro"), 100)
        );
    }

    @Test
    void truncatesToDiscordLimit() {
        assertEquals("1234…", DiscordMessageFormatter.format("123456", Map.of(), 5));
    }

    @Test
    void parsesNamedHexAndRgbColors() {
        assertEquals(Color.CYAN, DiscordMessageFormatter.parseColor("cyan", Color.RED));
        assertEquals(new Color(47, 128, 237), DiscordMessageFormatter.parseColor("#2F80ED", Color.RED));
        assertEquals(new Color(47, 128, 237), DiscordMessageFormatter.parseColor("47, 128, 237", Color.RED));
    }

    @Test
    void invalidColorFallsBack() {
        assertEquals(Color.ORANGE, DiscordMessageFormatter.parseColor("#not-a-color", Color.ORANGE));
        assertEquals(Color.ORANGE, DiscordMessageFormatter.parseColor("300, 20, 20", Color.ORANGE));
    }

    @Test
    void acceptsOnlyHttpImageUrls() {
        assertEquals("https://example.com/image.png", DiscordMessageFormatter.safeHttpUrl("https://example.com/image.png"));
        assertNull(DiscordMessageFormatter.safeHttpUrl("file:///secret.png"));
        assertNull(DiscordMessageFormatter.safeHttpUrl("not a url"));
    }
}

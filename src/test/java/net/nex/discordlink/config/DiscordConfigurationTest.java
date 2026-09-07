package net.nex.discordlink.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordConfigurationTest {

    @Test
    void bundledConfigurationContainsEveryDiscordMessageTemplate() {
        try (var stream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(stream);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );

            assertTrue(config.getBoolean("discord-messages.events.join.enabled"));
            assertTrue(config.getBoolean("discord-messages.events.quit.enabled"));
            assertTrue(config.getBoolean("discord-messages.events.death.enabled"));
            assertTrue(config.getBoolean("discord-messages.events.advancement.enabled"));
            assertEquals("SUCCESS", config.getString("discord-messages.security-login.button-style"));
            assertEquals("DANGER", config.getString("discord-messages.unlink-confirmation.button-style"));
            assertNotNull(config.getConfigurationSection("discord-messages.audit.event-colors"));
            assertEquals("**{player}**: {message}", config.getString("discord-messages.chat.minecraft-to-discord-format"));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}

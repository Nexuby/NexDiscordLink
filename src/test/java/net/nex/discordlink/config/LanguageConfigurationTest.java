package net.nex.discordlink.config;

import net.nex.discordlink.utils.DoctorChecks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LanguageConfigurationTest {

    @Test
    void englishAndTurkishLanguagesHaveMatchingKeysTypesAndPlaceholders() {
        YamlConfiguration english = load("lang/messages_en.yml");
        YamlConfiguration turkish = load("lang/messages_tr.yml");
        Set<String> englishKeys = leafKeys(english);
        Set<String> turkishKeys = leafKeys(turkish);

        assertEquals(englishKeys, turkishKeys, "Language keys must remain in parity");
        for (String key : englishKeys) {
            assertEquals(
                    DoctorChecks.valueKind(english.get(key)),
                    DoctorChecks.valueKind(turkish.get(key)),
                    "Value type differs for " + key
            );
            assertEquals(
                    DoctorChecks.placeholders(english.get(key)),
                    DoctorChecks.placeholders(turkish.get(key)),
                    "Placeholders differ for " + key
            );
        }
    }

    private YamlConfiguration load(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " is missing");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private Set<String> leafKeys(ConfigurationSection configuration) {
        Set<String> keys = new LinkedHashSet<>();
        for (String key : configuration.getKeys(true)) {
            if (!configuration.isConfigurationSection(key)) keys.add(key);
        }
        return keys;
    }
}

package net.nex.discordlink.config;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final NexDiscordLink plugin;
    private FileConfiguration config;

    public ConfigManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getLanguage() {
        Object value = config.get("settings.language");
        return value instanceof String language && !language.isBlank() ? language : "en";
    }

    public String getBotToken() {
        return config.getString("bot-token", "");
    }
}

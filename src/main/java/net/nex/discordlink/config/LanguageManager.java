package net.nex.discordlink.config;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {
    private final NexDiscordLink plugin;
    private YamlConfiguration langConfig;
    private YamlConfiguration fallbackConfig;
    private String prefix;

    public LanguageManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    public void loadLanguages() {
        String lang = plugin.getConfigManager().getLanguage();

        // Always save default language files (won't overwrite if exists)
        plugin.saveResource("lang/messages_en.yml", false);
        plugin.saveResource("lang/messages_tr.yml", false);

        File langFile = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");

        if (!langFile.exists()) {
            // Fallback to english if selected language file doesn't exist
            langFile = new File(plugin.getDataFolder(), "lang/messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load fallback (en) if selected lang is not en
        if (!lang.equalsIgnoreCase("en")) {
            File fallbackFile = new File(plugin.getDataFolder(), "lang/messages_en.yml");
            if (fallbackFile.exists()) {
                fallbackConfig = YamlConfiguration.loadConfiguration(fallbackFile);
            }
        }

        prefix = colorize(langConfig.getString("prefix", "&8[&bNexDiscordLink&8] &7"));
    }

    public String getMessage(String key) {
        String msg = langConfig.getString(key);
        if (msg == null && fallbackConfig != null) {
            msg = fallbackConfig.getString(key);
        }
        if (msg == null) return "Missing key: " + key;
        return colorize(msg);
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return msg;
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        boolean isList = langConfig.isList(key) || (fallbackConfig != null && fallbackConfig.isList(key));
        if (isList) {
            java.util.List<String> lines = langConfig.getStringList(key);
            if (lines.isEmpty() && fallbackConfig != null) {
                lines = fallbackConfig.getStringList(key);
            }
            for (String line : lines) {
                String msg = line;
                for (int i = 0; i < placeholders.length; i += 2) {
                    if (i + 1 < placeholders.length) {
                        msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
                    }
                }
                sender.sendMessage(colorize(msg));
            }
        } else {
            sender.sendMessage(prefix + getMessage(key, placeholders));
        }
    }

    public void sendConsoleMessage(String key, String... placeholders) {
        plugin.getServer().getConsoleSender().sendMessage(prefix + getMessage(key, placeholders));
    }

    private String colorize(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            // Convert hex to Spigot format &x&R&R&G&G&B&B
            StringBuilder builder = new StringBuilder("§x");
            for (char c : hexCode.substring(1).toCharArray()) {
                builder.append("§").append(c);
            }
            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

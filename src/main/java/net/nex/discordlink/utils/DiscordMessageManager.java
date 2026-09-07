package net.nex.discordlink.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.Color;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DiscordMessageManager {

    private static final int TITLE_LIMIT = 256;
    private static final int DESCRIPTION_LIMIT = 4096;
    private static final int FOOTER_LIMIT = 2048;
    private static final int AUTHOR_LIMIT = 256;
    private static final String ROOT = "discord-messages.";

    private final NexDiscordLink plugin;

    public DiscordMessageManager(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(String template, boolean fallback) {
        return plugin.getConfig().getBoolean(ROOT + template + ".enabled", fallback);
    }

    public String getChannelId(String template, String fallbackConfigPath) {
        String channelId = plugin.getConfig().getString(ROOT + template + ".channel-id", "").trim();
        if (channelId.isEmpty() && fallbackConfigPath != null) {
            channelId = plugin.getConfig().getString(fallbackConfigPath, "").trim();
        }
        return channelId;
    }

    public EmbedBuilder createEmbed(
            String template,
            String fallbackTitleKey,
            String fallbackDescriptionKey,
            Color fallbackColor,
            String playerName,
            UUID playerUuid,
            String... placeholders
    ) {
        Map<String, String> values = placeholders(playerName, playerUuid, placeholders);
        EmbedBuilder embed = new EmbedBuilder();

        String title = resolveText(template, "title", fallbackTitleKey, TITLE_LIMIT, values);
        String description = resolveText(template, "description", fallbackDescriptionKey, DESCRIPTION_LIMIT, values);
        if (!title.isBlank()) embed.setTitle(title);
        if (!description.isBlank()) embed.setDescription(description);

        applyStyle(embed, template, fallbackColor, playerName, values);
        return embed;
    }

    public String resolveText(
            String template,
            String property,
            String fallbackLanguageKey,
            int limit,
            String... placeholders
    ) {
        return resolveText(template, property, fallbackLanguageKey, limit, placeholders(null, null, placeholders));
    }

    public String format(String value, int limit, String... placeholders) {
        return DiscordMessageFormatter.format(value, placeholders(null, null, placeholders), limit);
    }

    public Color resolveColor(String configPath, Color fallback) {
        return DiscordMessageFormatter.parseColor(plugin.getConfig().getString(configPath), fallback);
    }

    public ButtonStyle resolveButtonStyle(String template, ButtonStyle fallback) {
        String configured = plugin.getConfig().getString(ROOT + template + ".button-style", "").trim();
        if (configured.isEmpty()) return fallback;
        try {
            return ButtonStyle.valueOf(configured.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public void setFooter(
            EmbedBuilder embed,
            String template,
            String fallbackLanguageKey,
            String... placeholders
    ) {
        String path = ROOT + template;
        String defaults = ROOT + "defaults";
        if (!inheritedBoolean(path + ".footer.enabled", defaults + ".footer.enabled", false)) return;

        Map<String, String> values = placeholders(null, null, placeholders);
        String configured = inheritedString(path + ".footer.text", defaults + ".footer.text");
        String value = configured.isBlank() && fallbackLanguageKey != null
                ? plugin.getLanguageManager().getMessage(fallbackLanguageKey)
                : configured;
        String text = DiscordMessageFormatter.format(value, values, FOOTER_LIMIT);
        String icon = formattedUrl(inheritedString(path + ".footer.icon-url", defaults + ".footer.icon-url"), values);
        if (!text.isBlank()) embed.setFooter(text, icon);
    }

    private String resolveText(
            String template,
            String property,
            String fallbackLanguageKey,
            int limit,
            Map<String, String> values
    ) {
        String configured = plugin.getConfig().getString(ROOT + template + "." + property, "");
        String value = configured == null || configured.isBlank()
                ? fallbackLanguageKey == null ? "" : plugin.getLanguageManager().getMessage(fallbackLanguageKey)
                : configured;
        return DiscordMessageFormatter.format(value, values, limit);
    }

    private void applyStyle(
            EmbedBuilder embed,
            String template,
            Color fallbackColor,
            String playerName,
            Map<String, String> values
    ) {
        String path = ROOT + template;
        String defaults = ROOT + "defaults";

        String colorValue = inheritedString(path + ".color", defaults + ".color");
        embed.setColor(DiscordMessageFormatter.parseColor(colorValue, fallbackColor));

        if (inheritedBoolean(path + ".timestamp", defaults + ".timestamp", true)) {
            embed.setTimestamp(Instant.now());
        }

        if (inheritedBoolean(path + ".author.enabled", defaults + ".author.enabled", playerName != null)) {
            String authorText = inheritedString(path + ".author.text", defaults + ".author.text");
            if (authorText.isBlank()) authorText = "{player}";
            authorText = DiscordMessageFormatter.format(authorText, values, AUTHOR_LIMIT);
            String authorUrl = formattedUrl(inheritedString(path + ".author.url", defaults + ".author.url"), values);
            String authorIcon = formattedUrl(inheritedString(path + ".author.icon-url", defaults + ".author.icon-url"), values);
            if (!authorText.isBlank()) embed.setAuthor(authorText, authorUrl, authorIcon);
        }

        if (inheritedBoolean(path + ".thumbnail.enabled", defaults + ".thumbnail.enabled", playerName != null)) {
            String thumbnail = inheritedString(path + ".thumbnail.url", defaults + ".thumbnail.url");
            if (thumbnail.isBlank() && playerName != null) thumbnail = "https://mc-heads.net/avatar/{player}";
            thumbnail = formattedUrl(thumbnail, values);
            if (thumbnail != null) embed.setThumbnail(thumbnail);
        }

        if (inheritedBoolean(path + ".image.enabled", defaults + ".image.enabled", false)) {
            String image = formattedUrl(inheritedString(path + ".image.url", defaults + ".image.url"), values);
            if (image != null) embed.setImage(image);
        }

        if (inheritedBoolean(path + ".footer.enabled", defaults + ".footer.enabled", false)) {
            String footerText = DiscordMessageFormatter.format(
                    inheritedString(path + ".footer.text", defaults + ".footer.text"), values, FOOTER_LIMIT
            );
            String footerIcon = formattedUrl(
                    inheritedString(path + ".footer.icon-url", defaults + ".footer.icon-url"), values
            );
            if (!footerText.isBlank()) embed.setFooter(footerText, footerIcon);
        }
    }

    private boolean inheritedBoolean(String path, String defaultPath, boolean fallback) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains(path)) return config.getBoolean(path);
        if (config.contains(defaultPath)) return config.getBoolean(defaultPath);
        return fallback;
    }

    private String inheritedString(String path, String defaultPath) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains(path)) return config.getString(path, "").trim();
        return config.getString(defaultPath, "").trim();
    }

    private String formattedUrl(String value, Map<String, String> placeholders) {
        String formatted = DiscordMessageFormatter.format(value, placeholders, 2048);
        return DiscordMessageFormatter.safeHttpUrl(formatted);
    }

    private Map<String, String> placeholders(String playerName, UUID playerUuid, String... placeholders) {
        Map<String, String> values = new LinkedHashMap<>();
        if (playerName != null) values.put("player", playerName);
        if (playerUuid != null) values.put("uuid", playerUuid.toString());
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            values.put(placeholders[index], placeholders[index + 1]);
        }
        return values;
    }
}

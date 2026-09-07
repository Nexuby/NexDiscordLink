package net.nex.discordlink.listeners;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class ChatListener implements Listener {

    private static final int HTTP_TIMEOUT_MILLIS = 5000;

    private final NexDiscordLink plugin;

    public ChatListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-bridge.enabled", false)) return;

        String channelId = plugin.getConfig().getString("chat-bridge.channel-id");
        String webhookUrl = plugin.getConfig().getString("chat-bridge.webhook-url");
        String message = org.bukkit.ChatColor.stripColor(event.getMessage());
        String playerName = event.getPlayer().getName();
        String content = plugin.getDiscordMessageManager().format(
                plugin.getConfig().getString("discord-messages.chat.minecraft-to-discord-format", "**{player}**: {message}"),
                2000,
                "player", playerName,
                "message", message
        );

        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(
                    plugin,
                    () -> sendWebhook(webhookUrl, playerName, content)
            );
        } else if (channelId != null && !channelId.isEmpty()) {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) return;
            TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(content)
                        .setAllowedMentions(Collections.emptyList())
                        .queue();
            }
        }
    }

    private void sendWebhook(String webhookUrl, String username, String content) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);
            connection.setDoOutput(true);

            String webhookUsername = plugin.getDiscordMessageManager().format(
                    plugin.getConfig().getString("discord-messages.chat.webhook-username", "{player}"),
                    80,
                    "player", username
            );
            String avatarUrl = plugin.getDiscordMessageManager().format(
                    plugin.getConfig().getString("discord-messages.chat.webhook-avatar-url", "https://mc-heads.net/avatar/{player}"),
                    2048,
                    "player", username
            );
            // Properly escape JSON strings to prevent injection
            String safeUsername = escapeJson(webhookUsername);
            String safeContent = escapeJson(content);
            String safeAvatarUrl = escapeJson(avatarUrl);
            String json = String.format("{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                    safeUsername, safeAvatarUrl, safeContent);
            json = json.substring(0, json.length() - 1) + ", \"allowed_mentions\": {\"parse\": []}}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Discord chat webhook returned HTTP " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send Discord chat webhook: " + e.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}

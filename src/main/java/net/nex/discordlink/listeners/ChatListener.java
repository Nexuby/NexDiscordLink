package net.nex.discordlink.listeners;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatListener implements Listener {

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

        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            sendWebhook(webhookUrl, playerName, message);
        } else if (channelId != null && !channelId.isEmpty()) {
            if (plugin.getDiscordBot().getJda() == null) return;
            TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("**" + playerName + "**: " + message).queue();
            }
        }
    }

    private void sendWebhook(String webhookUrl, String username, String content) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String avatarUrl = "https://mc-heads.net/avatar/" + username;
            // Properly escape JSON strings to prevent injection
            String safeUsername = escapeJson(username);
            String safeContent = escapeJson(content);
            String safeAvatarUrl = escapeJson(avatarUrl);
            String json = String.format("{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                    safeUsername, safeAvatarUrl, safeContent);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            connection.getResponseCode(); // Trigger request
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
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

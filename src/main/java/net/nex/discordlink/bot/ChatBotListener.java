package net.nex.discordlink.bot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ChatBotListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public ChatBotListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!plugin.getConfig().getBoolean("chat-bridge.enabled", false)) return;

        String channelId = plugin.getConfig().getString("chat-bridge.channel-id");
        if (channelId == null || !channelId.equals(event.getChannel().getId())) return;

        if (event.getAuthor().isBot()) return;

        String format = plugin.getConfig().getString("chat-bridge.discord-to-game-format", "&8[&bDiscord&8] &f{user}: &7{message}");
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', format)
                .replace("{user}", sanitizeGameText(event.getAuthor().getName(), 64))
                .replace("{message}", sanitizeGameText(event.getMessage().getContentDisplay(), 256));
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(formattedMessage));
    }

    private String sanitizeGameText(String value, int maxLength) {
        String sanitized = value
                .replace("§", "")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replace("\r", " ")
                .replace("\n", " ");
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
    }
}

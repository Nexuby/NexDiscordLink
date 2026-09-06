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
        String message = format
                .replace("{user}", event.getAuthor().getName())
                .replace("{message}", event.getMessage().getContentDisplay());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}

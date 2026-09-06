package net.nex.discordlink.listeners;

import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.utils.EmbedUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.Color;

public class GameEventsListener implements Listener {

    private final NexDiscordLink plugin;
    private final EmbedUtils embedUtils;

    public GameEventsListener(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.embedUtils = new EmbedUtils(plugin);
    }

    private void sendEmbed(String channelId, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendEmbed(channelId, embed);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String channelId = plugin.getConfig().getString("channels.log-channel-id");
        if (channelId == null) return;

        sendEmbed(channelId, embedUtils.createEmbed(
                event.getPlayer(),
                "events.join_title",
                "events.join_desc",
                Color.GREEN,
                "player", event.getPlayer().getName()
        ));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String channelId = plugin.getConfig().getString("channels.log-channel-id");
        if (channelId == null) return;

        sendEmbed(channelId, embedUtils.createEmbed(
                event.getPlayer(),
                "events.quit_title",
                "events.quit_desc",
                Color.RED,
                "player", event.getPlayer().getName()
        ));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        String channelId = plugin.getConfig().getString("channels.log-channel-id");
        if (channelId == null) return;

        String deathMessage = event.getDeathMessage();
        if (deathMessage == null) deathMessage = event.getEntity().getName() + " died";

        sendEmbed(channelId, embedUtils.createEmbed(
                event.getEntity(),
                "events.death_title",
                "events.death_desc",
                Color.BLACK,
                "death_message", deathMessage
        ));
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String channelId = plugin.getConfig().getString("channels.log-channel-id");
        if (channelId == null) return;

        // Filter out recipes
        if (event.getAdvancement().getKey().getKey().startsWith("recipes/")) return;

        String advancementName = event.getAdvancement().getKey().getKey(); // Simplified name
        // Ideally we get the display name, but that requires NMS or more complex API usage
        // For now, using key

        sendEmbed(channelId, embedUtils.createEmbed(
                event.getPlayer(),
                "events.advancement_title",
                "events.advancement_desc",
                Color.YELLOW,
                "player", event.getPlayer().getName(),
                "advancement", advancementName
        ));
    }
}

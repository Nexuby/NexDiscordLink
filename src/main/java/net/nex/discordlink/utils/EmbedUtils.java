package net.nex.discordlink.utils;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.entity.Player;

import java.awt.Color;
public class EmbedUtils {

    private final NexDiscordLink plugin;

    public EmbedUtils(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public MessageEmbed createEmbed(
            String template,
            Player player,
            String titleKey,
            String descKey,
            Color color,
            String... placeholders
    ) {
        return plugin.getDiscordMessageManager().createEmbed(
                template,
                titleKey,
                descKey,
                color,
                player == null ? null : player.getName(),
                player == null ? null : player.getUniqueId(),
                placeholders
        ).build();
    }
}

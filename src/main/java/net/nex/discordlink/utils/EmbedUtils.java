package net.nex.discordlink.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.Instant;

public class EmbedUtils {

    private final NexDiscordLink plugin;

    public EmbedUtils(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public MessageEmbed createEmbed(Player player, String titleKey, String descKey, Color color, String... placeholders) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(plugin.getLanguageManager().getMessage(titleKey));
        embed.setDescription(plugin.getLanguageManager().getMessage(descKey, placeholders));
        embed.setColor(color);
        embed.setTimestamp(Instant.now());

        if (player != null) {
            String avatarUrl = "https://mc-heads.net/avatar/" + player.getName();
            embed.setThumbnail(avatarUrl);
            embed.setAuthor(player.getName(), null, avatarUrl);
        }

        return embed.build();
    }
}

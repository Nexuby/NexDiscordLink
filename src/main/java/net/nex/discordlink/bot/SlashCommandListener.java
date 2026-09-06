package net.nex.discordlink.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.Color;
import java.time.Instant;
import java.util.UUID;

public class SlashCommandListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public SlashCommandListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("profile")) {
            if (event.getGuild() == null) {
                event.reply(plugin.getLanguageManager().getMessage("discord.command.no_dm")).setEphemeral(true).queue();
                return;
            }
            handleProfileCommand(event);
        }
    }

    private void handleProfileCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String discordId = event.getUser().getId();

        // Run database query asynchronously if possible, but here we are in JDA thread which is async to Bukkit main thread usually.
        // However, Bukkit.getOfflinePlayer might need to be careful.
        // Actually JDA events run on JDA threads. DatabaseManager is thread safe?
        // SQLiteDatabaseManager usually uses HikariCP which is thread safe.

        UUID uuid = plugin.getDatabaseManager().getPlayerUUID(discordId);

        if (uuid == null) {
            event.getHook().sendMessage(plugin.getLanguageManager().getMessage("discord.command.profile.not_linked")).setEphemeral(true).queue();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> sendProfile(event, uuid));
    }

    private void sendProfile(SlashCommandInteractionEvent event, UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String playerName = offlinePlayer.getName();
        if (playerName == null) playerName = "Unknown";

        String status = offlinePlayer.isOnline()
                ? plugin.getLanguageManager().getMessage("discord.command.profile.embed.status_online")
                : plugin.getLanguageManager().getMessage("discord.command.profile.embed.status_offline");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(plugin.getLanguageManager().getMessage("discord.command.profile.embed.title"));
        embed.setColor(Color.CYAN);

        embed.addField(plugin.getLanguageManager().getMessage("discord.command.profile.embed.field_player"), playerName, true);
        embed.addField(plugin.getLanguageManager().getMessage("discord.command.profile.embed.field_uuid"), uuid.toString(), true);
        embed.addField(plugin.getLanguageManager().getMessage("discord.command.profile.embed.field_status"), status, true);

        String avatarUrl = "https://mc-heads.net/avatar/" + playerName;
        embed.setThumbnail(avatarUrl);
        embed.setAuthor(playerName, null, avatarUrl);
        embed.setTimestamp(Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}

package net.nex.discordlink.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
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
        switch (DiscordCommandRouter.resolve(event.getName())) {
            case PROFILE -> handleProfileCommand(event);
            case LINK -> handleLinkCommand(event);
            case UNLINK -> handleUnlinkCommand(event);
            case HELP -> event.reply(plugin.getLanguageManager().getMessage("discord.command.help.content"))
                    .setEphemeral(true)
                    .queue();
            case UNKNOWN -> {
                // Handled by the listener responsible for that command.
            }
        }
    }

    private void handleProfileCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

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

    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("code");
        if (option == null) option = event.getOption("kod");
        if (option == null) {
            event.reply(plugin.getLanguageManager().getMessage("discord.command.link.missing_code"))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String code = option.getAsString().trim();
        event.deferReply(true).queue();
        plugin.getLinkManager().processLinkCode(
                code,
                event.getUser().getId(),
                event.getUser().getName(),
                message -> event.getHook().sendMessage(message).setEphemeral(true).queue()
        );
    }

    private void handleUnlinkCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        String discordId = event.getUser().getId();
        UUID uuid = plugin.getDatabaseManager().getPlayerUUID(discordId);
        if (uuid == null) {
            event.getHook().sendMessage(plugin.getLanguageManager().getMessage("discord.command.profile.not_linked"))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        plugin.getUnlinkManager().performUnlink(uuid, "Discord command", success -> {
            String key = success ? "discord.command.unlink.success" : "discord.command.unlink.failed";
            event.getHook().sendMessage(plugin.getLanguageManager().getMessage(key))
                    .setEphemeral(true)
                    .queue();
        });
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

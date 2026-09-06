package net.nex.discordlink.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class AccountStatusCommand implements CommandExecutor {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final NexDiscordLink plugin;

    public AccountStatusCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLanguageManager().sendMessage(sender, "commands.player_only");
            return true;
        }
        if (!player.hasPermission("nexdiscord.status")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no_permission");
            return true;
        }

        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String discordId = plugin.getDatabaseManager().getDiscordId(uuid);
            if (discordId == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getLanguageManager().sendMessage(player, "commands.unlink_not_linked"));
                return;
            }

            long linkedAt = plugin.getDatabaseManager().getLinkedAt(uuid);
            boolean twoFactorEnabled = plugin.getDatabaseManager().get2FASecret(uuid) != null;
            String discordAccount = resolveDiscordAccount(discordId);
            String linkedDate = linkedAt > 0
                    ? DATE_FORMAT.format(Instant.ofEpochMilli(linkedAt))
                    : plugin.getLanguageManager().getMessage("commands.status_unknown");
            String twoFactor = plugin.getLanguageManager().getMessage(
                    twoFactorEnabled ? "commands.status_enabled" : "commands.status_disabled"
            );

            Bukkit.getScheduler().runTask(plugin, () -> plugin.getLanguageManager().sendMessage(
                    player,
                    "commands.account_status",
                    "discord", discordAccount,
                    "linked_at", linkedDate,
                    "two_factor", twoFactor
            ));
        });
        return true;
    }

    private String resolveDiscordAccount(String discordId) {
        if (plugin.getDiscordBot() == null) return discordId;
        JDA jda = plugin.getDiscordBot().getJda();
        if (jda == null) return discordId;
        User user = jda.getUserById(discordId);
        return user == null ? discordId : user.getName() + " (" + discordId + ")";
    }
}

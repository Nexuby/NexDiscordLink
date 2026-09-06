package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ADMIN_ACTION;

public class ResetRewardCommand implements CommandExecutor {

    private final NexDiscordLink plugin;

    public ResetRewardCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexdiscord.admin")) {
            plugin.getLanguageManager().sendMessage(sender, "commands.no_permission");
            return true;
        }

        if (args.length < 1) {
            plugin.getLanguageManager().sendMessage(sender, "commands.reset_reward_usage");
            return true;
        }

        String target = args[0];

        if (target.equalsIgnoreCase("all")) {
            plugin.getDatabaseManager().resetAllLinkRewardCounts();
            plugin.getAuditLogger().log(ADMIN_ACTION, sender.getName(), "Reset link rewards: all players");
            plugin.getLanguageManager().sendMessage(sender, "commands.reset_reward_all_success");
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(target);
            if (player.hasPlayedBefore() || player.isOnline()) {
                plugin.getDatabaseManager().resetLinkRewardCount(player.getUniqueId());
                plugin.getAuditLogger().log(ADMIN_ACTION, sender.getName(), "Reset link rewards: " + player.getName());
                plugin.getLanguageManager().sendMessage(sender, "commands.reset_reward_success", "player", player.getName());
            } else {
                plugin.getLanguageManager().sendMessage(sender, "commands.player_not_found");
            }
        }

        return true;
    }
}

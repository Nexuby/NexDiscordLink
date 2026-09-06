package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ADMIN_ACTION;

public class SyncRolesCommand {

    private final NexDiscordLink plugin;

    public SyncRolesCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexdiscord.admin")) {
            plugin.getLanguageManager().sendMessage(sender, "commands.no_permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("tümü")) {
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getRoleManager().syncPlayerRole(player);
                count++;
            }
            plugin.getAuditLogger().log(ADMIN_ACTION, sender.getName(), "Manual role sync: " + count + " players");
            plugin.getLanguageManager().sendMessage(sender, "commands.role_sync_all", "count", Integer.toString(count));
            return true;
        }

        Player player = Bukkit.getPlayerExact(args[0]);
        if (player == null) {
            plugin.getLanguageManager().sendMessage(sender, "commands.player_not_found");
            return true;
        }

        plugin.getRoleManager().syncPlayerRole(player);
        plugin.getAuditLogger().log(ADMIN_ACTION, sender.getName(), "Manual role sync: " + player.getName());
        plugin.getLanguageManager().sendMessage(sender, "commands.role_sync_player", "player", player.getName());
        return true;
    }
}

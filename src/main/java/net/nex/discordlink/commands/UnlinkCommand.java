package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ACCOUNT_UNLINKED;

public class UnlinkCommand implements CommandExecutor {

    private final NexDiscordLink plugin;

    public UnlinkCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().sendMessage(sender, "commands.player_only");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nexdiscord.unlink")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no_permission");
            return true;
        }

        if (!plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "commands.unlink_not_linked");
            return true;
        }

        String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
        if (discordId != null) {
            plugin.getRoleManager().removePlayerRoles(discordId);
        }

        plugin.getDatabaseManager().removePlayer(player.getUniqueId());
        plugin.refreshPlaceholders(player.getUniqueId());
        plugin.getAuditLogger().log(ACCOUNT_UNLINKED, player.getName(), "Minecraft command");
        plugin.getLanguageManager().sendMessage(player, "commands.unlink_success");

        return true;
    }
}

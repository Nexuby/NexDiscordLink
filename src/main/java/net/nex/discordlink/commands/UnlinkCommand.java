package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        if (!plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "commands.unlink_not_linked");
            return true;
        }

        String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
        if (discordId != null) {
            plugin.getRoleManager().removePlayerRoles(discordId);
        }

        plugin.getDatabaseManager().removePlayer(player.getUniqueId());
        plugin.getLanguageManager().sendMessage(player, "commands.unlink_success");

        return true;
    }
}

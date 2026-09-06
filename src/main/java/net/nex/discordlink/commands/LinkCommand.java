package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    private final NexDiscordLink plugin;

    public LinkCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().sendMessage(sender, "commands.player_only");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nexdiscord.link")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no_permission");
            return true;
        }

        if (plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "commands.link_already_linked");
            return true;
        }

        String code = plugin.getLinkManager().generateCode(player.getUniqueId());
        String messageKey;
        if (plugin.getLinkManager().isDmEnabled() && plugin.getLinkManager().isModalEnabled()) {
            messageKey = "commands.link_code_generated_both";
        } else if (plugin.getLinkManager().isModalEnabled()) {
            messageKey = "commands.link_code_generated_modal";
        } else {
            messageKey = "commands.link_code_generated_dm";
        }
        plugin.getLanguageManager().sendMessage(player, messageKey, "code", code);

        return true;
    }
}

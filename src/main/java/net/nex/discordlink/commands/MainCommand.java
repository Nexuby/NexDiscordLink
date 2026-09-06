package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MainCommand implements CommandExecutor {

    private final NexDiscordLink plugin;
    private final ResetRewardCommand resetRewardCommand;

    public MainCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.resetRewardCommand = new ResetRewardCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("nexdiscord.reload")) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.no_permission");
                    return true;
                }

                plugin.reloadPlugin(sender);
                return true;
            }

            if (args[0].equalsIgnoreCase("resetreward")) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                return resetRewardCommand.onCommand(sender, command, label, subArgs);
            }
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("commands.help", "version", plugin.getDescription().getVersion()));
        return true;
    }
}

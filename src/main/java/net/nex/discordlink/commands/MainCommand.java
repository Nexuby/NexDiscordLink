package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ADMIN_ACTION;

public class MainCommand implements CommandExecutor {

    private final NexDiscordLink plugin;
    private final ResetRewardCommand resetRewardCommand;
    private final StatusCommand statusCommand;

    public MainCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.resetRewardCommand = new ResetRewardCommand(plugin);
        this.statusCommand = new StatusCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (CommandAliases.isReload(args[0])) {
                if (!sender.hasPermission("nexdiscord.reload")) {
                    plugin.getLanguageManager().sendMessage(sender, "commands.no_permission");
                    return true;
                }

                plugin.getAuditLogger().log(ADMIN_ACTION, sender.getName(), "Plugin reload requested");
                plugin.reloadPlugin(sender);
                return true;
            }

            if (CommandAliases.isResetReward(args[0])) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                return resetRewardCommand.onCommand(sender, command, label, subArgs);
            }

            if (CommandAliases.isStatus(args[0])) {
                return statusCommand.execute(sender);
            }
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("commands.help", "version", plugin.getDescription().getVersion()));
        return true;
    }
}

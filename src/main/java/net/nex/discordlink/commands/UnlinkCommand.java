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

        if (!player.hasPermission("nexdiscord.unlink")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no_permission");
            return true;
        }

        if (!plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "commands.unlink_not_linked");
            return true;
        }

        String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
        if (!plugin.getConfig().getBoolean("unlink-security.require-confirmation", true)) {
            plugin.getUnlinkManager().performUnlink(player.getUniqueId(), "Minecraft command", success -> {
                if (!success) plugin.getLanguageManager().sendMessage(player, "commands.unlink_failed");
            });
            return true;
        }

        if (plugin.getTwoFactorManager().has2FA(player.getUniqueId())) {
            if (args.length == 0) {
                plugin.getLanguageManager().sendMessage(player, "commands.unlink_2fa_required");
                return true;
            }
            int code;
            try {
                code = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
                return true;
            }
            if (plugin.getTwoFactorManager().verifyStoredCode(player.getUniqueId(), code)
                    != net.nex.discordlink.utils.TwoFactorManager.VerificationResult.SUCCESS) {
                plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
                return true;
            }
            plugin.getUnlinkManager().performUnlink(player.getUniqueId(), "Minecraft command with 2FA", success -> {
                if (!success) plugin.getLanguageManager().sendMessage(player, "commands.unlink_failed");
            });
            return true;
        }

        if (plugin.getUnlinkManager().requestDiscordApproval(player, discordId)) {
            plugin.getLanguageManager().sendMessage(player, "commands.unlink_confirmation_sent");
        } else {
            plugin.getLanguageManager().sendMessage(player, "security.unlink_dm_failed");
        }

        return true;
    }
}

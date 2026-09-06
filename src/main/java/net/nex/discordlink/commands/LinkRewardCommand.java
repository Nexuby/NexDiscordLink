package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class LinkRewardCommand implements CommandExecutor {

    private final NexDiscordLink plugin;

    public LinkRewardCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLanguageManager().sendMessage(sender, "commands.player_only");
            return true;
        }
        if (!player.hasPermission("nexdiscord.reward.preview")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no_permission");
            return true;
        }

        List<String> preview = plugin.getConfig().getStringList("rewards.link-rewards.preview");
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int claimed = plugin.getDatabaseManager().getLinkRewardCount(uuid);
            int limit = plugin.getConfig().getInt("rewards.link-rewards.limit", 1);
            String remaining = limit <= 0 ? "∞" : Integer.toString(Math.max(0, limit - claimed));
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLanguageManager().sendMessage(
                        player,
                        "commands.reward_preview_header",
                        "claimed", Integer.toString(claimed),
                        "remaining", remaining
                );
                for (String line : preview) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
            });
        });
        return true;
    }
}

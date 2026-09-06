package net.nex.discordlink.commands;

import net.dv8tion.jda.api.JDA;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class StatusCommand {

    private final NexDiscordLink plugin;

    public StatusCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender) {
        if (!sender.hasPermission("nexdiscord.admin")) {
            plugin.getLanguageManager().sendMessage(sender, "commands.no_permission");
            return true;
        }

        JDA jda = plugin.getDiscordBot() == null ? null : plugin.getDiscordBot().getJda();
        boolean botConnected = jda != null && jda.getStatus() == JDA.Status.CONNECTED;
        int guildCount = jda == null ? 0 : jda.getGuilds().size();
        String databaseType = plugin.getConfig().getString("database-settings.type", "sqlite").toUpperCase();
        String botStatus = localizedState(botConnected);
        String securityStatus = localizedState(plugin.getSecurityManager() != null);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean databaseHealthy = plugin.getDatabaseManager().ping();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getLanguageManager().sendMessage(
                    sender,
                    "commands.admin_status",
                    "version", plugin.getDescription().getVersion(),
                    "database_type", databaseType,
                    "database_status", localizedState(databaseHealthy),
                    "bot_status", botStatus,
                    "guild_count", Integer.toString(guildCount),
                    "security_status", securityStatus
            ));
        });
        return true;
    }

    private String localizedState(boolean healthy) {
        return plugin.getLanguageManager().getMessage(
                healthy ? "commands.status_ok" : "commands.status_unavailable"
        );
    }
}

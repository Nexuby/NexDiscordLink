package net.nex.discordlink.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class SyncManager {

    private final NexDiscordLink plugin;

    public SyncManager(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public void updateNickname(Player player) {
        if (!plugin.getConfig().getBoolean("sync.nickname.enabled", true)) return;

        String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
        if (discordId == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getDiscordBot().getJda() == null) return;

            // Iterate all guilds or specific guild
            // For simplicity, we assume the bot is in one main guild or we update in all
            for (Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
                guild.retrieveMemberById(discordId).queue(member -> {
                    String format = plugin.getConfig().getString("sync.nickname.format", "%name%");
                    String nickname = format.replace("%name%", player.getName());
                    // Add placeholderAPI support if needed, but for now just name

                    if (nickname.length() > 32) nickname = nickname.substring(0, 32);

                    try {
                        guild.modifyNickname(member, nickname).queue(
                            success -> {},
                            error -> plugin.getLanguageManager().sendConsoleMessage("console.bot_error", "error", "Failed to update nickname: " + error.getMessage())
                        );
                    } catch (Exception e) {
                        // Permission error likely
                    }
                }, error -> {});
            }
        });
    }

    public void syncBan(Player player, String reason) {
        if (!plugin.getConfig().getBoolean("sync.ban.enabled", false)) return;

        String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
        if (discordId == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getDiscordBot().getJda() == null) return;

            for (Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
                guild.retrieveMemberById(discordId).queue(member -> {
                    String banReason = plugin.getLanguageManager().getMessage("sync.ban_reason", "reason", reason);
                    guild.ban(member, 0, TimeUnit.SECONDS).reason(banReason).queue(
                        success -> {},
                        error -> plugin.getLanguageManager().sendConsoleMessage("console.bot_error", "error", "Failed to ban user: " + error.getMessage())
                    );
                }, error -> {});
            }
        });
    }
}

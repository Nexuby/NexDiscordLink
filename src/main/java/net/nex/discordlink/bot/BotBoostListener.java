package net.nex.discordlink.bot;

import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class BotBoostListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public BotBoostListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent event) {
        // Check if user started boosting (old time null, new time not null)
        if (event.getOldTimeBoosted() == null && event.getNewTimeBoosted() != null) {
            String discordId = event.getUser().getId();
            UUID uuid = plugin.getDatabaseManager().getPlayerUUID(discordId);

            if (uuid != null) {
                // Give rewards on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        plugin.getLanguageManager().sendMessage(player, "rewards.boost_received");
                    }

                    // Execute boost rewards (can be configured separately, but for now using salary commands or a new section)
                    // Plan says "give rewards/perks". Let's assume a new config section "rewards.boost-commands"
                    List<String> commands = plugin.getConfig().getStringList("rewards.boost-commands");
                    if (commands.isEmpty()) {
                        // Fallback to salary commands if not defined
                        commands = plugin.getConfig().getStringList("rewards.commands");
                    }

                    String playerName = (player != null) ? player.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                    if (playerName != null) {
                        for (String cmd : commands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
                        }
                    }
                });
            }
        }
    }
}

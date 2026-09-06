package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardScheduler extends BukkitRunnable {

    private final NexDiscordLink plugin;

    public RewardScheduler(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<String> commands = plugin.getConfig().getStringList("rewards.commands");
        if (commands.isEmpty()) return;

        // Collect online linked players on main thread, then check DB async
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<UUID> linkedPlayers = new ArrayList<>();
            for (Player player : onlinePlayers) {
                if (player.isOnline() && plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
                    linkedPlayers.add(player.getUniqueId());
                }
            }

            // Give rewards on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID uuid : linkedPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        for (String cmd : commands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
                        }
                        plugin.getLanguageManager().sendMessage(player, "rewards.salary_received");
                    }
                }
            });
        });
    }
}

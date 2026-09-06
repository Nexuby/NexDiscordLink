package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LinkManager {

    private final NexDiscordLink plugin;
    private final Map<String, UUID> codeMap;
    private final Map<UUID, String> playerCodeMap;
    private final Random random;

    public LinkManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.codeMap = new ConcurrentHashMap<>();
        this.playerCodeMap = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    public String generateCode(UUID uuid) {
        // Return existing code if active
        if (playerCodeMap.containsKey(uuid)) {
            return playerCodeMap.get(uuid);
        }

        String code = String.format("%04d", random.nextInt(10000));
        while (codeMap.containsKey(code)) {
            code = String.format("%04d", random.nextInt(10000));
        }

        codeMap.put(code, uuid);
        playerCodeMap.put(uuid, code);

        final String finalCode = code;

        // Expire after 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalCode.equals(playerCodeMap.get(uuid))) {
                    playerCodeMap.remove(uuid);
                    codeMap.remove(finalCode);
                }
            }
        }.runTaskLater(plugin, 20 * 60 * 5);

        return code;
    }

    public UUID verifyCode(String code) {
        UUID uuid = codeMap.remove(code);
        if (uuid != null) {
            playerCodeMap.remove(uuid);
        }
        return uuid;
    }

    public void processLink(UUID uuid, String discordId, String discordName, Consumer<String> replyCallback) {
        // Check if discord user is already linked
        if (plugin.getDatabaseManager().getPlayerUUID(discordId) != null) {
            replyCallback.accept(plugin.getLanguageManager().getMessage("link.already_linked"));
            return;
        }

        // Link
        plugin.getDatabaseManager().createPlayer(uuid, discordId);

        // Switch to main thread for Bukkit API interactions
        Bukkit.getScheduler().runTask(plugin, () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String playerName = player.getName() != null ? player.getName() : "Unknown";

            replyCallback.accept(plugin.getLanguageManager().getMessage("link.success", "player", playerName));

            // Notify player if online
            if (player.isOnline()) {
                plugin.getLanguageManager().sendMessage(player.getPlayer(), "commands.link_success_player", "discord", discordName);

                // Give Link Rewards
                giveLinkRewards(player.getPlayer());

                // Sync Role
                plugin.getRoleManager().syncPlayerRole(player.getPlayer());
            }
        });
    }

    private void giveLinkRewards(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("rewards.link-rewards.enabled", false)) return;

        int limit = plugin.getConfig().getInt("rewards.link-rewards.limit", 1);
        int currentCount = plugin.getDatabaseManager().getLinkRewardCount(player.getUniqueId());

        if (limit > 0 && currentCount >= limit) {
            plugin.getLanguageManager().sendMessage(player, "rewards.link_reward_limit_reached");
            return;
        }

        // Give rewards
        List<String> commands = plugin.getConfig().getStringList("rewards.link-rewards.commands");
        for (String cmd : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
        }

        // Increment count
        plugin.getDatabaseManager().incrementLinkRewardCount(player.getUniqueId());
        plugin.getLanguageManager().sendMessage(player, "rewards.link_reward_received");
    }
}

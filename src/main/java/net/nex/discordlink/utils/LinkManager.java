package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ACCOUNT_LINKED;

public class LinkManager {

    private final NexDiscordLink plugin;
    private final Map<String, UUID> codeMap;
    private final Map<UUID, String> playerCodeMap;
    private final Map<String, Deque<Long>> failedAttempts;
    private final java.util.Set<String> codesInProgress;
    private final SecureRandom random;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    public LinkManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.codeMap = new ConcurrentHashMap<>();
        this.playerCodeMap = new ConcurrentHashMap<>();
        this.failedAttempts = new ConcurrentHashMap<>();
        this.codesInProgress = ConcurrentHashMap.newKeySet();
        this.random = new SecureRandom();
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

    public boolean isDmEnabled() {
        String type = plugin.getConfig().getString("link-system.type", "BOTH");
        return !"MODAL".equalsIgnoreCase(type);
    }

    public boolean isModalEnabled() {
        String type = plugin.getConfig().getString("link-system.type", "BOTH");
        return !"DM".equalsIgnoreCase(type);
    }

    public void processLinkCode(String code, String discordId, String discordName, Consumer<String> replyCallback) {
        if (isRateLimited(discordId)) {
            replyCallback.accept(plugin.getLanguageManager().getMessage("link.rate_limited"));
            return;
        }

        UUID uuid = codeMap.get(code);
        if (uuid == null) {
            recordFailedAttempt(discordId);
            replyCallback.accept(plugin.getLanguageManager().getMessage("link.invalid_code"));
            return;
        }

        if (!codesInProgress.add(code)) {
            replyCallback.accept(plugin.getLanguageManager().getMessage("link.code_in_use"));
            return;
        }

        try {
            if (plugin.getDatabaseManager().getPlayerUUID(discordId) != null) {
                replyCallback.accept(plugin.getLanguageManager().getMessage("link.already_linked"));
                return;
            }

            if (plugin.getDatabaseManager().isLinked(uuid)) {
                removeCode(code, uuid);
                replyCallback.accept(plugin.getLanguageManager().getMessage("link.minecraft_already_linked"));
                return;
            }

            if (!plugin.getDatabaseManager().createPlayer(uuid, discordId)) {
                replyCallback.accept(plugin.getLanguageManager().getMessage("link.failed"));
                return;
            }

            removeCode(code, uuid);
            failedAttempts.remove(discordId);
            completeLink(uuid, discordName, replyCallback);
        } finally {
            codesInProgress.remove(code);
        }
    }

    private void removeCode(String code, UUID uuid) {
        if (codeMap.remove(code, uuid)) {
            playerCodeMap.remove(uuid, code);
        }
    }

    private void completeLink(UUID uuid, String discordName, Consumer<String> replyCallback) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String playerName = player.getName() != null ? player.getName() : "Unknown";

            replyCallback.accept(plugin.getLanguageManager().getMessage("link.success", "player", playerName));
            plugin.getAuditLogger().log(ACCOUNT_LINKED, playerName, "Discord: " + discordName);

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

    private boolean isRateLimited(String discordId) {
        Deque<Long> attempts = failedAttempts.get(discordId);
        if (attempts == null) return false;

        long cutoff = System.currentTimeMillis() - ATTEMPT_WINDOW_MILLIS;
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.removeFirst();
            }
            if (attempts.isEmpty()) {
                failedAttempts.remove(discordId, attempts);
                return false;
            }
            return attempts.size() >= MAX_FAILED_ATTEMPTS;
        }
    }

    private void recordFailedAttempt(String discordId) {
        Deque<Long> attempts = failedAttempts.computeIfAbsent(discordId, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
        }
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

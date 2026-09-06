package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private final java.util.Set<UUID> pendingProxyLinks;
    private final SecureRandom random;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    public LinkManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.codeMap = new ConcurrentHashMap<>();
        this.playerCodeMap = new ConcurrentHashMap<>();
        this.failedAttempts = new ConcurrentHashMap<>();
        this.codesInProgress = ConcurrentHashMap.newKeySet();
        this.pendingProxyLinks = ConcurrentHashMap.newKeySet();
        this.random = new SecureRandom();
    }

    public String generateCode(UUID uuid) {
        if (isProxyMode()) return generateSharedCode(uuid);

        // Return existing code if active
        if (playerCodeMap.containsKey(uuid)) {
            return playerCodeMap.get(uuid);
        }

        String code = createNumericCode();
        while (codeMap.containsKey(code)) {
            code = createNumericCode();
        }

        codeMap.put(code, uuid);
        playerCodeMap.put(uuid, code);

        final String finalCode = code;

        long expiryMinutes = Math.max(1L, plugin.getConfig().getLong("link-system.code-expiry-minutes", 5L));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalCode.equals(playerCodeMap.get(uuid))) {
                    playerCodeMap.remove(uuid);
                    codeMap.remove(finalCode);
                }
            }
        }.runTaskLater(plugin, 20L * 60L * expiryMinutes);

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

        UUID uuid = isProxyMode()
                ? plugin.getDatabaseManager().getLinkCodeOwner(code, System.currentTimeMillis())
                : codeMap.get(code);
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
            completeLink(uuid, discordId, discordName, replyCallback);
        } finally {
            codesInProgress.remove(code);
        }
    }

    private void removeCode(String code, UUID uuid) {
        if (isProxyMode()) {
            plugin.getDatabaseManager().removeLinkCode(code, uuid);
            pendingProxyLinks.remove(uuid);
            return;
        }
        if (codeMap.remove(code, uuid)) {
            playerCodeMap.remove(uuid, code);
        }
    }

    private String generateSharedCode(UUID uuid) {
        long now = System.currentTimeMillis();
        plugin.getDatabaseManager().deleteExpiredLinkCodes(now);
        String existing = plugin.getDatabaseManager().getActiveLinkCode(uuid, now);
        if (existing != null) {
            pendingProxyLinks.add(uuid);
            return existing;
        }

        long expiryMinutes = Math.max(1L, plugin.getConfig().getLong("link-system.code-expiry-minutes", 5L));
        long expiresAt = now + Duration.ofMinutes(expiryMinutes).toMillis();
        String serverId = plugin.getConfig().getString("proxy.server-id", "server");
        for (int attempt = 0; attempt < 25; attempt++) {
            String code = createNumericCode();
            if (plugin.getDatabaseManager().createLinkCode(code, uuid, serverId, expiresAt)) {
                pendingProxyLinks.add(uuid);
                return code;
            }
        }
        return null;
    }

    private String createNumericCode() {
        int length = Math.max(4, Math.min(9, plugin.getConfig().getInt("link-system.code-length", 6)));
        int bound = 1;
        for (int i = 0; i < length; i++) bound *= 10;
        return String.format("%0" + length + "d", random.nextInt(bound));
    }

    private boolean isProxyMode() {
        return plugin.getConfig().getBoolean("proxy.enabled", false);
    }

    public void checkProxyCompletions() {
        if (!isProxyMode() || pendingProxyLinks.isEmpty()) return;
        java.util.Set<UUID> snapshot = new java.util.HashSet<>(pendingProxyLinks);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            for (UUID uuid : snapshot) {
                if (plugin.getDatabaseManager().isLinked(uuid)) {
                    String discordId = plugin.getDatabaseManager().getDiscordId(uuid);
                    Bukkit.getScheduler().runTask(plugin, () -> completeProxyLink(uuid, discordId));
                } else if (plugin.getDatabaseManager().getActiveLinkCode(uuid, now) == null) {
                    pendingProxyLinks.remove(uuid);
                }
            }
        });
    }

    private void completeProxyLink(UUID uuid, String discordId) {
        if (!pendingProxyLinks.remove(uuid)) return;
        org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        String discordName = discordId;
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            net.dv8tion.jda.api.entities.User user = plugin.getDiscordBot().getJda().getUserById(discordId);
            if (user != null) discordName = user.getName();
        }
        plugin.getLanguageManager().sendMessage(player, "commands.link_success_player", "discord", discordName);
        giveLinkRewards(player, discordId);
        plugin.getRoleManager().syncPlayerRole(player);
        plugin.refreshPlaceholders(uuid);
    }

    private void completeLink(UUID uuid, String discordId, String discordName, Consumer<String> replyCallback) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String playerName = player.getName() != null ? player.getName() : "Unknown";

            replyCallback.accept(plugin.getLanguageManager().getMessage("link.success", "player", playerName));
            plugin.getAuditLogger().log(ACCOUNT_LINKED, playerName, "Discord: " + discordName);

            // Notify player if online
            if (player.isOnline()) {
                plugin.getLanguageManager().sendMessage(player.getPlayer(), "commands.link_success_player", "discord", discordName);

                // Give Link Rewards
                giveLinkRewards(player.getPlayer(), discordId);

                // Sync Role
                plugin.getRoleManager().syncPlayerRole(player.getPlayer());
            }
            plugin.refreshPlaceholders(uuid);
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

    private void giveLinkRewards(org.bukkit.entity.Player player, String discordId) {
        if (!plugin.getConfig().getBoolean("rewards.link-rewards.enabled", false)) return;

        int limit = plugin.getConfig().getInt("rewards.link-rewards.limit", 1);
        int currentCount = plugin.getDatabaseManager().getLinkRewardCount(player.getUniqueId());

        if (limit > 0 && currentCount >= limit) {
            plugin.getLanguageManager().sendMessage(player, "rewards.link_reward_limit_reached");
            return;
        }

        LinkedHashSet<String> commands = new LinkedHashSet<>(
                plugin.getConfig().getStringList("rewards.link-rewards.commands")
        );
        commands.addAll(plugin.getConfig().getStringList(
                currentCount == 0
                        ? "rewards.link-rewards.first-link-commands"
                        : "rewards.link-rewards.relink-commands"
        ));
        commands.addAll(getDiscordRoleRewardCommands(discordId));

        if (commands.isEmpty()) return;
        for (String cmd : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
        }

        // Increment count
        plugin.getDatabaseManager().incrementLinkRewardCount(player.getUniqueId());
        plugin.getLanguageManager().sendMessage(player, "rewards.link_reward_received");
    }

    private List<String> getDiscordRoleRewardCommands(String discordId) {
        List<String> commands = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(
                "rewards.link-rewards.discord-role-commands"
        );
        if (section == null || plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) {
            return commands;
        }

        java.util.Set<String> roleIds = new java.util.HashSet<>();
        for (net.dv8tion.jda.api.entities.Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
            net.dv8tion.jda.api.entities.Member member = guild.getMemberById(discordId);
            if (member == null) continue;
            for (net.dv8tion.jda.api.entities.Role role : member.getRoles()) {
                roleIds.add(role.getId());
            }
        }
        for (String roleId : section.getKeys(false)) {
            if (roleIds.contains(roleId)) commands.addAll(section.getStringList(roleId));
        }
        return commands;
    }
}

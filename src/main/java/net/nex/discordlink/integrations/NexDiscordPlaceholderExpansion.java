package net.nex.discordlink.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NexDiscordPlaceholderExpansion extends PlaceholderExpansion implements Listener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final NexDiscordLink plugin;
    private final Map<UUID, CachedSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Set<UUID> refreshInProgress = ConcurrentHashMap.newKeySet();
    private static final long CACHE_MILLIS = Duration.ofSeconds(30).toMillis();

    public NexDiscordPlaceholderExpansion(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexdiscord";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nexuby";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        AccountSnapshot snapshot = getSnapshot(player.getUniqueId());
        return switch (params.toLowerCase(java.util.Locale.ROOT)) {
            case "linked" -> Boolean.toString(snapshot.discordId != null);
            case "discord_id" -> snapshot.discordId == null ? "" : snapshot.discordId;
            case "discord_username" -> snapshot.discordName;
            case "2fa_enabled" -> Boolean.toString(snapshot.twoFactorEnabled);
            case "linked_at" -> formatLinkedAt(snapshot.linkedAt);
            case "reward_count" -> Integer.toString(snapshot.rewardCount);
            default -> null;
        };
    }

    public void refresh(UUID uuid) {
        if (!refreshInProgress.add(uuid)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String discordId = plugin.getDatabaseManager().getDiscordId(uuid);
                snapshots.put(uuid, new CachedSnapshot(
                        new AccountSnapshot(
                                discordId,
                                resolveDiscordName(discordId),
                                plugin.getDatabaseManager().get2FASecret(uuid) != null,
                                plugin.getDatabaseManager().getLinkedAt(uuid),
                                plugin.getDatabaseManager().getLinkRewardCount(uuid)
                        ),
                        System.currentTimeMillis() + CACHE_MILLIS
                ));
            } finally {
                refreshInProgress.remove(uuid);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snapshots.remove(event.getPlayer().getUniqueId());
        refreshInProgress.remove(event.getPlayer().getUniqueId());
    }

    private AccountSnapshot getSnapshot(UUID uuid) {
        CachedSnapshot cached = snapshots.get(uuid);
        if (cached == null || cached.expiresAt < System.currentTimeMillis()) refresh(uuid);
        return cached == null ? AccountSnapshot.EMPTY : cached.snapshot;
    }

    private String resolveDiscordName(String discordId) {
        if (discordId == null || plugin.getDiscordBot() == null) return "";
        JDA jda = plugin.getDiscordBot().getJda();
        if (jda == null) return "";
        User user = jda.getUserById(discordId);
        return user == null ? "" : user.getName();
    }

    private String formatLinkedAt(long timestamp) {
        return timestamp <= 0 ? "" : DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    private record CachedSnapshot(AccountSnapshot snapshot, long expiresAt) {
    }

    private record AccountSnapshot(
            String discordId,
            String discordName,
            boolean twoFactorEnabled,
            long linkedAt,
            int rewardCount
    ) {
        private static final AccountSnapshot EMPTY = new AccountSnapshot(null, "", false, 0L, 0);
    }
}

package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {

    private final NexDiscordLink plugin;
    private final SensitiveDataProtector dataProtector;
    private final Set<UUID> frozenPlayers;
    private final Map<UUID, VerificationSession> verificationSessions;
    private final SecureRandom secureRandom;

    private static final long VERIFICATION_TIMEOUT_TICKS = 20L * 60L * 5L;
    private static final long VERIFICATION_TIMEOUT_MILLIS = Duration.ofMinutes(5).toMillis();

    public SecurityManager(NexDiscordLink plugin, SensitiveDataProtector dataProtector) {
        this.plugin = plugin;
        this.dataProtector = dataProtector;
        this.frozenPlayers = ConcurrentHashMap.newKeySet();
        this.verificationSessions = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
    }

    public String beginVerification(Player player, String discordId, String ipAddress) {
        UUID uuid = player.getUniqueId();
        byte[] tokenBytes = new byte[18];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        VerificationSession session = new VerificationSession(
                discordId,
                ipAddress,
                token,
                System.currentTimeMillis() + VERIFICATION_TIMEOUT_MILLIS
        );
        verificationSessions.put(uuid, session);
        frozenPlayers.add(uuid);
        plugin.getLanguageManager().sendMessage(player, "security.frozen_message");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> expireVerification(uuid, token), VERIFICATION_TIMEOUT_TICKS);
        return token;
    }

    public VerificationResult validateVerification(UUID uuid, String discordId, String token, String currentIp) {
        VerificationSession session = verificationSessions.get(uuid);
        if (session == null) return VerificationResult.NOT_FOUND;
        if (System.currentTimeMillis() > session.expiresAt()) return VerificationResult.EXPIRED;
        if (!session.discordId().equals(discordId)) return VerificationResult.USER_MISMATCH;
        if (!constantTimeEquals(session.token(), token)) return VerificationResult.INVALID_TOKEN;
        if (!session.ipAddress().equals(currentIp)) return VerificationResult.IP_MISMATCH;
        return VerificationResult.VALID;
    }

    public boolean completeVerification(UUID uuid, String token) {
        VerificationSession session = verificationSessions.get(uuid);
        if (session == null || !constantTimeEquals(session.token(), token)) return false;
        if (!verificationSessions.remove(uuid, session)) return false;

        frozenPlayers.remove(uuid);
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            plugin.getLanguageManager().sendMessage(player, "security.verified_success");
        }
        return true;
    }

    public boolean cancelVerification(UUID uuid, String token) {
        VerificationSession session = verificationSessions.get(uuid);
        if (session == null || !constantTimeEquals(session.token(), token)) return false;
        boolean removed = verificationSessions.remove(uuid, session);
        if (removed) frozenPlayers.remove(uuid);
        return removed;
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public String protectIp(String ipAddress) {
        return dataProtector.fingerprintIp(ipAddress);
    }

    public boolean matchesIp(String storedValue, String ipAddress) {
        return dataProtector.matchesIp(storedValue, ipAddress);
    }

    public boolean isProtectedIp(String storedValue) {
        return dataProtector.isIpFingerprint(storedValue);
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        frozenPlayers.remove(uuid);
        verificationSessions.remove(uuid);
    }

    private void expireVerification(UUID uuid, String token) {
        VerificationSession session = verificationSessions.get(uuid);
        if (session == null || !constantTimeEquals(session.token(), token)) return;
        if (System.currentTimeMillis() <= session.expiresAt()) return;

        if (verificationSessions.remove(uuid, session)) {
            frozenPlayers.remove(uuid);
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.kickPlayer(plugin.getLanguageManager().getMessage("security.verification_expired"));
            }
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private record VerificationSession(String discordId, String ipAddress, String token, long expiresAt) {
    }

    public enum VerificationResult {
        VALID,
        NOT_FOUND,
        EXPIRED,
        USER_MISMATCH,
        INVALID_TOKEN,
        IP_MISMATCH
    }
}

package net.nex.discordlink.utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwoFactorManager {

    private static final long SETUP_TIMEOUT_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final long ATTEMPT_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final NexDiscordLink plugin;
    private final GoogleAuthenticator authenticator;
    private final SensitiveDataProtector dataProtector;
    private final java.util.Set<UUID> pendingVerification = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PendingSetup> setupPending = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> failedAttempts = new ConcurrentHashMap<>();

    public TwoFactorManager(NexDiscordLink plugin, SensitiveDataProtector dataProtector) {
        this.plugin = plugin;
        this.dataProtector = dataProtector;
        this.authenticator = new GoogleAuthenticator();
    }

    public String generateSecret(Player player) {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        String secret = key.getKey();
        setupPending.put(
                player.getUniqueId(),
                new PendingSetup(secret, System.currentTimeMillis() + SETUP_TIMEOUT_MILLIS)
        );
        failedAttempts.remove(player.getUniqueId());
        return secret;
    }

    public VerificationResult verifySetup(Player player, int code) {
        UUID uuid = player.getUniqueId();
        PendingSetup pending = setupPending.get(uuid);
        if (pending == null) return VerificationResult.NOT_CONFIGURED;
        if (System.currentTimeMillis() > pending.expiresAt()) {
            setupPending.remove(uuid, pending);
            return VerificationResult.EXPIRED;
        }
        if (isRateLimited(uuid)) return VerificationResult.RATE_LIMITED;

        if (!authenticator.authorize(pending.secret(), code)) {
            recordFailedAttempt(uuid);
            return VerificationResult.INVALID;
        }

        String encryptedSecret = dataProtector.encrypt(pending.secret());
        if (!plugin.getDatabaseManager().set2FASecret(uuid, encryptedSecret)) {
            return VerificationResult.STORAGE_ERROR;
        }

        setupPending.remove(uuid, pending);
        failedAttempts.remove(uuid);
        return VerificationResult.SUCCESS;
    }

    public VerificationResult verifyLogin(Player player, int code) {
        UUID uuid = player.getUniqueId();
        VerificationResult result = verifyStoredCode(uuid, code);
        if (result == VerificationResult.SUCCESS) {
            pendingVerification.remove(uuid);
        }
        return result;
    }

    public VerificationResult verifyStoredCode(UUID uuid, int code) {
        if (isRateLimited(uuid)) return VerificationResult.RATE_LIMITED;

        String secret;
        try {
            secret = loadSecret(uuid);
        } catch (IllegalStateException exception) {
            plugin.getLogger().severe("Could not decrypt the 2FA secret for " + uuid + ": " + exception.getMessage());
            return VerificationResult.STORAGE_ERROR;
        }
        if (secret == null) return VerificationResult.NOT_CONFIGURED;
        if (!authenticator.authorize(secret, code)) {
            recordFailedAttempt(uuid);
            return VerificationResult.INVALID;
        }

        failedAttempts.remove(uuid);
        return VerificationResult.SUCCESS;
    }

    public boolean hasPendingSetup(UUID uuid) {
        PendingSetup pending = setupPending.get(uuid);
        if (pending == null) return false;
        if (System.currentTimeMillis() <= pending.expiresAt()) return true;
        setupPending.remove(uuid, pending);
        return false;
    }

    public boolean isPendingVerification(UUID uuid) {
        return pendingVerification.contains(uuid);
    }

    public void addPendingVerification(UUID uuid) {
        pendingVerification.add(uuid);
    }

    public void removePendingVerification(UUID uuid) {
        pendingVerification.remove(uuid);
        failedAttempts.remove(uuid);
    }

    public boolean has2FA(UUID uuid) {
        return plugin.getDatabaseManager().get2FASecret(uuid) != null;
    }

    public boolean remove2FA(UUID uuid) {
        boolean removed = plugin.getDatabaseManager().remove2FASecret(uuid);
        if (removed) {
            pendingVerification.remove(uuid);
            setupPending.remove(uuid);
            failedAttempts.remove(uuid);
        }
        return removed;
    }

    private String loadSecret(UUID uuid) {
        String storedSecret = plugin.getDatabaseManager().get2FASecret(uuid);
        if (storedSecret == null) return null;

        String plaintext = dataProtector.decrypt(storedSecret);
        if (!dataProtector.isEncrypted(storedSecret)) {
            String encrypted = dataProtector.encrypt(plaintext);
            if (!plugin.getDatabaseManager().set2FASecret(uuid, encrypted)) {
                plugin.getLogger().warning("Could not migrate a legacy 2FA secret for " + uuid);
            }
        }
        return plaintext;
    }

    private boolean isRateLimited(UUID uuid) {
        Deque<Long> attempts = failedAttempts.get(uuid);
        if (attempts == null) return false;

        long cutoff = System.currentTimeMillis() - ATTEMPT_WINDOW_MILLIS;
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.removeFirst();
            }
            if (attempts.isEmpty()) {
                failedAttempts.remove(uuid, attempts);
                return false;
            }
            return attempts.size() >= MAX_FAILED_ATTEMPTS;
        }
    }

    private void recordFailedAttempt(UUID uuid) {
        Deque<Long> attempts = failedAttempts.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
        }
    }

    private record PendingSetup(String secret, long expiresAt) {
    }

    public enum VerificationResult {
        SUCCESS,
        INVALID,
        RATE_LIMITED,
        EXPIRED,
        NOT_CONFIGURED,
        STORAGE_ERROR
    }
}

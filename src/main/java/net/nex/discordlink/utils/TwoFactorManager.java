package net.nex.discordlink.utils;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TwoFactorManager {

    private final NexDiscordLink plugin;
    private final GoogleAuthenticator gAuth;
    private final Set<UUID> pendingVerification = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> setupPending = new ConcurrentHashMap<>(); // UUID -> Secret

    public TwoFactorManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.gAuth = new GoogleAuthenticator();
    }

    public String generateSecret(Player player) {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        setupPending.put(player.getUniqueId(), secret);
        return secret;
    }

    public boolean verifySetup(Player player, int code) {
        if (!setupPending.containsKey(player.getUniqueId())) return false;

        String secret = setupPending.get(player.getUniqueId());
        if (gAuth.authorize(secret, code)) {
            plugin.getDatabaseManager().set2FASecret(player.getUniqueId(), secret);
            setupPending.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean verifyLogin(Player player, int code) {
        String secret = plugin.getDatabaseManager().get2FASecret(player.getUniqueId());
        if (secret == null) return true; // No 2FA setup

        if (gAuth.authorize(secret, code)) {
            pendingVerification.remove(player.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().getMessage("security.2fa_verified"));
            return true;
        }
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
    }

    public boolean has2FA(UUID uuid) {
        return plugin.getDatabaseManager().get2FASecret(uuid) != null;
    }

    public void remove2FA(UUID uuid) {
        plugin.getDatabaseManager().remove2FASecret(uuid);
    }
}

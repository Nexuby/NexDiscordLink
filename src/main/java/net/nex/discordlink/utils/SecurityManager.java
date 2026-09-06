package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {

    private final NexDiscordLink plugin;
    private final Set<UUID> frozenPlayers;

    public SecurityManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        this.frozenPlayers = ConcurrentHashMap.newKeySet();
    }

    public void freezePlayer(Player player) {
        frozenPlayers.add(player.getUniqueId());
        plugin.getLanguageManager().sendMessage(player, "security.frozen_message");
    }

    public void unfreezePlayer(UUID uuid) {
        frozenPlayers.remove(uuid);
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            plugin.getLanguageManager().sendMessage(player, "security.verified_success");
        }
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }
}

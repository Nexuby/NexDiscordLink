package net.nex.discordlink.listeners;

import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.utils.SyncManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class SyncListener implements Listener {

    private final NexDiscordLink plugin;
    private final SyncManager syncManager;

    public SyncListener(NexDiscordLink plugin, SyncManager syncManager) {
        this.plugin = plugin;
        this.syncManager = syncManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        syncManager.updateNickname(event.getPlayer());
        plugin.getRoleManager().syncPlayerRole(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        // Check if the player is being banned (kick due to ban)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getBanList(BanList.Type.NAME).isBanned(event.getPlayer().getName())) {
                String reason = "Banned from server";
                org.bukkit.BanEntry banEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(event.getPlayer().getName());
                if (banEntry != null && banEntry.getReason() != null) {
                    reason = banEntry.getReason();
                }
                syncManager.syncBan(event.getPlayer(), reason);
            }
        }, 1L); // Run 1 tick later so ban entry is registered
    }
}

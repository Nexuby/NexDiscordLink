package net.nex.discordlink.listeners;

import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateListener implements Listener {

    private final NexDiscordLink plugin;
    private final int resourceId;

    public UpdateListener(NexDiscordLink plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission("nexdiscord.update.notify")) return;
        if (!plugin.getConfig().getBoolean("settings.update-checker", true)) return;

        new UpdateChecker(plugin, resourceId).getVersion(version -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!event.getPlayer().isOnline()) return;
                if (!plugin.getDescription().getVersion().equalsIgnoreCase(version)) {
                    String currentVersion = plugin.getDescription().getVersion();
                    String link = "https://www.spigotmc.org/resources/" + resourceId;

                    plugin.getLanguageManager().sendMessage(event.getPlayer(), "update.notification",
                        "current_version", currentVersion,
                        "new_version", version,
                        "link", link
                    );
                }
            });
        });
    }
}

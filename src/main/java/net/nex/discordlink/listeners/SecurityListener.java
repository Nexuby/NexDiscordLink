package net.nex.discordlink.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.utils.SecurityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.Color;
import java.net.InetAddress;

public class SecurityListener implements Listener {

    private final NexDiscordLink plugin;
    private final SecurityManager securityManager;

    public SecurityListener(NexDiscordLink plugin, SecurityManager securityManager) {
        this.plugin = plugin;
        this.securityManager = securityManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("security-settings.enabled")) return;

        Player player = event.getPlayer();

        if (player.getAddress() == null) return; // Proxy or unusual join

        InetAddress address = player.getAddress().getAddress();
        if (address == null) return;
        String currentIp = address.getHostAddress();

        // Check if linked
        String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
        if (discordId == null) return; // Not linked, skip security check

        String storedIp = plugin.getDatabaseManager().getIpAddress(player.getUniqueId());

        if (storedIp == null) {
            // First time login after link or IP not saved yet
            plugin.getDatabaseManager().updateIpAddress(player.getUniqueId(), currentIp);
            // Continue to check 2FA...
        } else if (!storedIp.equals(currentIp)) {
            // IP Mismatch! Freeze player and send DM
            securityManager.freezePlayer(player);

            // Send DM
            if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
                plugin.getDiscordBot().getJda().retrieveUserById(discordId).queue(user -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(plugin.getLanguageManager().getMessage("security.dm_verify_title"));
                    embed.setDescription(plugin.getLanguageManager().getMessage("security.dm_verify_desc", "ip", currentIp));
                    embed.setColor(Color.RED);

                    user.openPrivateChannel().queue(channel -> {
                        channel.sendMessageEmbeds(embed.build())
                                .setActionRow(Button.success("verify_login:" + player.getUniqueId(), plugin.getLanguageManager().getMessage("security.dm_verify_button")))
                                .queue();
                    });
                });
            }
        }

        // Check 2FA
        if (plugin.getTwoFactorManager().has2FA(player.getUniqueId())) {
            plugin.getTwoFactorManager().addPendingVerification(player.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().getMessage("security.2fa_required"));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (securityManager.isFrozen(event.getPlayer()) || plugin.getTwoFactorManager().isPendingVerification(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (securityManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getTwoFactorManager().isPendingVerification(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            // Allow entering code in chat? Maybe not secure, better use command.
            // But for UX, many plugins allow chat input.
            // Let's stick to command /2fa login <code> for now to be safe and consistent.
            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("security.2fa_required"));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (securityManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getTwoFactorManager().isPendingVerification(event.getPlayer().getUniqueId())) {
            String msg = event.getMessage().toLowerCase();
            if (!msg.startsWith("/2fa") && !msg.startsWith("/login") && !msg.startsWith("/verify")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("security.2fa_required"));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (securityManager.isFrozen(event.getPlayer()) || plugin.getTwoFactorManager().isPendingVerification(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        securityManager.removePlayer(event.getPlayer());
        plugin.getTwoFactorManager().removePendingVerification(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (securityManager.isFrozen(event.getPlayer()) || plugin.getTwoFactorManager().isPendingVerification(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}

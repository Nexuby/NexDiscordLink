package net.nex.discordlink.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.commands.CommandAliases;
import net.nex.discordlink.utils.SecurityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.awt.Color;
import java.net.InetAddress;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.IP_CHALLENGE;

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
            plugin.getDatabaseManager().updateIpAddress(player.getUniqueId(), securityManager.protectIp(currentIp));
            // Continue to check 2FA...
        } else if (!securityManager.matchesIp(storedIp, currentIp)) {
            // IP Mismatch! Freeze player and send DM
            String verificationToken = securityManager.beginVerification(player, discordId, currentIp);
            plugin.getAuditLogger().log(IP_CHALLENGE, player.getName(), "New network address requires verification");

            // Send DM
            if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
                plugin.getDiscordBot().getJda().retrieveUserById(discordId).queue(user -> {
                    EmbedBuilder embed = plugin.getDiscordMessageManager().createEmbed(
                            "security-login",
                            "security.dm_verify_title",
                            "security.dm_verify_desc",
                            Color.RED,
                            player.getName(),
                            player.getUniqueId(),
                            "ip", currentIp
                    );

                    user.openPrivateChannel().queue(channel -> {
                        channel.sendMessageEmbeds(embed.build())
                                .setActionRow(Button.of(
                                        plugin.getDiscordMessageManager().resolveButtonStyle(
                                                "security-login",
                                                net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS
                                        ),
                                        "verify_login:" + player.getUniqueId() + ":" + verificationToken,
                                        plugin.getDiscordMessageManager().resolveText(
                                                "security-login",
                                                "button-label",
                                                "security.dm_verify_button",
                                                80,
                                                "player", player.getName()
                                        )
                                ))
                                .queue(
                                        success -> {},
                                        error -> handleDiscordDeliveryFailure(player.getUniqueId(), verificationToken)
                                );
                    }, error -> handleDiscordDeliveryFailure(player.getUniqueId(), verificationToken));
                }, error -> handleDiscordDeliveryFailure(player.getUniqueId(), verificationToken));
            } else {
                handleDiscordDeliveryFailure(player.getUniqueId(), verificationToken);
            }
        } else if (!securityManager.isProtectedIp(storedIp)) {
            // Transparently migrate legacy plaintext IP values.
            plugin.getDatabaseManager().updateIpAddress(player.getUniqueId(), securityManager.protectIp(currentIp));
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
            if (!CommandAliases.isAllowedDuringTwoFactorVerification(event.getMessage())) {
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isRestricted(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isRestricted(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (isRestricted(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (isRestricted(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isRestricted(Player player) {
        return securityManager.isFrozen(player)
                || plugin.getTwoFactorManager().isPendingVerification(player.getUniqueId());
    }

    private void handleDiscordDeliveryFailure(java.util.UUID uuid, String verificationToken) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!securityManager.cancelVerification(uuid, verificationToken)) return;
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.kickPlayer(plugin.getLanguageManager().getMessage("security.discord_unavailable"));
            }
        });
    }
}

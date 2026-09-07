package net.nex.discordlink.utils;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ACCOUNT_UNLINKED;

public class UnlinkManager {

    private final NexDiscordLink plugin;
    private final UnlinkApprovalStore approvals = new UnlinkApprovalStore();

    public UnlinkManager(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public boolean requestDiscordApproval(Player player, String discordId) {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) return false;

        String token = approvals.create(player.getUniqueId(), discordId);
        plugin.getDiscordBot().getJda().retrieveUserById(discordId).queue(user ->
                user.openPrivateChannel().queue(channel ->
                        channel.sendMessage(plugin.getDiscordMessageManager().resolveText(
                                        "unlink-confirmation",
                                        "content",
                                        "security.unlink_dm_request",
                                        2000,
                                        "player", player.getName(),
                                        "uuid", player.getUniqueId().toString()
                                ))
                                .setActionRow(Button.of(
                                        plugin.getDiscordMessageManager().resolveButtonStyle(
                                                "unlink-confirmation",
                                                ButtonStyle.DANGER
                                        ),
                                        "confirm_unlink:" + player.getUniqueId() + ":" + token,
                                        plugin.getDiscordMessageManager().resolveText(
                                                "unlink-confirmation",
                                                "button-label",
                                                "security.unlink_dm_button",
                                                80,
                                                "player", player.getName()
                                        )
                                ))
                                .queue(
                                        success -> {},
                                        error -> notifyDeliveryFailure(player.getUniqueId())
                                ),
                        error -> notifyDeliveryFailure(player.getUniqueId())
                ),
                error -> notifyDeliveryFailure(player.getUniqueId())
        );
        return true;
    }

    public boolean consumeApproval(UUID uuid, String discordId, String token) {
        return approvals.consume(uuid, discordId, token);
    }

    public void performUnlink(UUID uuid, String source, Consumer<Boolean> completion) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String discordId = plugin.getDatabaseManager().getDiscordId(uuid);
            boolean removed = discordId != null && plugin.getDatabaseManager().removePlayer(uuid);
            if (removed) plugin.getRoleManager().removePlayerRoles(discordId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (removed) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    String playerName = player.getName() == null ? uuid.toString() : player.getName();
                    plugin.getAuditLogger().log(ACCOUNT_UNLINKED, playerName, source);
                    plugin.refreshPlaceholders(uuid);
                    if (player.isOnline()) {
                        plugin.getLanguageManager().sendMessage(player.getPlayer(), "commands.unlink_success");
                    }
                }
                completion.accept(removed);
            });
        });
    }

    private void notifyDeliveryFailure(UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getLanguageManager().sendMessage(player, "security.unlink_dm_failed");
            }
        });
    }
}

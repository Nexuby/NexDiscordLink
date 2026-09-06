package net.nex.discordlink.bot;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.utils.SecurityManager.VerificationResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.IP_VERIFIED;
import static net.nex.discordlink.utils.AuditLogger.AuditEvent.VERIFICATION_FAILED;

public class SecurityBotListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public SecurityBotListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("verify_login:")) {
            String[] parts = event.getComponentId().split(":", 3);
            if (parts.length != 3) {
                event.reply(plugin.getLanguageManager().getMessage("security.verification_invalid")).setEphemeral(true).queue();
                return;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException exception) {
                event.reply(plugin.getLanguageManager().getMessage("security.verification_invalid")).setEphemeral(true).queue();
                return;
            }

            String token = parts[2];
            String discordId = event.getUser().getId();
            event.deferReply(true).queue();
            Bukkit.getScheduler().runTask(plugin, () -> verifyOnMainThread(event, uuid, discordId, token));
            return;
        }

        if (event.getComponentId().startsWith("confirm_unlink:")) {
            String[] parts = event.getComponentId().split(":", 3);
            if (parts.length != 3) {
                event.reply(plugin.getLanguageManager().getMessage("security.unlink_confirmation_invalid"))
                        .setEphemeral(true).queue();
                return;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException exception) {
                event.reply(plugin.getLanguageManager().getMessage("security.unlink_confirmation_invalid"))
                        .setEphemeral(true).queue();
                return;
            }

            String discordId = event.getUser().getId();
            if (!plugin.getUnlinkManager().consumeApproval(uuid, discordId, parts[2])) {
                event.reply(plugin.getLanguageManager().getMessage("security.unlink_confirmation_invalid"))
                        .setEphemeral(true).queue();
                return;
            }

            event.deferReply(true).queue();
            plugin.getUnlinkManager().performUnlink(uuid, "Discord DM confirmation", success -> {
                String key = success ? "security.unlink_confirmed" : "discord.command.unlink.failed";
                event.getHook().sendMessage(plugin.getLanguageManager().getMessage(key)).queue();
            });
        }
    }

    private void verifyOnMainThread(ButtonInteractionEvent event, UUID uuid, String discordId, String token) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || player.getAddress() == null || player.getAddress().getAddress() == null) {
            event.getHook().sendMessage(plugin.getLanguageManager().getMessage("security.player_not_online")).queue();
            return;
        }

        String currentIp = player.getAddress().getAddress().getHostAddress();
        VerificationResult result = plugin.getSecurityManager().validateVerification(uuid, discordId, token, currentIp);
        if (result != VerificationResult.VALID) {
            plugin.getAuditLogger().log(VERIFICATION_FAILED, player.getName(), "IP verification: " + result.name());
            event.getHook().sendMessage(messageFor(result)).queue();
            return;
        }

        if (!plugin.getDatabaseManager().updateIpAddress(uuid, plugin.getSecurityManager().protectIp(currentIp))) {
            event.getHook().sendMessage(plugin.getLanguageManager().getMessage("security.verification_save_failed")).queue();
            return;
        }

        if (!plugin.getSecurityManager().completeVerification(uuid, token)) {
            event.getHook().sendMessage(plugin.getLanguageManager().getMessage("security.verification_invalid")).queue();
            return;
        }

        plugin.getAuditLogger().log(IP_VERIFIED, player.getName(), "Network address verified");
        event.getHook().sendMessage(plugin.getLanguageManager().getMessage("security.verified_dm")).queue();
    }

    private String messageFor(VerificationResult result) {
        return switch (result) {
            case EXPIRED -> plugin.getLanguageManager().getMessage("security.verification_expired");
            case USER_MISMATCH -> plugin.getLanguageManager().getMessage("security.verification_wrong_user");
            case IP_MISMATCH -> plugin.getLanguageManager().getMessage("security.verification_ip_changed");
            default -> plugin.getLanguageManager().getMessage("security.verification_invalid");
        };
    }
}

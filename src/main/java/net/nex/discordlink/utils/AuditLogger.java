package net.nex.discordlink.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.nex.discordlink.NexDiscordLink;

import java.awt.Color;
import java.time.Instant;

public class AuditLogger {

    private final NexDiscordLink plugin;

    public AuditLogger(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public void log(AuditEvent event, String actor, String detail) {
        if (!plugin.getConfig().getBoolean("audit-log.enabled", true)) return;

        String safeActor = AuditLogSanitizer.sanitize(actor);
        String safeDetail = AuditLogSanitizer.sanitize(detail);
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(plugin.getLanguageManager().getMessage("audit.title"))
                .setColor(event.color)
                .addField(
                        plugin.getLanguageManager().getMessage("audit.field_event"),
                        plugin.getLanguageManager().getMessage("audit.event." + event.key),
                        true
                )
                .addField(plugin.getLanguageManager().getMessage("audit.field_actor"), safeActor, true)
                .addField(plugin.getLanguageManager().getMessage("audit.field_detail"), safeDetail, false)
                .setTimestamp(Instant.now());

        String channelId = plugin.getConfig().getString("audit-log.channel-id", "").trim();
        if (channelId.isEmpty()) {
            channelId = plugin.getConfig().getString("channels.log-channel-id", "").trim();
        }
        if (!channelId.isEmpty() && !channelId.startsWith("123456") && plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendEmbed(channelId, embed.build());
        }

        if (plugin.getConfig().getBoolean("audit-log.console", false)) {
            plugin.getLogger().info("AUDIT " + event.name() + " actor=" + safeActor + " detail=" + safeDetail);
        }
    }

    public enum AuditEvent {
        ACCOUNT_LINKED("account_linked", new Color(46, 204, 113)),
        ACCOUNT_UNLINKED("account_unlinked", new Color(241, 196, 15)),
        TWO_FACTOR_ENABLED("two_factor_enabled", new Color(52, 152, 219)),
        TWO_FACTOR_DISABLED("two_factor_disabled", new Color(230, 126, 34)),
        TWO_FACTOR_VERIFIED("two_factor_verified", new Color(46, 204, 113)),
        VERIFICATION_FAILED("verification_failed", new Color(231, 76, 60)),
        IP_CHALLENGE("ip_challenge", new Color(231, 76, 60)),
        IP_VERIFIED("ip_verified", new Color(46, 204, 113)),
        ROLE_SYNC("role_sync", new Color(155, 89, 182)),
        ADMIN_ACTION("admin_action", new Color(52, 73, 94));

        private final String key;
        private final Color color;

        AuditEvent(String key, Color color) {
            this.key = key;
            this.color = color;
        }
    }
}

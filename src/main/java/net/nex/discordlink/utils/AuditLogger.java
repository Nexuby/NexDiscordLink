package net.nex.discordlink.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.nex.discordlink.NexDiscordLink;

import java.awt.Color;

public class AuditLogger {

    private final NexDiscordLink plugin;

    public AuditLogger(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public void log(AuditEvent event, String actor, String detail) {
        if (!plugin.getConfig().getBoolean("audit-log.enabled", true)) return;

        String safeActor = AuditLogSanitizer.sanitize(actor);
        String safeDetail = AuditLogSanitizer.sanitize(detail);
        Color color = plugin.getDiscordMessageManager().resolveColor(
                "discord-messages.audit.event-colors." + event.key,
                event.color
        );
        EmbedBuilder embed = plugin.getDiscordMessageManager().createEmbed(
                "audit",
                "audit.title",
                null,
                color,
                null,
                null,
                "event", plugin.getLanguageManager().getMessage("audit.event." + event.key),
                "actor", safeActor,
                "detail", safeDetail
        );
        embed.addField(
                plugin.getDiscordMessageManager().resolveText("audit", "field-event", "audit.field_event", 256),
                plugin.getDiscordMessageManager().format(plugin.getLanguageManager().getMessage("audit.event." + event.key), 1024),
                true
        );
        embed.addField(
                plugin.getDiscordMessageManager().resolveText("audit", "field-actor", "audit.field_actor", 256),
                plugin.getDiscordMessageManager().format(safeActor, 1024),
                true
        );
        embed.addField(
                plugin.getDiscordMessageManager().resolveText("audit", "field-detail", "audit.field_detail", 256),
                plugin.getDiscordMessageManager().format(safeDetail, 1024),
                false
        );

        String channelId = plugin.getDiscordMessageManager().getChannelId("audit", "audit-log.channel-id");
        if (channelId.isEmpty()) {
            channelId = plugin.getConfig().getString("channels.log-channel-id", "").trim();
        }
        if (plugin.getDiscordMessageManager().isEnabled("audit", true)
                && !channelId.isEmpty()
                && !channelId.startsWith("123456")
                && plugin.getDiscordBot() != null) {
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

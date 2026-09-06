package net.nex.discordlink.utils;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.nex.discordlink.NexDiscordLink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bukkit.Bukkit;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class ConsoleAppender extends AbstractAppender {

    private static final int HTTP_TIMEOUT_MILLIS = 5000;
    private static final int MAX_QUEUED_LINES = 1000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final Pattern DISCORD_TOKEN = Pattern.compile("(?:mfa\\.[A-Za-z0-9_-]{40,}|[MN][A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{6}\\.[A-Za-z0-9_-]{20,})");
    private static final Pattern WEBHOOK_URL = Pattern.compile("https://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/api/webhooks/[^\\s]+");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile("(?i)(bot-token|password|secret|api[-_]?key)\\s*[:=]\\s*\\S+");
    private static final Pattern OTP_AUTH = Pattern.compile("otpauth://\\S+");

    private final NexDiscordLink plugin;
    private final Queue<String> logQueue = new ConcurrentLinkedQueue<>();

    public ConsoleAppender(NexDiscordLink plugin) {
        super("NexDiscordLinkAppender", null, null);
        this.plugin = plugin;
        start();

        // Start scheduler to send logs
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendLogs, 20L, 40L); // Every 2 seconds
    }

    @Override
    public void append(LogEvent event) {
        String webhookUrl = plugin.getConfig().getString("channels.console-webhook-url", "");
        String channelId = plugin.getConfig().getString("channels.console-channel-id", "");
        if ((webhookUrl == null || webhookUrl.isEmpty()) && (channelId == null || channelId.isEmpty())) return;

        String message = redact(event.getMessage().getFormattedMessage());
        // Filter out some spammy messages or color codes if needed
        // For now, just strip colors
        message = message.replaceAll("\u001B\\[[;\\d]*m", ""); // Strip ANSI colors

        String time = TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimeMillis()));
        String level = event.getLevel().name();

        logQueue.add(String.format("[%s %s]: %s", time, level, message));
        while (logQueue.size() > MAX_QUEUED_LINES) {
            logQueue.poll();
        }
    }

    private void sendLogs() {
        if (logQueue.isEmpty()) return;

        String webhookUrl = plugin.getConfig().getString("channels.console-webhook-url");
        boolean useWebhook = webhookUrl != null && !webhookUrl.isEmpty();

        if (!useWebhook && (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null)) return;

        StringBuilder batch = new StringBuilder();
        while (!logQueue.isEmpty()) {
            String log = logQueue.poll();
            if (batch.length() + log.length() + 10 > 2000) {
                // Send current batch and start new
                sendBatch(batch.toString(), useWebhook, webhookUrl);
                batch = new StringBuilder();
            }
            batch.append(log).append("\n");
        }

        if (batch.length() > 0) {
            sendBatch(batch.toString(), useWebhook, webhookUrl);
        }
    }

    private void sendBatch(String content, boolean useWebhook, String webhookUrl) {
        String finalContent = "```" + content.replace("```", "``\u200B`") + "```";

        if (useWebhook) {
            sendWebhook(webhookUrl, finalContent);
        } else {
            String channelId = plugin.getConfig().getString("channels.console-channel-id");
            if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_CONSOLE_CHANNEL_ID")) return;

            TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(finalContent)
                        .setAllowedMentions(Collections.emptyList())
                        .queue();
            }
        }
    }

    private void sendWebhook(String webhookUrl, String content) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("content", content);
            JsonObject allowedMentions = new JsonObject();
            allowedMentions.add("parse", new com.google.gson.JsonArray());
            json.add("allowed_mentions", allowedMentions);

            java.net.URL url = new java.net.URL(webhookUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
            conn.setReadTimeout(HTTP_TIMEOUT_MILLIS);
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            // Ignore errors to prevent console loops
        }
    }

    public void register() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(this);
    }

    public void unregister() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(this);
    }

    private String redact(String message) {
        String redacted = DISCORD_TOKEN.matcher(message).replaceAll("<redacted-token>");
        redacted = WEBHOOK_URL.matcher(redacted).replaceAll("<redacted-webhook>");
        redacted = SENSITIVE_ASSIGNMENT.matcher(redacted).replaceAll("$1=<redacted>");
        return OTP_AUTH.matcher(redacted).replaceAll("<redacted-otpauth-uri>");
    }
}

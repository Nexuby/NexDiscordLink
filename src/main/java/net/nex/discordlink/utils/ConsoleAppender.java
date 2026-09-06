package net.nex.discordlink.utils;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.nex.discordlink.NexDiscordLink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bukkit.Bukkit;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConsoleAppender extends AbstractAppender {

    private final NexDiscordLink plugin;
    private final Queue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

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

        String message = event.getMessage().getFormattedMessage();
        // Filter out some spammy messages or color codes if needed
        // For now, just strip colors
        message = message.replaceAll("\u001B\\[[;\\d]*m", ""); // Strip ANSI colors

        String time = dateFormat.format(new Date(event.getTimeMillis()));
        String level = event.getLevel().name();

        logQueue.add(String.format("[%s %s]: %s", time, level, message));
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
        String finalContent = "```" + content + "```";

        if (useWebhook) {
            sendWebhook(webhookUrl, finalContent);
        } else {
            String channelId = plugin.getConfig().getString("channels.console-channel-id");
            if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_CONSOLE_CHANNEL_ID")) return;

            TextChannel channel = plugin.getDiscordBot().getJda().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(finalContent).queue();
            }
        }
    }

    private void sendWebhook(String webhookUrl, String content) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("content", content);

            java.net.URL url = new java.net.URL(webhookUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
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
}

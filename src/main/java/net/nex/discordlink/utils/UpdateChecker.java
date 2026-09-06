package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final int HTTP_TIMEOUT_MILLIS = 5000;

    private final NexDiscordLink plugin;
    private final int resourceId;

    public UpdateChecker(NexDiscordLink plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
                connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);

                try (InputStream inputStream = connection.getInputStream();
                     Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
                }
            } catch (IOException exception) {
                plugin.getLogger().info("Unable to check for updates: " + exception.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}

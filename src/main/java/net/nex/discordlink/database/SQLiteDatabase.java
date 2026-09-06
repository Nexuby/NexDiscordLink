package net.nex.discordlink.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.nex.discordlink.NexDiscordLink;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SQLiteDatabase implements DatabaseManager {

    private final NexDiscordLink plugin;
    private HikariDataSource dataSource;

    public SQLiteDatabase(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean init() {
        File file = new File(plugin.getDataFolder(), "database.db");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLanguageManager().sendConsoleMessage("console.database_error");
                e.printStackTrace();
                return false;
            }
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setPoolName("NexDiscordLink-SQLite");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.setConnectionTestQuery("SELECT 1");
            config.setMaxLifetime(60000); // 60 Sec
            config.setIdleTimeout(45000); // 45 Sec
            config.setMaximumPoolSize(1); // SQLite only supports 1 concurrent writer

            dataSource = new HikariDataSource(config);

            if (!createTable()) {
                close();
                return false;
            }
            plugin.getLanguageManager().sendConsoleMessage("console.database_connected");
            return true;
        } catch (Exception e) {
            plugin.getLanguageManager().sendConsoleMessage("console.database_error");
            e.printStackTrace();
            close();
            return false;
        }
    }

    private boolean createTable() {
        String queryLink = "CREATE TABLE IF NOT EXISTS nex_discord_link (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "discord_id VARCHAR(20) NOT NULL, " +
                "ip_address VARCHAR(45), " +
                "secret_key VARCHAR(32)" +
                ");";

        String queryRewards = "CREATE TABLE IF NOT EXISTS nex_reward_history (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "count INTEGER DEFAULT 0" +
                ");";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(queryLink)) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(queryRewards)) {
                ps.execute();
            }

            // Migration for existing tables
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE nex_discord_link ADD COLUMN secret_key VARCHAR(32)")) {
                ps.execute();
            } catch (SQLException ignored) {
                // Column likely already exists
            }

            return true;
        } catch (SQLException e) {
            plugin.getLanguageManager().sendConsoleMessage("console.database_error");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public void createPlayer(UUID uuid, String discordId) {
        String query = "INSERT INTO nex_discord_link (uuid, discord_id) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET discord_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.setString(3, discordId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        String query = "DELETE FROM nex_discord_link WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDiscordId(UUID uuid) {
        String query = "SELECT discord_id FROM nex_discord_link WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public UUID getPlayerUUID(String discordId) {
        String query = "SELECT uuid FROM nex_discord_link WHERE discord_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getIpAddress(UUID uuid) {
        String query = "SELECT ip_address FROM nex_discord_link WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ip_address");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isLinked(UUID uuid) {
        return getDiscordId(uuid) != null;
    }

    @Override
    public void updateIpAddress(UUID uuid, String ip) {
        String query = "UPDATE nex_discord_link SET ip_address = ? WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getLinkRewardCount(UUID uuid) {
        String query = "SELECT count FROM nex_reward_history WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void incrementLinkRewardCount(UUID uuid) {
        String query = "INSERT INTO nex_reward_history (uuid, count) VALUES (?, 1) ON CONFLICT(uuid) DO UPDATE SET count = count + 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetLinkRewardCount(UUID uuid) {
        String query = "UPDATE nex_reward_history SET count = 0 WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetAllLinkRewardCounts() {
        String query = "UPDATE nex_reward_history SET count = 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void set2FASecret(UUID uuid, String secret) {
        String query = "UPDATE nex_discord_link SET secret_key = ? WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, secret);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String get2FASecret(UUID uuid) {
        String query = "SELECT secret_key FROM nex_discord_link WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("secret_key");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void remove2FASecret(UUID uuid) {
        set2FASecret(uuid, null);
    }
}

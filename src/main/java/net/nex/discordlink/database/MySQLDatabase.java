package net.nex.discordlink.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLDatabase implements DatabaseManager {

    private final NexDiscordLink plugin;
    private HikariDataSource dataSource;

    public MySQLDatabase(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean init() {
        FileConfiguration config = plugin.getConfig();
        HikariConfig hikariConfig = new HikariConfig();

        String host = config.getString("database-settings.host");
        int port = config.getInt("database-settings.port");
        String database = config.getString("database-settings.database");
        String username = config.getString("database-settings.username");
        String password = config.getString("database-settings.password");

        hikariConfig.setPoolName("NexDiscordLink-MySQL");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(hikariConfig);
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
                "ip_address VARCHAR(64), " +
                "secret_key TEXT, " +
                "linked_at BIGINT DEFAULT 0" +
                ");";

        String queryRewards = "CREATE TABLE IF NOT EXISTS nex_reward_history (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "count INTEGER DEFAULT 0" +
                ");";
        String queryCodes = "CREATE TABLE IF NOT EXISTS nex_link_codes (" +
                "code VARCHAR(12) PRIMARY KEY, " +
                "uuid VARCHAR(36) UNIQUE NOT NULL, " +
                "server_id VARCHAR(64) NOT NULL, " +
                "expires_at BIGINT NOT NULL" +
                ");";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(queryLink)) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(queryRewards)) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement(queryCodes)) {
                ps.execute();
            }

            if (!hasUniqueDiscordIdIndex(conn)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "CREATE UNIQUE INDEX idx_nex_discord_link_discord_id " +
                        "ON nex_discord_link(discord_id)")) {
                    ps.execute();
                }
            }

            // Migration for existing tables
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE nex_discord_link ADD COLUMN secret_key VARCHAR(32)")) {
                ps.execute();
            } catch (SQLException ignored) {
                // Column likely already exists
            }

            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE nex_discord_link ADD COLUMN linked_at BIGINT DEFAULT 0")) {
                ps.execute();
            } catch (SQLException ignored) {
                // Column likely already exists
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "ALTER TABLE nex_discord_link MODIFY COLUMN ip_address VARCHAR(64), MODIFY COLUMN secret_key TEXT")) {
                ps.execute();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLanguageManager().sendConsoleMessage("console.database_error");
            e.printStackTrace();
            return false;
        }
    }

    private boolean hasUniqueDiscordIdIndex(Connection conn) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(conn.getCatalog(), null, "nex_discord_link", true, false)) {
            while (indexes.next()) {
                if ("discord_id".equalsIgnoreCase(indexes.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public boolean ping() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet ignored = ps.executeQuery()) {
            return true;
        } catch (SQLException | RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean createPlayer(UUID uuid, String discordId) {
        String query = "INSERT INTO nex_discord_link (uuid, discord_id, linked_at) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.setLong(3, System.currentTimeMillis());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not create account link: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removePlayer(UUID uuid) {
        String query = "DELETE FROM nex_discord_link WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
    public long getLinkedAt(UUID uuid) {
        String query = "SELECT linked_at FROM nex_discord_link WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("linked_at") : 0L;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not read account link timestamp: " + exception.getMessage());
            return 0L;
        }
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
    public boolean updateIpAddress(UUID uuid, String ip) {
        String query = "UPDATE nex_discord_link SET ip_address = ? WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean createLinkCode(String code, UUID uuid, String serverId, long expiresAt) {
        String query = "INSERT INTO nex_link_codes (code, uuid, server_id, expires_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, code);
            ps.setString(2, uuid.toString());
            ps.setString(3, serverId);
            ps.setLong(4, expiresAt);
            return ps.executeUpdate() == 1;
        } catch (SQLException exception) {
            return false;
        }
    }

    @Override
    public String getActiveLinkCode(UUID uuid, long now) {
        String query = "SELECT code FROM nex_link_codes WHERE uuid = ? AND expires_at > ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("code") : null;
            }
        } catch (SQLException exception) {
            return null;
        }
    }

    @Override
    public UUID getLinkCodeOwner(String code, long now) {
        String query = "SELECT uuid FROM nex_link_codes WHERE code = ? AND expires_at > ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, code);
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
            }
        } catch (SQLException | IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public void removeLinkCode(String code, UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nex_link_codes WHERE code = ? AND uuid = ?")) {
            ps.setString(1, code);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not remove shared link code: " + exception.getMessage());
        }
    }

    @Override
    public void deleteExpiredLinkCodes(long now) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nex_link_codes WHERE expires_at <= ?")) {
            ps.setLong(1, now);
            ps.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not clean expired link codes: " + exception.getMessage());
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
        String query = "INSERT INTO nex_reward_history (uuid, count) VALUES (?, 1) ON DUPLICATE KEY UPDATE count = count + 1";
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
    public boolean set2FASecret(UUID uuid, String secret) {
        String query = "UPDATE nex_discord_link SET secret_key = ? WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, secret);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
    public boolean remove2FASecret(UUID uuid) {
        return set2FASecret(uuid, null);
    }
}

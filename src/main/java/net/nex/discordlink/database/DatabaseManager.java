package net.nex.discordlink.database;

import java.util.UUID;

public interface DatabaseManager {
    boolean init();
    void close();
    boolean ping();

    boolean createPlayer(UUID uuid, String discordId);
    void removePlayer(UUID uuid);
    String getDiscordId(UUID uuid);
    long getLinkedAt(UUID uuid);
    UUID getPlayerUUID(String discordId);
    String getIpAddress(UUID uuid);
    boolean isLinked(UUID uuid);
    boolean updateIpAddress(UUID uuid, String ip);

    int getLinkRewardCount(UUID uuid);
    void incrementLinkRewardCount(UUID uuid);
    void resetLinkRewardCount(UUID uuid);
    void resetAllLinkRewardCounts();

    // 2FA Methods
    boolean set2FASecret(UUID uuid, String secret);
    String get2FASecret(UUID uuid);
    boolean remove2FASecret(UUID uuid);
}

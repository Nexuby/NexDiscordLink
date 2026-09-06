package net.nex.discordlink.database;

import java.util.UUID;

public interface DatabaseManager {
    boolean init();
    void close();
    boolean ping();

    boolean createPlayer(UUID uuid, String discordId);
    boolean removePlayer(UUID uuid);
    String getDiscordId(UUID uuid);
    long getLinkedAt(UUID uuid);
    UUID getPlayerUUID(String discordId);
    String getIpAddress(UUID uuid);
    boolean isLinked(UUID uuid);
    boolean updateIpAddress(UUID uuid, String ip);

    boolean createLinkCode(String code, UUID uuid, String serverId, long expiresAt);
    String getActiveLinkCode(UUID uuid, long now);
    UUID getLinkCodeOwner(String code, long now);
    void removeLinkCode(String code, UUID uuid);
    void deleteExpiredLinkCodes(long now);

    int getLinkRewardCount(UUID uuid);
    void incrementLinkRewardCount(UUID uuid);
    void resetLinkRewardCount(UUID uuid);
    void resetAllLinkRewardCounts();

    // 2FA Methods
    boolean set2FASecret(UUID uuid, String secret);
    String get2FASecret(UUID uuid);
    boolean remove2FASecret(UUID uuid);
}

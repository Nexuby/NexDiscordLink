package net.nex.discordlink.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnlinkApprovalStoreTest {

    @Test
    void approvalIsSingleUseAndBoundToDiscordAccount() {
        AtomicLong clock = new AtomicLong(1_000L);
        UnlinkApprovalStore store = new UnlinkApprovalStore(clock::get, () -> "token");
        UUID uuid = UUID.randomUUID();

        store.create(uuid, "discord-a");
        assertFalse(store.consume(uuid, "discord-b", "token"));
        assertTrue(store.consume(uuid, "discord-a", "token"));
        assertFalse(store.consume(uuid, "discord-a", "token"));
    }

    @Test
    void approvalExpiresAfterTwoMinutes() {
        AtomicLong clock = new AtomicLong(1_000L);
        UnlinkApprovalStore store = new UnlinkApprovalStore(clock::get, () -> "token");
        UUID uuid = UUID.randomUUID();

        store.create(uuid, "discord-a");
        clock.addAndGet(120_001L);
        assertFalse(store.consume(uuid, "discord-a", "token"));
    }
}

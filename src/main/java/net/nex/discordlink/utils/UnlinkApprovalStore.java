package net.nex.discordlink.utils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class UnlinkApprovalStore {

    private static final long EXPIRY_MILLIS = Duration.ofMinutes(2).toMillis();
    private final Map<UUID, PendingApproval> pending = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final Supplier<String> tokenFactory;

    public UnlinkApprovalStore() {
        SecureRandom random = new SecureRandom();
        this.clock = System::currentTimeMillis;
        this.tokenFactory = () -> {
            byte[] bytes = new byte[18];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        };
    }

    UnlinkApprovalStore(LongSupplier clock, Supplier<String> tokenFactory) {
        this.clock = clock;
        this.tokenFactory = tokenFactory;
    }

    public String create(UUID uuid, String discordId) {
        String token = tokenFactory.get();
        pending.put(uuid, new PendingApproval(discordId, token, clock.getAsLong() + EXPIRY_MILLIS));
        return token;
    }

    public boolean consume(UUID uuid, String discordId, String token) {
        PendingApproval approval = pending.get(uuid);
        if (approval == null
                || approval.expiresAt < clock.getAsLong()
                || !approval.discordId.equals(discordId)
                || !approval.token.equals(token)) {
            if (approval != null && approval.expiresAt < clock.getAsLong()) pending.remove(uuid, approval);
            return false;
        }
        return pending.remove(uuid, approval);
    }

    private record PendingApproval(String discordId, String token, long expiresAt) {
    }
}

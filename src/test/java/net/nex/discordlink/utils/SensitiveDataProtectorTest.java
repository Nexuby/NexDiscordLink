package net.nex.discordlink.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataProtectorTest {

    private final SensitiveDataProtector protector = new SensitiveDataProtector(testKey());

    @Test
    void encryptsAndDecryptsWithUniqueCiphertexts() {
        String first = protector.encrypt("JBSWY3DPEHPK3PXP");
        String second = protector.encrypt("JBSWY3DPEHPK3PXP");

        assertTrue(protector.isEncrypted(first));
        assertNotEquals(first, second);
        assertEquals("JBSWY3DPEHPK3PXP", protector.decrypt(first));
        assertEquals("JBSWY3DPEHPK3PXP", protector.decrypt(second));
    }

    @Test
    void fingerprintsIpAddressesDeterministically() {
        String fingerprint = protector.fingerprintIp("203.0.113.10");

        assertTrue(protector.isIpFingerprint(fingerprint));
        assertTrue(protector.matchesIp(fingerprint, "203.0.113.10"));
        assertFalse(protector.matchesIp(fingerprint, "203.0.113.11"));
    }

    @Test
    void acceptsLegacyPlaintextIpOnlyWhenItMatches() {
        assertTrue(protector.matchesIp("203.0.113.10", "203.0.113.10"));
        assertFalse(protector.matchesIp("203.0.113.10", "203.0.113.11"));
    }

    private byte[] testKey() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        return key;
    }
}

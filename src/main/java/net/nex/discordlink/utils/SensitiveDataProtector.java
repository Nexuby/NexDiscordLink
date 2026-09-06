package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

public class SensitiveDataProtector {

    private static final String ENVIRONMENT_KEY = "NEXDISCORDLINK_MASTER_KEY";
    private static final String ENCRYPTED_PREFIX = "enc:v1:";
    private static final String IP_PREFIX = "h1:";
    private static final int KEY_BYTES = 32;
    private static final int GCM_NONCE_BYTES = 12;

    private final SecretKeySpec encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SensitiveDataProtector(NexDiscordLink plugin) {
        try {
            this.encryptionKey = new SecretKeySpec(loadOrCreateKey(plugin), "AES");
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not initialize sensitive data protection", exception);
        }
    }

    SensitiveDataProtector(byte[] key) {
        if (key == null || key.length != KEY_BYTES) {
            throw new IllegalArgumentException("Encryption key must be exactly " + KEY_BYTES + " bytes");
        }
        this.encryptionKey = new SecretKeySpec(key.clone(), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[GCM_NONCE_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(ciphertext, 0, payload, nonce.length, ciphertext.length);
            return ENCRYPTED_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt sensitive data", exception);
        }
    }

    public String decrypt(String storedValue) {
        if (!isEncrypted(storedValue)) return storedValue;

        try {
            byte[] payload = Base64.getUrlDecoder().decode(storedValue.substring(ENCRYPTED_PREFIX.length()));
            if (payload.length <= GCM_NONCE_BYTES) {
                throw new IllegalArgumentException("Encrypted payload is too short");
            }

            byte[] nonce = new byte[GCM_NONCE_BYTES];
            byte[] ciphertext = new byte[payload.length - GCM_NONCE_BYTES];
            System.arraycopy(payload, 0, nonce, 0, nonce.length);
            System.arraycopy(payload, nonce.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not decrypt sensitive data", exception);
        }
    }

    public boolean isEncrypted(String storedValue) {
        return storedValue != null && storedValue.startsWith(ENCRYPTED_PREFIX);
    }

    public String fingerprintIp(String ipAddress) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(encryptionKey.getEncoded(), "HmacSHA256"));
            byte[] digest = mac.doFinal(ipAddress.getBytes(StandardCharsets.UTF_8));
            return IP_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not protect IP address", exception);
        }
    }

    public boolean matchesIp(String storedValue, String ipAddress) {
        if (storedValue == null) return false;
        if (!storedValue.startsWith(IP_PREFIX)) {
            return MessageDigest.isEqual(
                    storedValue.getBytes(StandardCharsets.UTF_8),
                    ipAddress.getBytes(StandardCharsets.UTF_8)
            );
        }

        return MessageDigest.isEqual(
                storedValue.getBytes(StandardCharsets.UTF_8),
                fingerprintIp(ipAddress).getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean isIpFingerprint(String storedValue) {
        return storedValue != null && storedValue.startsWith(IP_PREFIX);
    }

    private byte[] loadOrCreateKey(NexDiscordLink plugin) throws IOException {
        String environmentValue = System.getenv(ENVIRONMENT_KEY);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return decodeKey(environmentValue.trim());
        }

        Path dataDirectory = plugin.getDataFolder().toPath();
        Files.createDirectories(dataDirectory);
        Path keyFile = dataDirectory.resolve("secret.key");

        if (Files.exists(keyFile)) {
            return decodeKey(Files.readString(keyFile, StandardCharsets.UTF_8).trim());
        }

        byte[] key = new byte[KEY_BYTES];
        secureRandom.nextBytes(key);
        String encoded = Base64.getEncoder().encodeToString(key);
        Files.writeString(
                keyFile,
                encoded + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
        restrictPermissions(keyFile);
        return key;
    }

    private byte[] decodeKey(String encoded) {
        byte[] key = Base64.getDecoder().decode(encoded);
        if (key.length != KEY_BYTES) {
            throw new IllegalArgumentException(ENVIRONMENT_KEY + " must decode to exactly " + KEY_BYTES + " bytes");
        }
        return key;
    }

    private void restrictPermissions(Path keyFile) {
        try {
            Files.setPosixFilePermissions(keyFile, Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows and some file systems do not expose POSIX permissions.
        }
    }
}

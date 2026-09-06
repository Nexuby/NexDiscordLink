package net.nex.discordlink.commands;

import java.util.Locale;
import java.util.Set;

public final class CommandAliases {

    private static final Set<String> RELOAD = Set.of(
            "reload", "yenile", "yenidenyukle", "yenidenyükle"
    );
    private static final Set<String> RESET_REWARD = Set.of(
            "resetreward", "odulsifirla", "ödülsıfırla"
    );
    private static final Set<String> STATUS = Set.of(
            "status", "durum", "kontrol"
    );
    private static final Set<String> SYNC = Set.of(
            "sync", "senkronize", "esitle", "eşitle"
    );
    private static final Set<String> TWO_FACTOR_COMMANDS = Set.of(
            "2fa", "ikifaktor", "ikifaktör", "ikiasamali", "ikiaşamalı"
    );
    private static final Set<String> TWO_FACTOR_SETUP = Set.of(
            "setup", "kur", "kurulum"
    );
    private static final Set<String> TWO_FACTOR_VERIFY = Set.of(
            "verify", "login", "dogrula", "doğrula", "giris", "giriş"
    );
    private static final Set<String> TWO_FACTOR_DISABLE = Set.of(
            "disable", "kapat", "devredisi", "devredışı"
    );

    private CommandAliases() {
    }

    public static boolean isReload(String value) {
        return RELOAD.contains(normalize(value));
    }

    public static boolean isResetReward(String value) {
        return RESET_REWARD.contains(normalize(value));
    }

    public static boolean isStatus(String value) {
        return STATUS.contains(normalize(value));
    }

    public static boolean isSync(String value) {
        return SYNC.contains(normalize(value));
    }

    public static TwoFactorAction getTwoFactorAction(String value) {
        String normalized = normalize(value);
        if (TWO_FACTOR_SETUP.contains(normalized)) return TwoFactorAction.SETUP;
        if (TWO_FACTOR_VERIFY.contains(normalized)) return TwoFactorAction.VERIFY;
        if (TWO_FACTOR_DISABLE.contains(normalized)) return TwoFactorAction.DISABLE;
        return TwoFactorAction.UNKNOWN;
    }

    public static boolean isAllowedDuringTwoFactorVerification(String commandLine) {
        if (commandLine == null || !commandLine.startsWith("/")) return false;
        String withoutSlash = commandLine.substring(1).trim();
        if (withoutSlash.isEmpty()) return false;
        String rootCommand = withoutSlash.split("\\s+", 2)[0];
        return TWO_FACTOR_COMMANDS.contains(normalize(rootCommand))
                || "login".equals(normalize(rootCommand))
                || "verify".equals(normalize(rootCommand));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public enum TwoFactorAction {
        SETUP,
        VERIFY,
        DISABLE,
        UNKNOWN
    }
}

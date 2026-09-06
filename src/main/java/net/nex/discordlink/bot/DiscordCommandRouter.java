package net.nex.discordlink.bot;

import java.util.Locale;
import java.util.Set;

public final class DiscordCommandRouter {

    private static final Set<String> PROFILE = Set.of("profile", "hesap");
    private static final Set<String> LINK = Set.of("link", "eşle");
    private static final Set<String> UNLINK = Set.of("unlink", "eşlemeyi-kaldır");
    private static final Set<String> HELP = Set.of("help", "yardım");

    private DiscordCommandRouter() {
    }

    public static CommandType resolve(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (PROFILE.contains(normalized)) return CommandType.PROFILE;
        if (LINK.contains(normalized)) return CommandType.LINK;
        if (UNLINK.contains(normalized)) return CommandType.UNLINK;
        if (HELP.contains(normalized)) return CommandType.HELP;
        return CommandType.UNKNOWN;
    }

    public enum CommandType {
        PROFILE,
        LINK,
        UNLINK,
        HELP,
        UNKNOWN
    }
}

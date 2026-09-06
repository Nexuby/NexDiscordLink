package net.nex.discordlink.bot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ConsoleCommandPolicy {

    private ConsoleCommandPolicy() {
    }

    public static Optional<ParsedCommand> parse(String rawCommand) {
        if (rawCommand == null) return Optional.empty();

        String command = rawCommand.trim();
        if (command.isEmpty() || command.startsWith("/") || command.contains("\n") || command.contains("\r")) {
            return Optional.empty();
        }

        String commandName = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return Optional.of(new ParsedCommand(command, commandName));
    }

    public static boolean isAllowed(String commandName, List<String> whitelist) {
        if (commandName == null || whitelist == null) return false;
        return whitelist.stream()
                .map(entry -> entry.toLowerCase(Locale.ROOT).trim())
                .anyMatch(entry -> entry.equals(commandName));
    }

    public record ParsedCommand(String command, String commandName) {
    }
}

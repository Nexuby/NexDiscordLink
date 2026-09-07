package net.nex.discordlink.utils;

import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DoctorService {

    private static final Set<String> DYNAMIC_CONFIG_PREFIXES = Set.of(
            "sync.role-sync.vault-groups.",
            "rewards.link-rewards.discord-role-commands."
    );
    private static final Set<String> BUTTON_STYLES = Set.of("PRIMARY", "SECONDARY", "SUCCESS", "DANGER");

    private final NexDiscordLink plugin;

    public DoctorService(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public void runAutomaticChecks() {
        ConsoleCommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(message("doctor.started"));
        printReport(console, "config.yml", inspectConfig());
        printReport(console, "lang", inspectLanguage());
    }

    private Report inspectConfig() {
        Report report = new Report();
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration user = loadFile(file, report, "doctor.file_missing", "doctor.yaml_invalid");
        YamlConfiguration bundled = loadResource("config.yml", report);
        if (user == null || bundled == null) return report;

        compareSchema(user, bundled, report, "doctor.config_missing_keys", "doctor.config_unknown_keys");
        validateTypes(user, bundled, report);

        ConfigurationSection config = plugin.getConfig();
        validateToken(config, report);
        validateEnums(config, report);
        validateNumbers(config, report);
        validateDiscordIds(config, report);
        validateUrlsAndColors(config, report);
        validateFeatureDependencies(config, report);
        return report;
    }

    private Report inspectLanguage() {
        Report report = new Report();
        String language = plugin.getConfigManager().getLanguage().trim().toLowerCase(Locale.ROOT);
        File selectedFile = new File(plugin.getDataFolder(), "lang/messages_" + language + ".yml");
        YamlConfiguration selected = loadFile(selectedFile, report, "doctor.language_file_missing", "doctor.language_yaml_invalid");
        YamlConfiguration expected = loadResource("lang/messages_en.yml", report);
        if (selected == null || expected == null) return report;

        compareSchema(selected, expected, report, "doctor.language_missing_keys", "doctor.language_unknown_keys");
        validateLanguageValues(selected, expected, report);

        if (!language.equals("en")) {
            File fallbackFile = new File(plugin.getDataFolder(), "lang/messages_en.yml");
            loadFile(fallbackFile, report, "doctor.fallback_file_missing", "doctor.fallback_yaml_invalid");
        }
        return report;
    }

    private void compareSchema(
            YamlConfiguration actual,
            YamlConfiguration expected,
            Report report,
            String missingMessage,
            String unknownMessage
    ) {
        Set<String> actualKeys = leafKeys(actual);
        Set<String> expectedKeys = leafKeys(expected);

        Set<String> missing = new LinkedHashSet<>(expectedKeys);
        missing.removeAll(actualKeys);
        if (!missing.isEmpty()) {
            report.warning(message(
                    missingMessage,
                    "count", Integer.toString(missing.size()),
                    "examples", DoctorChecks.examples(missing, 6)
            ));
        }

        Set<String> unknown = new LinkedHashSet<>(actualKeys);
        unknown.removeAll(expectedKeys);
        unknown.removeIf(this::isDynamicConfigPath);
        if (!unknown.isEmpty()) {
            report.warning(message(
                    unknownMessage,
                    "count", Integer.toString(unknown.size()),
                    "examples", DoctorChecks.examples(unknown, 6)
            ));
        }
    }

    private void validateTypes(YamlConfiguration actual, YamlConfiguration expected, Report report) {
        for (String key : leafKeys(expected)) {
            if (!actual.contains(key)) continue;
            Object actualValue = actual.get(key);
            Object expectedValue = expected.get(key);
            String actualKind = DoctorChecks.valueKind(actualValue);
            String expectedKind = DoctorChecks.valueKind(expectedValue);
            if (!actualKind.equals(expectedKind)) {
                report.error(message(
                        "doctor.config_wrong_type",
                        "path", key,
                        "expected", expectedKind,
                        "actual", actualKind
                ));
            }
        }
    }

    private void validateToken(ConfigurationSection config, Report report) {
        String token = string(config, "bot-token", "");
        if (token.isEmpty() || token.equalsIgnoreCase("YOUR_BOT_TOKEN_HERE")) {
            report.error(message("doctor.bot_token_missing"));
        }
    }

    private void validateEnums(ConfigurationSection config, Report report) {
        validateEnum(config, report, "database-settings.type", Set.of("SQLITE", "MYSQL"));
        validateEnum(config, report, "link-system.type", Set.of("DM", "MODAL", "BOTH"));
        validateEnum(config, report, "link-system.account-policy", Set.of("ONE_TO_ONE"));
        validateEnum(config, report, "sync.role-sync.direction", Set.of(
                "MINECRAFT_TO_DISCORD", "DISCORD_TO_MINECRAFT", "BIDIRECTIONAL"
        ));

        for (String key : leafKeys(config)) {
            if (!key.endsWith("button-style")) continue;
            String value = string(config, key, "");
            if (!value.isEmpty() && !BUTTON_STYLES.contains(value.toUpperCase(Locale.ROOT))) {
                report.error(message("doctor.invalid_enum", "path", key));
            }
        }
    }

    private void validateEnum(ConfigurationSection config, Report report, String path, Set<String> allowed) {
        String value = string(config, path, "").toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) report.error(message("doctor.invalid_enum", "path", path));
    }

    private void validateNumbers(ConfigurationSection config, Report report) {
        validateRange(config, report, "database-settings.port", 1, 65535);
        validateRange(config, report, "link-system.code-length", 4, 12);
        validateRange(config, report, "link-system.code-expiry-minutes", 1, 1440);
        validateRange(config, report, "proxy.link-check-interval-ticks", 20, 72000);
        validateRange(config, report, "rewards.salary-interval", 1, 525600);
        validateRange(config, report, "sync.role-sync.interval-minutes", 1, 525600);
    }

    private void validateRange(ConfigurationSection config, Report report, String path, int min, int max) {
        if (!config.isInt(path) && !config.isLong(path)) {
            report.error(message("doctor.invalid_number", "path", path));
            return;
        }
        long value = config.getLong(path);
        if (value < min || value > max) report.error(message("doctor.invalid_number", "path", path));
    }

    private void validateDiscordIds(ConfigurationSection config, Report report) {
        for (String key : leafKeys(config)) {
            boolean idPath = key.endsWith("channel-id")
                    || key.endsWith("role-id")
                    || key.startsWith("sync.role-sync.vault-groups.");
            if (!idPath) continue;
            String value = string(config, key, "");
            if (!value.isEmpty() && !DoctorChecks.isDiscordId(value)) {
                report.error(message("doctor.invalid_discord_id", "path", key));
            } else if (DoctorChecks.looksLikeExampleDiscordId(value)) {
                report.error(message("doctor.example_discord_id", "path", key));
            }
        }

        ConfigurationSection roleCommands = config.getConfigurationSection(
                "rewards.link-rewards.discord-role-commands"
        );
        if (roleCommands != null) {
            for (String roleId : roleCommands.getKeys(false)) {
                if (!DoctorChecks.isDiscordId(roleId) || DoctorChecks.looksLikeExampleDiscordId(roleId)) {
                    report.error(message("doctor.invalid_reward_role_id"));
                }
            }
        }
    }

    private void validateUrlsAndColors(ConfigurationSection config, Report report) {
        for (String key : leafKeys(config)) {
            String value = string(config, key, "");
            if ((key.endsWith("url") || key.endsWith("webhook-url"))
                    && !value.isEmpty()
                    && DiscordMessageFormatter.safeHttpUrl(DoctorChecks.replacePlaceholdersForValidation(value)) == null) {
                report.error(message("doctor.invalid_url", "path", key));
            }
            if ((key.endsWith(".color") || key.contains(".event-colors."))
                    && !DiscordMessageFormatter.isValidColor(value)) {
                report.error(message("doctor.invalid_color", "path", key));
            }
        }
    }

    private void validateFeatureDependencies(ConfigurationSection config, Report report) {
        String databaseType = string(config, "database-settings.type", "sqlite");
        if (config.getBoolean("proxy.enabled") && !databaseType.equalsIgnoreCase("mysql")) {
            report.error(message("doctor.proxy_requires_mysql"));
        }
        if (databaseType.equalsIgnoreCase("mysql")) {
            for (String path : List.of("database-settings.host", "database-settings.database", "database-settings.username")) {
                if (string(config, path, "").isBlank()) report.error(message("doctor.required_setting_empty", "path", path));
            }
            if (string(config, "database-settings.password", "").isBlank()) {
                report.warning(message("doctor.mysql_password_empty"));
            }
        }

        if (config.getBoolean("chat-bridge.enabled")) {
            String channel = string(config, "chat-bridge.channel-id", "");
            String webhook = string(config, "chat-bridge.webhook-url", "");
            if (channel.isEmpty() && webhook.isEmpty()) report.error(message("doctor.chat_destination_missing"));
        }
        if (config.getBoolean("console-command.enabled") && config.getStringList("console-command.whitelist").isEmpty()) {
            report.warning(message("doctor.console_whitelist_empty"));
        }
    }

    private void validateLanguageValues(YamlConfiguration selected, YamlConfiguration expected, Report report) {
        for (String key : leafKeys(expected)) {
            if (!selected.contains(key)) continue;
            Object selectedValue = selected.get(key);
            Object expectedValue = expected.get(key);
            String selectedKind = DoctorChecks.valueKind(selectedValue);
            String expectedKind = DoctorChecks.valueKind(expectedValue);
            if (!selectedKind.equals(expectedKind)) {
                report.error(message("doctor.language_wrong_type", "path", key));
                continue;
            }

            Set<String> required = DoctorChecks.placeholders(expectedValue);
            Set<String> present = DoctorChecks.placeholders(selectedValue);
            Set<String> missing = new LinkedHashSet<>(required);
            missing.removeAll(present);
            if (!missing.isEmpty()) {
                report.error(message(
                        "doctor.language_missing_placeholders",
                        "path", key,
                        "placeholders", String.join(", ", missing)
                ));
            }
            Set<String> extra = new LinkedHashSet<>(present);
            extra.removeAll(required);
            if (!extra.isEmpty()) {
                report.warning(message(
                        "doctor.language_extra_placeholders",
                        "path", key,
                        "placeholders", String.join(", ", extra)
                ));
            }
        }
    }

    private YamlConfiguration loadFile(File file, Report report, String missingKey, String invalidKey) {
        if (!file.isFile()) {
            report.error(message(missingKey, "file", file.getName()));
            return null;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            report.error(message(invalidKey, "file", file.getName()));
            return null;
        }
    }

    private YamlConfiguration loadResource(String path, Report report) {
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                report.error(message("doctor.bundled_resource_missing", "file", path));
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            report.error(message("doctor.bundled_resource_missing", "file", path));
            return null;
        }
    }

    private Set<String> leafKeys(ConfigurationSection configuration) {
        Set<String> keys = new LinkedHashSet<>();
        for (String key : configuration.getKeys(true)) {
            if (!configuration.isConfigurationSection(key)) keys.add(key);
        }
        return keys;
    }

    private boolean isDynamicConfigPath(String path) {
        return DYNAMIC_CONFIG_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String string(ConfigurationSection configuration, String path, String fallback) {
        Object value = configuration.get(path);
        return value instanceof String string ? string.trim() : value == null ? fallback : "";
    }

    private void printReport(ConsoleCommandSender console, String name, Report report) {
        for (Finding finding : report.findings) {
            String severity = message(finding.error ? "doctor.severity_error" : "doctor.severity_warning");
            console.sendMessage(message("doctor.finding", "severity", severity, "message", finding.message));
        }
        String resultKey = report.findings.isEmpty() ? "doctor.result_healthy" : "doctor.result_summary";
        console.sendMessage(message(
                resultKey,
                "name", name,
                "errors", Integer.toString(report.errors),
                "warnings", Integer.toString(report.warnings)
        ));
    }

    private String message(String key, String... placeholders) {
        return plugin.getLanguageManager().getMessage(key, placeholders);
    }

    private static final class Report {
        private final List<Finding> findings = new ArrayList<>();
        private int errors;
        private int warnings;

        private void error(String message) {
            findings.add(new Finding(true, message));
            errors++;
        }

        private void warning(String message) {
            findings.add(new Finding(false, message));
            warnings++;
        }
    }

    private record Finding(boolean error, String message) {
    }
}

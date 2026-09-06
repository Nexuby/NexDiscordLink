package net.nex.discordlink;

import net.nex.discordlink.config.ConfigManager;
import net.nex.discordlink.config.LanguageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class NexDiscordLink extends JavaPlugin {

    private static NexDiscordLink instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private net.nex.discordlink.database.DatabaseManager databaseManager;
    private net.nex.discordlink.bot.DiscordBot discordBot;
    private net.nex.discordlink.utils.SecurityManager securityManager;
    private net.nex.discordlink.utils.SyncManager syncManager;
    private net.nex.discordlink.utils.LinkManager linkManager;
    private net.nex.discordlink.utils.RoleManager roleManager;
    private net.nex.discordlink.utils.TwoFactorManager twoFactorManager;
    private net.nex.discordlink.utils.SensitiveDataProtector sensitiveDataProtector;
    private net.nex.discordlink.utils.ConsoleAppender consoleAppender;
    private net.nex.discordlink.utils.AuditLogger auditLogger;
    private net.nex.discordlink.integrations.NexDiscordPlaceholderExpansion placeholderExpansion;
    private BukkitTask rewardTask;
    private BukkitTask roleSyncTask;
    private int botGeneration;

    @Override
    public void onEnable() {
        instance = this;

        // Load Config
        this.configManager = new ConfigManager(this);

        // Load Language
        this.languageManager = new LanguageManager(this);

        languageManager.sendConsoleMessage("console.loading_config");
        languageManager.sendConsoleMessage("console.loading_lang");

        // Connect to Database
        if (!setupDatabase()) {
            languageManager.sendConsoleMessage("console.database_fatal");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Managers
        try {
            this.sensitiveDataProtector = new net.nex.discordlink.utils.SensitiveDataProtector(this);
        } catch (IllegalStateException exception) {
            getLogger().severe(exception.getMessage());
            languageManager.sendConsoleMessage("console.security_storage_fatal");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.securityManager = new net.nex.discordlink.utils.SecurityManager(this, sensitiveDataProtector);
        this.syncManager = new net.nex.discordlink.utils.SyncManager(this);
        this.linkManager = new net.nex.discordlink.utils.LinkManager(this);
        this.roleManager = new net.nex.discordlink.utils.RoleManager(this);
        this.twoFactorManager = new net.nex.discordlink.utils.TwoFactorManager(this, sensitiveDataProtector);
        this.auditLogger = new net.nex.discordlink.utils.AuditLogger(this);

        // Initialize Console Appender
        this.consoleAppender = new net.nex.discordlink.utils.ConsoleAppender(this);
        this.consoleAppender.register();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new net.nex.discordlink.listeners.SecurityListener(this, securityManager), this);
        getServer().getPluginManager().registerEvents(new net.nex.discordlink.listeners.SyncListener(this, syncManager), this);
        getServer().getPluginManager().registerEvents(new net.nex.discordlink.listeners.GameEventsListener(this), this);
        getServer().getPluginManager().registerEvents(new net.nex.discordlink.listeners.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new net.nex.discordlink.listeners.UpdateListener(this, 130935), this);

        // Register Commands
        getCommand("nexdiscord").setExecutor(new net.nex.discordlink.commands.MainCommand(this));
        getCommand("link").setExecutor(new net.nex.discordlink.commands.LinkCommand(this));
        getCommand("unlink").setExecutor(new net.nex.discordlink.commands.UnlinkCommand(this));
        getCommand("2fa").setExecutor(new net.nex.discordlink.commands.TwoFactorCommand(this));
        getCommand("linkstatus").setExecutor(new net.nex.discordlink.commands.AccountStatusCommand(this));
        getCommand("linkreward").setExecutor(new net.nex.discordlink.commands.LinkRewardCommand(this));

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new net.nex.discordlink.integrations.NexDiscordPlaceholderExpansion(this);
            placeholderExpansion.register();
            getServer().getPluginManager().registerEvents(placeholderExpansion, this);
        }

        scheduleRewards();
        scheduleRoleSync();

        // Initialize bStats
        int pluginId = 28456; // Replace with your own plugin ID
        new org.bstats.bukkit.Metrics(this, pluginId);

        startDiscordBot(null, false);
    }

    private void printStartupMessage() {
        org.bukkit.command.ConsoleCommandSender sender = org.bukkit.Bukkit.getConsoleSender();
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  _   _           _____  _                       _ _     _       _    "));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b | \\ | |         |  __ \\(_)                     | | |   (_)     | |   "));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b |  \\| | _____  _| |  | |_ ___  ___ ___  _ __ __| | |    _ _ __ | | __"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b | . ` |/ _ \\ \\/ / |  | | / __|/ __/ _ \\| '__/ _` | |   | | '_ \\| |/ /"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b | |\\  |  __/>  <| |__| | \\__ \\ (_| (_) | | | (_| | |___| | | | |   < "));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b |_| \\_|\\___/_/\\_\\_____/|_|___/\\___\\___/|_|  \\__,_|______|_|_| |_|_|\\_\\"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b                                                                       "));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f NexDiscordLink &7v" + getDescription().getVersion() + " &fby &bNexuby"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f"));
        sender.sendMessage(languageManager.getMessage("console.startup_config_loaded"));
        sender.sendMessage(languageManager.getMessage("console.startup_lang_loaded"));
        sender.sendMessage(languageManager.getMessage("console.startup_database_connected"));
        sender.sendMessage(languageManager.getMessage("console.startup_bot_started"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f"));
        sender.sendMessage(languageManager.getMessage("console.startup_ready"));
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f"));
    }

    private boolean setupDatabase() {
        String type = getConfig().getString("database-settings.type", "sqlite");
        if (type.equalsIgnoreCase("mysql")) {
            this.databaseManager = new net.nex.discordlink.database.MySQLDatabase(this);
        } else {
            this.databaseManager = new net.nex.discordlink.database.SQLiteDatabase(this);
        }
        return this.databaseManager.init();
    }

    public void reloadPlugin(CommandSender sender) {
        languageManager.sendMessage(sender, "commands.reload_started");

        botGeneration++;
        if (discordBot != null) {
            discordBot.stop();
            discordBot = null;
        }

        configManager.loadConfig();
        languageManager.loadLanguages();

        if (databaseManager != null) {
            databaseManager.close();
        }
        if (!setupDatabase()) {
            languageManager.sendMessage(sender, "commands.reload_failed");
            languageManager.sendConsoleMessage("console.database_fatal");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        scheduleRewards();
        scheduleRoleSync();
        startDiscordBot(sender, true);
    }

    private void scheduleRewards() {
        if (rewardTask != null) {
            rewardTask.cancel();
        }

        long minutes = Math.max(1L, getConfig().getLong("rewards.salary-interval", 60));
        long interval = minutes * 20L * 60L;
        rewardTask = new net.nex.discordlink.utils.RewardScheduler(this)
                .runTaskTimer(this, interval, interval);
    }

    private void scheduleRoleSync() {
        if (roleSyncTask != null) {
            roleSyncTask.cancel();
        }
        if (!getConfig().getBoolean("sync.role-sync.enabled", false)) {
            roleSyncTask = null;
            return;
        }

        long minutes = Math.max(1L, getConfig().getLong("sync.role-sync.interval-minutes", 5L));
        long interval = minutes * 20L * 60L;
        roleSyncTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                roleManager.syncPlayerRole(player);
            }
        }, interval, interval);
    }

    private void startDiscordBot(CommandSender reloadSender, boolean reload) {
        int generation = ++botGeneration;
        this.discordBot = new net.nex.discordlink.bot.DiscordBot(this);
        this.discordBot.start(success -> {
            if (generation != botGeneration || !isEnabled()) return;

            if (success) {
                if (reload) {
                    languageManager.sendMessage(reloadSender, "commands.reload_success");
                } else {
                    printStartupMessage();
                }
                return;
            }

            if (reload && reloadSender != null) {
                languageManager.sendMessage(reloadSender, "commands.reload_failed");
            }
            getServer().getPluginManager().disablePlugin(this);
        });
    }

    @Override
    public void onDisable() {
        // Shutdown logic
        botGeneration++;
        if (rewardTask != null) {
            rewardTask.cancel();
            rewardTask = null;
        }
        if (roleSyncTask != null) {
            roleSyncTask.cancel();
            roleSyncTask = null;
        }
        if (consoleAppender != null) {
            consoleAppender.unregister();
        }
        if (discordBot != null) {
            discordBot.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (languageManager != null) {
            languageManager.sendConsoleMessage("console.disabled");
        }
    }

    public static NexDiscordLink getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public net.nex.discordlink.database.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public net.nex.discordlink.utils.RoleManager getRoleManager() {
        return roleManager;
    }

    public net.nex.discordlink.utils.LinkManager getLinkManager() {
        return linkManager;
    }

    public net.nex.discordlink.bot.DiscordBot getDiscordBot() {
        return discordBot;
    }

    public net.nex.discordlink.utils.SecurityManager getSecurityManager() {
        return securityManager;
    }

    public net.nex.discordlink.utils.TwoFactorManager getTwoFactorManager() {
        return twoFactorManager;
    }

    public net.nex.discordlink.utils.AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public void refreshPlaceholders(java.util.UUID uuid) {
        if (placeholderExpansion != null) placeholderExpansion.refresh(uuid);
    }
}

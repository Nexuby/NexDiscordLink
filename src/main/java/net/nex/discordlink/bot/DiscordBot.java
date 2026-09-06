package net.nex.discordlink.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

public class DiscordBot {

    private final NexDiscordLink plugin;
    private volatile JDA jda;
    private volatile boolean stopped;

    public DiscordBot(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public void start(Consumer<Boolean> completion) {
        stopped = false;
        plugin.getLanguageManager().sendConsoleMessage("console.bot_starting");

        String token = plugin.getConfigManager().getBotToken();
        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            completeOnMainThread(completion, false, null, "Token is missing or invalid");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            JDA startedJda = null;
            try {
                startedJda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new SecurityBotListener(plugin))
                        .addEventListeners(new BotBoostListener(plugin))
                        .addEventListeners(new LinkBotListener(plugin))
                        .addEventListeners(new SlashCommandListener(plugin))
                        .addEventListeners(new ChatBotListener(plugin))
                        .addEventListeners(new ModalLinkListener(plugin))
                        .addEventListeners(new ConsoleCommandListener(plugin))
                        .build();

                jda = startedJda;
                startedJda.awaitReady();
                if (stopped) {
                    startedJda.shutdownNow();
                    return;
                }

                // Register Slash Commands
                startedJda.updateCommands().addCommands(
                    Commands.slash("profile", plugin.getLanguageManager().getMessage("discord.command.profile.description")),
                    Commands.slash("hesap", plugin.getLanguageManager().getMessage("discord.command.profile.description")),
                    Commands.slash("link", plugin.getLanguageManager().getMessage("discord.command.link.description"))
                        .addOption(OptionType.STRING, "code", plugin.getLanguageManager().getMessage("discord.command.link.option"), true),
                    Commands.slash("eşle", plugin.getLanguageManager().getMessage("discord.command.link.description"))
                        .addOption(OptionType.STRING, "kod", plugin.getLanguageManager().getMessage("discord.command.link.option"), true),
                    Commands.slash("unlink", plugin.getLanguageManager().getMessage("discord.command.unlink.description")),
                    Commands.slash("eşlemeyi-kaldır", plugin.getLanguageManager().getMessage("discord.command.unlink.description")),
                    Commands.slash("help", plugin.getLanguageManager().getMessage("discord.command.help.description")),
                    Commands.slash("yardım", plugin.getLanguageManager().getMessage("discord.command.help.description")),
                    Commands.slash("setup-link", "Setup the link channel message (Admin only)")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                    Commands.slash("console", "Execute a command in server console")
                        .addOption(OptionType.STRING, "command", "The command to execute", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR))
                ).queue();

                completeOnMainThread(completion, true, startedJda.getSelfUser().getName(), null);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                if (startedJda != null) startedJda.shutdownNow();
                completeOnMainThread(completion, false, null, "Discord startup was interrupted");
            } catch (Exception exception) {
                if (startedJda != null) startedJda.shutdownNow();
                completeOnMainThread(completion, false, null, exception.getMessage());
            }
        });
    }

    public void stop() {
        stopped = true;
        JDA current = jda;
        jda = null;
        if (current != null) {
            current.shutdownNow();
        }
    }

    public void sendEmbed(String channelId, MessageEmbed embed) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessageEmbeds(embed).queue();
        }
    }

    public JDA getJda() {
        return jda;
    }

    private void completeOnMainThread(Consumer<Boolean> completion, boolean success, String botName, String error) {
        Runnable task = () -> {
            if (success) {
                plugin.getLanguageManager().sendConsoleMessage("console.bot_started", "bot_name", botName);
            } else {
                plugin.getLanguageManager().sendConsoleMessage(
                        "console.bot_error",
                        "error",
                        error == null || error.isBlank() ? "Unknown error" : error
                );
            }
            completion.accept(success);
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}

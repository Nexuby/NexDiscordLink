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

public class DiscordBot {

    private final NexDiscordLink plugin;
    private JDA jda;

    public DiscordBot(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getLanguageManager().sendConsoleMessage("console.bot_starting");

        String token = plugin.getConfigManager().getBotToken();
        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLanguageManager().sendConsoleMessage("console.bot_error", "error", "Token is missing or invalid!");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new SecurityBotListener(plugin))
                        .addEventListeners(new BotBoostListener(plugin))
                        .addEventListeners(new LinkBotListener(plugin))
                        .addEventListeners(new SlashCommandListener(plugin))
                        .addEventListeners(new ChatBotListener(plugin))
                        .addEventListeners(new ModalLinkListener(plugin))
                        .addEventListeners(new ConsoleCommandListener(plugin))
                        .build();

                jda.awaitReady();

                // Register Slash Commands
                jda.updateCommands().addCommands(
                    Commands.slash("profile", plugin.getLanguageManager().getMessage("discord.command.profile.description")),
                    Commands.slash("setup-link", "Setup the link channel message (Admin only)")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                    Commands.slash("console", "Execute a command in server console")
                        .addOption(OptionType.STRING, "command", "The command to execute", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR))
                ).queue();

                String botName = jda.getSelfUser().getName();
                plugin.getLanguageManager().sendConsoleMessage("console.bot_started", "bot_name", botName);

            } catch (InterruptedException e) {
                plugin.getLanguageManager().sendConsoleMessage("console.bot_error", "error", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
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
}

package net.nex.discordlink.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ConsoleCommandListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public ConsoleCommandListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("console")) {
            // Check if console command is enabled in config
            if (!plugin.getConfig().getBoolean("console-command.enabled", false)) {
                event.reply(plugin.getLanguageManager().getMessage("discord.command.console.disabled")).setEphemeral(true).queue();
                return;
            }

            if (event.getGuild() == null) {
                event.reply(plugin.getLanguageManager().getMessage("discord.command.no_dm")).setEphemeral(true).queue();
                return;
            }

            // Double check permission just in case, though Discord handles it visually
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply(plugin.getLanguageManager().getMessage("commands.no_permission")).setEphemeral(true).queue();
                return;
            }

            String command = event.getOption("command").getAsString();
            event.deferReply().queue();

            // Execute command on main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

                    String responseKey = success ? "discord.command.console.success" : "discord.command.console.fail";
                    String response = plugin.getLanguageManager().getMessage(responseKey, "command", command);

                    event.getHook().sendMessage(response).queue();
                }
            }.runTask(plugin);
        }
    }
}

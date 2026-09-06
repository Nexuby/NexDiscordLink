package net.nex.discordlink.bot;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SecurityBotListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public SecurityBotListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("verify_login:")) {
            String uuidStr = event.getComponentId().split(":")[1];
            UUID uuid = UUID.fromString(uuidStr);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Unfreeze
                plugin.getSecurityManager().unfreezePlayer(uuid);

                // Update IP
                String ip = player.getAddress().getAddress().getHostAddress();
                plugin.getDatabaseManager().updateIpAddress(uuid, ip);

                event.reply(plugin.getLanguageManager().getMessage("security.verified_dm")).setEphemeral(true).queue();
            } else {
                event.reply(plugin.getLanguageManager().getMessage("security.player_not_online")).setEphemeral(true).queue();
            }
        }
    }
}

package net.nex.discordlink.bot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.nex.discordlink.NexDiscordLink;

public class LinkBotListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public LinkBotListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(net.dv8tion.jda.api.entities.channel.ChannelType.PRIVATE)) return;

        String message = event.getMessage().getContentRaw().trim();

        int codeLength = plugin.getLinkManager().getCodeLength();
        if (message.matches("\\d{" + codeLength + "}")) {
            if (!plugin.getLinkManager().isDmEnabled()) return;

            plugin.getLinkManager().processLinkCode(
                    message,
                    event.getAuthor().getId(),
                    event.getAuthor().getName(),
                    response -> event.getChannel().sendMessage(response).queue()
            );
        }
    }
}

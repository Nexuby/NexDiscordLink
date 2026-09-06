package net.nex.discordlink.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.nex.discordlink.NexDiscordLink;

import java.awt.Color;
public class ModalLinkListener extends ListenerAdapter {

    private final NexDiscordLink plugin;

    public ModalLinkListener(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("setup-link")) {
            if (!plugin.getLinkManager().isModalEnabled()) {
                event.reply(plugin.getLanguageManager().getMessage("discord.modal.disabled")).setEphemeral(true).queue();
                return;
            }

            if (event.getGuild() == null) {
                event.reply(plugin.getLanguageManager().getMessage("discord.command.no_dm")).setEphemeral(true).queue();
                return;
            }

            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply(plugin.getLanguageManager().getMessage("commands.no_permission")).setEphemeral(true).queue();
                return;
            }

            String channelId = plugin.getConfig().getString("link-system.modal.channel-id");
            TextChannel channel = null;

            if (channelId != null && !channelId.isEmpty()) {
                channel = event.getGuild().getTextChannelById(channelId);
            }

            if (channel == null) {
                channel = event.getChannel().asTextChannel();
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(plugin.getLanguageManager().getMessage("discord.modal.embed.title"));
            embed.setDescription(plugin.getLanguageManager().getMessage("discord.modal.embed.description"));
            embed.setFooter(plugin.getLanguageManager().getMessage("discord.modal.embed.footer"));
            embed.setColor(Color.GREEN);

            String label = plugin.getConfig().getString("link-system.modal.button-label", "Link Account");
            String styleStr = plugin.getConfig().getString("link-system.modal.button-style", "PRIMARY");
            ButtonStyle style;
            try {
                style = ButtonStyle.valueOf(styleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                style = ButtonStyle.PRIMARY;
                plugin.getLogger().warning("Invalid button style '" + styleStr + "' in config, defaulting to PRIMARY.");
            }

            channel.sendMessageEmbeds(embed.build())
                    .setActionRow(Button.of(style, "link-button", label))
                    .queue();

            event.reply(plugin.getLanguageManager().getMessage("discord.modal.setup_sent")).setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("link-button")) {
            if (!plugin.getLinkManager().isModalEnabled()) {
                event.reply(plugin.getLanguageManager().getMessage("discord.modal.disabled")).setEphemeral(true).queue();
                return;
            }

            TextInput codeInput = TextInput.create("code", plugin.getConfig().getString("link-system.modal.input-label", "Code"), TextInputStyle.SHORT)
                    .setPlaceholder(plugin.getConfig().getString("link-system.modal.input-placeholder", "Enter 4-digit code"))
                    .setMinLength(4)
                    .setMaxLength(4)
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("link-modal", plugin.getConfig().getString("link-system.modal.modal-title", "Link Account"))
                    .addComponents(ActionRow.of(codeInput))
                    .build();

            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("link-modal")) {
            if (!plugin.getLinkManager().isModalEnabled()) {
                event.reply(plugin.getLanguageManager().getMessage("discord.modal.disabled")).setEphemeral(true).queue();
                return;
            }

            String code = event.getValue("code").getAsString();

            plugin.getLinkManager().processLinkCode(
                    code,
                    event.getUser().getId(),
                    event.getUser().getName(),
                    response -> event.reply(response).setEphemeral(true).queue()
            );
        }
    }
}

package net.nex.discordlink.bot;

import org.junit.jupiter.api.Test;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.HELP;
import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.LINK;
import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.PROFILE;
import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.UNLINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DiscordCommandRouterTest {

    @Test
    void routesEnglishAndTurkishAccountCommands() {
        assertEquals(PROFILE, DiscordCommandRouter.resolve("profile"));
        assertEquals(PROFILE, DiscordCommandRouter.resolve("hesap"));
        assertEquals(LINK, DiscordCommandRouter.resolve("link"));
        assertEquals(LINK, DiscordCommandRouter.resolve("eşle"));
        assertEquals(UNLINK, DiscordCommandRouter.resolve("unlink"));
        assertEquals(UNLINK, DiscordCommandRouter.resolve("eşlemeyi-kaldır"));
        assertEquals(HELP, DiscordCommandRouter.resolve("yardım"));
    }

    @Test
    void turkishSlashCommandNamesAreAcceptedByJda() {
        assertDoesNotThrow(() -> Commands.slash("eşle", "Hesabını eşleştir"));
        assertDoesNotThrow(() -> Commands.slash("eşlemeyi-kaldır", "Eşlemeyi kaldır"));
        assertDoesNotThrow(() -> Commands.slash("yardım", "Komutları göster"));
    }
}

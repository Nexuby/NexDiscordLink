package net.nex.discordlink.bot;

import org.junit.jupiter.api.Test;

import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.HELP;
import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.LINK;
import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.PROFILE;
import static net.nex.discordlink.bot.DiscordCommandRouter.CommandType.UNLINK;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

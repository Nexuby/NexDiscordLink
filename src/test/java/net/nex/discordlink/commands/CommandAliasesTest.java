package net.nex.discordlink.commands;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static net.nex.discordlink.commands.CommandAliases.TwoFactorAction.DISABLE;
import static net.nex.discordlink.commands.CommandAliases.TwoFactorAction.SETUP;
import static net.nex.discordlink.commands.CommandAliases.TwoFactorAction.VERIFY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandAliasesTest {

    @Test
    void pluginDescriptorRegistersRequestedLinkAliases() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/plugin.yml")) {
            PluginDescriptionFile description = new PluginDescriptionFile(input);
            Map<String, Object> linkCommand = description.getCommands().get("link");
            Map<String, Object> statusCommand = description.getCommands().get("linkstatus");

            assertEquals(
                    List.of("eşle", "hesapeşle", "esle", "hesapesle"),
                    linkCommand.get("aliases")
            );
            assertTrue(((List<?>) statusCommand.get("aliases")).contains("eşledurum"));
            assertTrue(((List<?>) statusCommand.get("aliases")).contains("hesabım"));
        }
    }

    @Test
    void recognizesTurkishAdministrationSubcommands() {
        assertTrue(CommandAliases.isReload("yenile"));
        assertTrue(CommandAliases.isReload("yenidenyükle"));
        assertTrue(CommandAliases.isResetReward("ödülsıfırla"));
        assertTrue(CommandAliases.isResetReward("odulsifirla"));
        assertTrue(CommandAliases.isStatus("durum"));
        assertTrue(CommandAliases.isStatus("kontrol"));
        assertTrue(CommandAliases.isSync("senkronize"));
        assertTrue(CommandAliases.isSync("eşitle"));
    }

    @Test
    void recognizesTurkishTwoFactorSubcommands() {
        assertEquals(SETUP, CommandAliases.getTwoFactorAction("kur"));
        assertEquals(VERIFY, CommandAliases.getTwoFactorAction("doğrula"));
        assertEquals(VERIFY, CommandAliases.getTwoFactorAction("giris"));
        assertEquals(DISABLE, CommandAliases.getTwoFactorAction("kapat"));
    }

    @Test
    void allowsTurkishTwoFactorCommandsWhilePlayerIsRestricted() {
        assertTrue(CommandAliases.isAllowedDuringTwoFactorVerification("/ikifaktör giriş 123456"));
        assertTrue(CommandAliases.isAllowedDuringTwoFactorVerification("/ikiasamali dogrula 123456"));
        assertFalse(CommandAliases.isAllowedDuringTwoFactorVerification("/spawn"));
    }
}

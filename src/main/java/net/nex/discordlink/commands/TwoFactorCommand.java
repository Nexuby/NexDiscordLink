package net.nex.discordlink.commands;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TwoFactorCommand implements CommandExecutor {

    private final NexDiscordLink plugin;

    public TwoFactorCommand(NexDiscordLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().sendMessage(sender, "commands.player_only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getLanguageManager().sendMessage(player, "commands.2fa_usage");
            return true;
        }

        // Check if player is linked
        if (!plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "commands.unlink_not_linked");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("setup")) {
            if (plugin.getTwoFactorManager().has2FA(player.getUniqueId())) {
                plugin.getLanguageManager().sendMessage(player, "security.2fa_already_setup");
                return true;
            }

            String secret = plugin.getTwoFactorManager().generateSecret(player);
            // otpauth URL for the QR code
            String otpAuthURL = "otpauth://totp/" + player.getName() + "?secret=" + secret + "&issuer=NexDiscordLink";

            plugin.getLanguageManager().sendMessage(player, "security.2fa_setup_instructions", "secret", secret);

            // Give QR Map
            org.bukkit.inventory.ItemStack map = net.nex.discordlink.utils.QRMapManager.createQRMap(player, otpAuthURL);
            player.getInventory().addItem(map);
            player.sendMessage(plugin.getLanguageManager().getMessage("security.2fa_map_given"));

            return true;
        }

        if (subCommand.equals("verify") || subCommand.equals("login")) {
            if (args.length < 2) {
                plugin.getLanguageManager().sendMessage(player, "commands.2fa_usage");
                return true;
            }

            int code;
            try {
                code = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
                return true;
            }

            // Check if verifying setup or login
            if (plugin.getTwoFactorManager().verifySetup(player, code)) {
                plugin.getLanguageManager().sendMessage(player, "security.2fa_setup_success");
            } else if (plugin.getTwoFactorManager().verifyLogin(player, code)) {
                // Message sent in verifyLogin
            } else {
                plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
            }
            return true;
        }

        if (subCommand.equals("disable")) {
            if (!plugin.getTwoFactorManager().has2FA(player.getUniqueId())) {
                plugin.getLanguageManager().sendMessage(player, "security.2fa_not_setup");
                return true;
            }

            // Require code to disable
            if (args.length < 2) {
                plugin.getLanguageManager().sendMessage(player, "security.2fa_disable_usage");
                return true;
            }

            int code;
            try {
                code = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
                return true;
            }

            // Verify code against stored secret
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            String secret = plugin.getDatabaseManager().get2FASecret(player.getUniqueId());

            if (gAuth.authorize(secret, code)) {
                plugin.getTwoFactorManager().remove2FA(player.getUniqueId());
                plugin.getLanguageManager().sendMessage(player, "security.2fa_disabled");
            } else {
                plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
            }
            return true;
        }

        plugin.getLanguageManager().sendMessage(player, "commands.2fa_usage");
        return true;
    }
}

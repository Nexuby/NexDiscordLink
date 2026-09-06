package net.nex.discordlink.commands;

import net.nex.discordlink.NexDiscordLink;
import net.nex.discordlink.commands.CommandAliases.TwoFactorAction;
import net.nex.discordlink.utils.TwoFactorManager.VerificationResult;
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

        if (!player.hasPermission("nexdiscord.2fa")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no_permission");
            return true;
        }

        if (args.length == 0) {
            plugin.getLanguageManager().sendMessage(player, "commands.2fa_usage");
            return true;
        }

        // Check if player is linked
        if (!plugin.getDatabaseManager().isLinked(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "commands.unlink_not_linked");
            return true;
        }

        TwoFactorAction action = CommandAliases.getTwoFactorAction(args[0]);

        if (action == TwoFactorAction.SETUP) {
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

        if (action == TwoFactorAction.VERIFY) {
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

            VerificationResult result;
            if (plugin.getTwoFactorManager().hasPendingSetup(player.getUniqueId())) {
                result = plugin.getTwoFactorManager().verifySetup(player, code);
                sendVerificationResult(player, result, true);
            } else {
                result = plugin.getTwoFactorManager().verifyLogin(player, code);
                sendVerificationResult(player, result, false);
            }
            return true;
        }

        if (action == TwoFactorAction.DISABLE) {
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

            VerificationResult result = plugin.getTwoFactorManager().verifyStoredCode(player.getUniqueId(), code);
            if (result == VerificationResult.SUCCESS) {
                if (plugin.getTwoFactorManager().remove2FA(player.getUniqueId())) {
                    plugin.getLanguageManager().sendMessage(player, "security.2fa_disabled");
                } else {
                    plugin.getLanguageManager().sendMessage(player, "security.2fa_storage_error");
                }
            } else {
                sendVerificationResult(player, result, false);
            }
            return true;
        }

        plugin.getLanguageManager().sendMessage(player, "commands.2fa_usage");
        return true;
    }

    private void sendVerificationResult(Player player, VerificationResult result, boolean setup) {
        switch (result) {
            case SUCCESS -> plugin.getLanguageManager().sendMessage(
                    player,
                    setup ? "security.2fa_setup_success" : "security.2fa_verified"
            );
            case RATE_LIMITED -> plugin.getLanguageManager().sendMessage(player, "security.2fa_rate_limited");
            case EXPIRED -> plugin.getLanguageManager().sendMessage(player, "security.2fa_setup_expired");
            case STORAGE_ERROR -> plugin.getLanguageManager().sendMessage(player, "security.2fa_storage_error");
            case NOT_CONFIGURED -> plugin.getLanguageManager().sendMessage(player, "security.2fa_not_setup");
            case INVALID -> plugin.getLanguageManager().sendMessage(player, "security.invalid_code");
        }
    }
}

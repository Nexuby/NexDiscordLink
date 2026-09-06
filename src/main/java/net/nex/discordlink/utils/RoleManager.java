package net.nex.discordlink.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.milkbowl.vault.permission.Permission;
import net.nex.discordlink.NexDiscordLink;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoleManager {

    private final NexDiscordLink plugin;
    private Permission permission;

    public RoleManager(NexDiscordLink plugin) {
        this.plugin = plugin;
        setupPermissions();
    }

    private boolean setupPermissions() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        permission = rsp.getProvider();
        return permission != null;
    }

    /**
     * Get all managed Discord role IDs from the vault-groups config section.
     */
    private Set<String> getManagedRoleIds() {
        Set<String> ids = new HashSet<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sync.role-sync.vault-groups");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String id = section.getString(key);
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * Main entry point: syncs linked role + vault roles for a player.
     * Called from Bukkit main thread (e.g., on join).
     */
    public void syncPlayerRole(Player player) {
        // Get Discord ID (database call) - run async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String discordId = plugin.getDatabaseManager().getDiscordId(player.getUniqueId());
            if (discordId == null) return;
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) return;

            // Get primary group on main thread (Vault is not always thread-safe)
            Bukkit.getScheduler().runTask(plugin, () -> {
                String primaryGroup = null;
                if (permission != null && plugin.getConfig().getBoolean("sync.role-sync.enabled", false)) {
                    try {
                        primaryGroup = permission.getPrimaryGroup(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to get primary group for " + player.getName() + ": " + e.getMessage());
                    }
                }

                final String group = primaryGroup;

                // Now do Discord API calls async
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    for (Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
                        guild.retrieveMemberById(discordId).queue(
                            member -> applyRoles(guild, member, group),
                            error -> {
                                // Member not found in this guild, ignore
                            }
                        );
                    }
                });
            });
        });
    }

    /**
     * Apply linked role and vault group role to a member.
     * Removes old managed roles and adds the correct one, all in a single REST action
     * to avoid race conditions.
     */
    private void applyRoles(Guild guild, Member member, String primaryGroup) {
        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();

        // 1. Linked Role
        if (plugin.getConfig().getBoolean("sync.linked-role.enabled", false)) {
            String linkedRoleId = plugin.getConfig().getString("sync.linked-role.role-id");
            if (linkedRoleId != null && !linkedRoleId.isEmpty() && !linkedRoleId.equals("YOUR_LINKED_ROLE_ID")) {
                Role linkedRole = guild.getRoleById(linkedRoleId);
                if (linkedRole != null) {
                    if (!member.getRoles().contains(linkedRole)) {
                        rolesToAdd.add(linkedRole);
                    }
                } else {
                    plugin.getLogger().warning("Linked role with ID " + linkedRoleId + " not found in guild " + guild.getName());
                }
            }
        }

        // 2. Vault Role Sync
        if (plugin.getConfig().getBoolean("sync.role-sync.enabled", false) && primaryGroup != null) {
            Set<String> managedRoleIds = getManagedRoleIds();
            String targetRoleId = plugin.getConfig().getString("sync.role-sync.vault-groups." + primaryGroup.toLowerCase());

            // Also try original case
            if (targetRoleId == null) {
                targetRoleId = plugin.getConfig().getString("sync.role-sync.vault-groups." + primaryGroup);
            }

            Role targetRole = null;
            if (targetRoleId != null && !targetRoleId.isEmpty()) {
                targetRole = guild.getRoleById(targetRoleId);
                if (targetRole == null) {
                    plugin.getLogger().warning("Vault group role with ID " + targetRoleId + " for group '" + primaryGroup + "' not found in guild " + guild.getName());
                }
            }

            // Remove all managed roles that are NOT the target
            for (String managedId : managedRoleIds) {
                if (targetRole != null && managedId.equals(targetRole.getId())) continue;

                Role roleToRemove = guild.getRoleById(managedId);
                if (roleToRemove != null && member.getRoles().contains(roleToRemove)) {
                    rolesToRemove.add(roleToRemove);
                }
            }

            // Add target role if member doesn't have it
            if (targetRole != null && !member.getRoles().contains(targetRole)) {
                rolesToAdd.add(targetRole);
            }
        }

        // Apply all changes in a single request to avoid race conditions
        if (!rolesToAdd.isEmpty() || !rolesToRemove.isEmpty()) {
            guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue(
                success -> {},
                error -> plugin.getLogger().warning("Failed to modify roles for " + member.getEffectiveName() + ": " + error.getMessage())
            );
        }
    }

    /**
     * Remove all managed roles (linked + vault) from a Discord user.
     * Called when a player unlinks.
     */
    public void removePlayerRoles(String discordId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getJda() == null) return;

            for (Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
                guild.retrieveMemberById(discordId).queue(member -> {
                    List<Role> rolesToRemove = new ArrayList<>();

                    // 1. Linked Role
                    if (plugin.getConfig().getBoolean("sync.linked-role.enabled", false)) {
                        String roleId = plugin.getConfig().getString("sync.linked-role.role-id");
                        if (roleId != null && !roleId.isEmpty()) {
                            Role role = guild.getRoleById(roleId);
                            if (role != null && member.getRoles().contains(role)) {
                                rolesToRemove.add(role);
                            }
                        }
                    }

                    // 2. Vault Roles
                    if (plugin.getConfig().getBoolean("sync.role-sync.enabled", false)) {
                        Set<String> managedRoleIds = getManagedRoleIds();
                        for (String managedId : managedRoleIds) {
                            Role role = guild.getRoleById(managedId);
                            if (role != null && member.getRoles().contains(role)) {
                                rolesToRemove.add(role);
                            }
                        }
                    }

                    // Remove all in single request
                    if (!rolesToRemove.isEmpty()) {
                        guild.modifyMemberRoles(member, new ArrayList<>(), rolesToRemove).queue(
                            success -> {},
                            error -> plugin.getLogger().warning("Failed to remove roles for " + member.getEffectiveName() + ": " + error.getMessage())
                        );
                    }
                }, error -> {
                    // Member not found in guild
                });
            }
        });
    }
}

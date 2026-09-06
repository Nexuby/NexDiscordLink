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

import static net.nex.discordlink.utils.AuditLogger.AuditEvent.ROLE_SYNC;

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
                final SyncDirection direction = SyncDirection.fromConfig(
                        plugin.getConfig().getString("sync.role-sync.direction", "MINECRAFT_TO_DISCORD")
                );

                // Now do Discord API calls async
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    for (Guild guild : plugin.getDiscordBot().getJda().getGuilds()) {
                        guild.retrieveMemberById(discordId).queue(
                            member -> synchronize(guild, member, player, group, direction),
                            error -> {
                                // Member not found in this guild, ignore
                            }
                        );
                    }
                });
            });
        });
    }

    private void synchronize(Guild guild, Member member, Player player, String primaryGroup, SyncDirection direction) {
        String discordGroup = findDiscordGroup(member);

        if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
            if (discordGroup != null) syncVaultGroup(player, discordGroup);
            return;
        }

        if (direction == SyncDirection.BIDIRECTIONAL && discordGroup != null) {
            // A configured Discord role is authoritative when both sides differ.
            syncVaultGroup(player, discordGroup);
            return;
        }

        applyRoles(guild, member, primaryGroup);
    }

    private String findDiscordGroup(Member member) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sync.role-sync.vault-groups");
        if (section == null) return null;

        Set<String> memberRoleIds = new HashSet<>();
        for (Role role : member.getRoles()) {
            memberRoleIds.add(role.getId());
        }
        for (String group : section.getKeys(false)) {
            String roleId = section.getString(group);
            if (roleId != null && memberRoleIds.contains(roleId)) return group;
        }
        return null;
    }

    private void syncVaultGroup(Player player, String targetGroup) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (permission == null || !player.isOnline()) return;
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("sync.role-sync.vault-groups");
            if (section == null || !section.contains(targetGroup)) return;

            boolean changed = false;
            for (String managedGroup : section.getKeys(false)) {
                if (managedGroup.equalsIgnoreCase(targetGroup)) continue;
                if (permission.playerInGroup(null, player, managedGroup)) {
                    changed |= permission.playerRemoveGroup(null, player, managedGroup);
                }
            }
            if (!permission.playerInGroup(null, player, targetGroup)) {
                changed |= permission.playerAddGroup(null, player, targetGroup);
            }
            if (changed) {
                plugin.getAuditLogger().log(ROLE_SYNC, player.getName(), "Discord -> Vault: " + targetGroup);
            }
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
                success -> plugin.getAuditLogger().log(
                        ROLE_SYNC,
                        member.getEffectiveName(),
                        "Vault -> Discord: added=" + rolesToAdd.size() + ", removed=" + rolesToRemove.size()
                ),
                error -> plugin.getLogger().warning("Failed to modify roles for " + member.getEffectiveName() + ": " + error.getMessage())
            );
        }
    }

    private enum SyncDirection {
        MINECRAFT_TO_DISCORD,
        DISCORD_TO_MINECRAFT,
        BIDIRECTIONAL;

        private static SyncDirection fromConfig(String value) {
            if (value == null) return MINECRAFT_TO_DISCORD;
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MINECRAFT_TO_DISCORD;
            }
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

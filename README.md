# NexDiscordLink

[![Build](https://github.com/Nexuby/NexDiscordLink/actions/workflows/build.yml/badge.svg)](https://github.com/Nexuby/NexDiscordLink/actions/workflows/build.yml)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![Minecraft 1.16.5+](https://img.shields.io/badge/Minecraft-1.16.5%2B-62b47a.svg)](#requirements)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

🇹🇷 **Türk kullanıcılar:** [Türkçe dokümantasyon için buraya tıklayın.](README_TR.md)

> Secure Minecraft–Discord account linking, synchronization, authentication, messaging, and network-wide automation in one plugin.

NexDiscordLink is a production-oriented Spigot/Paper integration for communities that want Discord to be part of the Minecraft account lifecycle—not just another chat relay. It combines secure account linking, TOTP authentication, IP-change verification, role and nickname synchronization, configurable rewards, rich Discord messages, auditing, and shared-database proxy support.

## At a glance

| Area | Included |
| --- | --- |
| Account linking | Discord DM, interactive modal, and bilingual slash commands |
| Account security | Single-use codes, rate limits, TOTP 2FA, IP-change verification, secure unlink approval |
| Synchronization | Linked role, Vault groups, Discord roles, nicknames, bans, scheduled and manual sync |
| Engagement | Link rewards, first-link/relink rewards, role rewards, salary, boost rewards |
| Messaging | Two-way chat, event notifications, audit embeds, per-message Discord styling |
| Networks | SQLite, MySQL, and shared-MySQL Velocity/Bungee backend deployments |
| Operations | Automatic config/lang doctors, sensitive-data redaction, PlaceholderAPI cache |
| Localization | English and Turkish messages, Minecraft aliases, and Discord commands |

**Documentation:** [Installation](#installation) · [Minecraft commands](#minecraft-commands) · [Discord commands](#discord-commands) · [Message customization](#discord-message-customization) · [Security](#security) · [Troubleshooting](#troubleshooting)

## Feature highlights

### Linking and account security

- Link through Discord DMs, an interactive button/modal panel, or English/Turkish slash commands.
- Enforce a one-Minecraft-account ↔ one-Discord-account policy with expiring, single-use, rate-limited codes.
- Protect accounts with TOTP 2FA, including an in-game QR map for authenticator setup.
- Challenge changed network addresses through a private Discord verification button.
- Require TOTP or a short-lived, account-bound Discord approval before unlinking.

### Synchronization and rewards

- Synchronize linked roles, Vault groups, Discord roles, Minecraft names, Discord nicknames, and bans.
- Choose Minecraft → Discord, Discord → Minecraft, or bidirectional group/role synchronization.
- Run synchronization on join, on a schedule, or immediately through an administrator command.
- Combine first-link, relink, Discord-role, recurring salary, and server-boost rewards using console commands.

### Messaging and operations

- Bridge chat in both directions with webhook avatar support and mention parsing disabled by default.
- Publish join, quit, death, advancement, profile, security, linking, and audit embeds.
- Customize each Discord message's channel, text, color, author, thumbnail, image, footer, timestamp, and buttons.
- Run automatic config and language doctors on startup and plugin reload—without exposing tokens or passwords.
- Use redacted audit logs, a guarded Discord console command, and cached PlaceholderAPI values.
- Deploy on one server with SQLite or across Velocity/Bungee backends with shared MySQL.

## Requirements

- Java 17 or newer
- Spigot or Paper 1.16.5 or newer
- A Discord bot
- Optional: Vault and a compatible permissions plugin for group synchronization
- Optional: PlaceholderAPI for placeholders
- A MySQL database reachable by every backend when proxy mode is enabled

Enable the `Server Members Intent` and `Message Content Intent` privileged options in the Discord Developer Portal.

Depending on the enabled features, the bot role needs `View Channels`, `Send Messages`, `Embed Links`, `Manage Roles`, `Manage Nicknames`, and—when ban synchronization is enabled—`Ban Members`. The bot role must be above every role it manages.

## Installation

1. Copy `NexDiscordLink-<version>.jar` into the server's `plugins` directory.
2. Start the server once and wait for the configuration files to be generated.
3. Stop the server completely.
4. Configure the bot token, channel IDs, and role IDs in `plugins/NexDiscordLink/config.yml`.
5. Enable the required intents and permissions in the Discord Developer Portal.
6. Start the server again.
7. If modal linking is enabled, run `/setup-link` on Discord to publish the linking message.

Never commit a live bot token, database password, webhook URL, `secret.key`, or a production server configuration to GitHub.

## Quick configuration

### Single server

The default SQLite storage is suitable for a single server:

```yaml
settings:
  language: "en"

proxy:
  enabled: false

database-settings:
  type: "sqlite"

link-system:
  type: "BOTH"
  code-length: 6
  code-expiry-minutes: 5
```

Available `link-system.type` values:

- `DM`: Players send the code to the bot through a direct message.
- `MODAL`: Players enter the code in the Discord linking modal.
- `BOTH`: Both methods are available.

### MySQL

```yaml
database-settings:
  type: "mysql"
  host: "127.0.0.1"
  port: 3306
  database: "nexdiscordlink"
  username: "nexdiscordlink"
  password: "USE_A_STRONG_PASSWORD"
```

Database tables and required new columns are created automatically during startup. If the database connection cannot be established, the plugin disables itself to avoid inconsistent data.

## Proxy network setup

For a Velocity or BungeeCord network, use the same MySQL configuration on every backend. SQLite is rejected when proxy mode is enabled.

Designated bot node:

```yaml
proxy:
  enabled: true
  server-id: "lobby-1"
  bot-enabled: true
  link-check-interval-ticks: 40
```

Other backend nodes:

```yaml
proxy:
  enabled: true
  server-id: "survival-1"
  bot-enabled: false
  link-check-interval-ticks: 40
```

Every `server-id` must be unique. Link codes are stored with an expiration time in the shared MySQL database. When the bot redeems a code on another node, the backend where the code was generated applies the player notification, reward, and role synchronization within two seconds by default.

Features that require immediate Discord DMs—such as IP-change verification and unlink approval—need a bot-enabled node. Use TOTP on bot-disabled worker nodes or configure these security flows for your network architecture.

## Minecraft commands

| Primary command | Turkish alternatives | Description |
| --- | --- | --- |
| `/link` | `/eşle`, `/hesapeşle`, `/esle`, `/hesapesle` | Generates an expiring account-link code. |
| `/unlink [TOTP code]` | `/eşlemesil`, `/eslemesil`, `/hesapayır`, `/hesapayir` | Securely removes the account link. |
| `/linkstatus` | `/eşledurum`, `/esledurum`, `/hesabım`, `/hesabim` | Shows the Discord account, link date, and 2FA state. |
| `/linkreward` | `/eşleödül`, `/esleodul`, `/ödül`, `/odul` | Shows link rewards and remaining claims. |
| `/2fa setup` | `/ikifaktör kur`, `/ikifaktor kur` | Starts TOTP setup and gives the player a QR map. |
| `/2fa login <code>` | `/ikifaktör giriş <kod>`, `/ikifaktor giris <kod>` | Completes login verification. |
| `/2fa disable <code>` | `/ikifaktör kapat <kod>`, `/ikifaktor kapat <kod>` | Disables TOTP after verifying the code. |
| `/nexdiscord reload` | `/discordyönet yenile`, `/discordyonet yenile` | Reloads configuration, database, schedulers, and the bot. |
| `/nexdiscord status` | `/discordyönet durum`, `/discordyonet kontrol` | Checks database, bot, guild, and security health. |
| `/nexdiscord sync [player\|all]` | `/discordyönet senkronize [oyuncu\|tümü]` | Starts role synchronization immediately. |
| `/nexdiscord resetreward <player\|all>` | `/discordyönet ödülsıfırla <oyuncu\|all>` | Resets link-reward counters. |

## Discord commands

| Command | Description |
| --- | --- |
| `/link code:<code>` / `/eşle kod:<kod>` | Links a Minecraft account. |
| `/profile` / `/hesap` | Shows the linked Minecraft profile. |
| `/unlink` / `/eşlemeyi-kaldır` | Removes the account link from Discord. |
| `/help` / `/yardım` | Shows available account commands. |
| `/setup-link` | Publishes the modal-linking message in the selected channel. |
| `/console command:<command>` | Executes an allowlisted server command when explicitly enabled. |

Account-related Discord command responses are ephemeral.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `nexdiscord.link` | Everyone | Generate an account-link code |
| `nexdiscord.unlink` | Everyone | Remove an account link |
| `nexdiscord.2fa` | Everyone | Manage TOTP authentication |
| `nexdiscord.status` | Everyone | View personal account status |
| `nexdiscord.reward.preview` | Everyone | Preview link rewards |
| `nexdiscord.reload` | OP | Reload the plugin configuration |
| `nexdiscord.admin` | OP | Use status, synchronization, and reward administration |
| `nexdiscord.update.notify` | OP | Receive update notifications |

## Role synchronization

```yaml
sync:
  linked-role:
    enabled: true
    role-id: "DISCORD_ROLE_ID"
  role-sync:
    enabled: true
    direction: "MINECRAFT_TO_DISCORD"
    interval-minutes: 5
    vault-groups:
      vip: "VIP_DISCORD_ROLE_ID"
      mvp: "MVP_DISCORD_ROLE_ID"
```

Available `direction` values:

- `MINECRAFT_TO_DISCORD`: The Vault group determines the Discord role.
- `DISCORD_TO_MINECRAFT`: The Discord role determines the Vault group.
- `BIDIRECTIONAL`: A configured Discord role takes priority when present; otherwise, the Minecraft group is applied to Discord.

Only groups and roles configured under `vault-groups` are managed.

## Rewards

Rewards are configured as console commands, allowing economy, item, XP, and third-party plugin commands to be combined:

```yaml
rewards:
  link-rewards:
    enabled: true
    limit: 1
    commands:
      - "eco give {player} 2000"
      - "give {player} golden_apple 2"
    first-link-commands:
      - "experience add {player} 250 points"
    relink-commands: []
    discord-role-commands:
      "SUPPORTER_DISCORD_ROLE_ID":
        - "eco give {player} 1000"
```

- `commands`: Runs for every successful link that is eligible for rewards.
- `first-link-commands`: Runs for the player's first rewarded link.
- `relink-commands`: Runs for later rewarded links.
- `discord-role-commands`: Adds commands based on the player's Discord roles.
- `limit: 0`: Removes the reward-claim limit.

## Discord message customization

Text remains editable in `plugins/NexDiscordLink/lang/messages_en.yml` and `messages_tr.yml`. The `discord-messages` section in `config.yml` controls presentation and routing for join, quit, death, advancement, link-panel, login-security, profile, audit, and Minecraft-to-Discord chat messages.

```yaml
discord-messages:
  defaults:
    timestamp: true
    footer:
      enabled: true
      text: "play.example.net"
      icon-url: "https://example.net/icon.png"

  events:
    join:
      enabled: true
      channel-id: "" # Empty uses channels.log-channel-id
      title: "Welcome to the server"
      description: "**{player}** joined us!"
      color: "#2F80ED"
      author:
        enabled: true
        text: "{player}"
        icon-url: "https://mc-heads.net/avatar/{player}"
      thumbnail:
        enabled: true
        url: "https://mc-heads.net/avatar/{player}"
      image:
        enabled: false
        url: "https://example.net/welcome.png"
```

Colors accept a name such as `GREEN`, a hexadecimal value such as `#2F80ED`, or an RGB value such as `47, 128, 237`. Only HTTP/HTTPS image URLs are accepted. Invalid colors fall back to the built-in event color, invalid image URLs are ignored, and text is safely truncated to Discord's field limits.

Common placeholders are `{player}` and `{uuid}`. Additional placeholders are `{death_message}` for deaths, `{advancement}` for advancements, `{ip}` for the private login-verification message, and `{message}` for the chat bridge. Audit templates may use `{event}`, `{actor}`, and `{detail}` in their title or description.

An empty `title`, `description`, button label, or field label keeps the text from the active language file. Each event can be routed to a separate `channel-id`; an empty value uses the existing log-channel setting. Chat messages always disable Discord mention parsing, including webhook delivery.

Apply configuration and language changes with `/nexdiscord reload` (or `/discordyönet yenile`). Re-run `/setup-link` after changing the link-panel appearance because previously published Discord messages are not edited retroactively.

## PlaceholderAPI

The expansion registers automatically when PlaceholderAPI is installed:

| Placeholder | Value |
| --- | --- |
| `%nexdiscord_linked%` | Whether the account is linked (`true`/`false`) |
| `%nexdiscord_discord_id%` | Linked Discord user ID |
| `%nexdiscord_discord_username%` | Cached Discord username |
| `%nexdiscord_2fa_enabled%` | TOTP state (`true`/`false`) |
| `%nexdiscord_linked_at%` | Link date |
| `%nexdiscord_reward_count%` | Number of claimed link rewards |

To avoid frequent TAB and scoreboard updates overloading the database, placeholder values are served from an asynchronously refreshed 30-second cache. On the first request, a value may briefly be empty or use its default.

## Security

### Sensitive data protection

- TOTP secrets are encrypted with AES-GCM.
- IP addresses are stored as non-reversible keyed HMAC fingerprints.
- Audit logs automatically redact IP, token, secret, and key values.
- Link-code and TOTP verification attempts are rate-limited.

On first startup, the plugin creates `plugins/NexDiscordLink/secret.key`. Back it up securely. Losing or replacing it makes existing encrypted TOTP secrets unreadable.

Managed deployments can provide a Base64-encoded 32-byte key through an environment variable:

```text
NEXDISCORDLINK_MASTER_KEY=<BASE64_ENCODED_32_BYTE_KEY>
```

Never commit the key or its value.

### Secure unlinking

- When TOTP is enabled, `/unlink <code>` is required.
- Without TOTP, a confirmation button valid for two minutes is sent to the linked Discord account.
- The approval token is single-use and bound to the linked Discord account.
- `/unlink` on Discord is treated as direct approval from the linked account.

### Audit logs

Account links, unlinks, TOTP changes, failed verifications, IP verification, role changes, and administrator actions can be sent to Discord:

```yaml
audit-log:
  enabled: true
  channel-id: "" # Empty uses channels.log-channel-id
  console: false
```

### Discord console command

`/console` can grant full server access and is disabled by default:

```yaml
console-command:
  enabled: false
  whitelist:
    - "say"
    - "list"
    - "tps"
```

Only commands whose root command appears in the allowlist can execute. An empty allowlist denies every command. Do not enable this feature unless Discord administrator access is tightly controlled.

## Build and test

Windows:

```powershell
.\gradlew.bat clean test shadowJar
```

Linux/macOS:

```bash
./gradlew clean test shadowJar
```

The shaded, deployable artifact is generated at:

```text
build/libs/NexDiscordLink-<version>.jar
```

GitHub Actions runs a clean build and test suite for every push.

## Upgrading

1. Stop the server completely.
2. Back up the current JAR, `config.yml`, `database.db` or MySQL database, and `secret.key`.
3. Replace the old JAR with the new version.
4. Start the server and review the startup logs.
5. Run `/nexdiscord status` to verify the database and bot state.

Do not use `/reload` or similar global reload commands for JAR updates. NexDiscordLink's own `/nexdiscord reload` command is intended for configuration changes.

Existing language files do not need to be deleted. New message keys automatically fall back to the current defaults bundled in the JAR.

## Troubleshooting

### Automatic config and language doctors

NexDiscordLink automatically inspects `config.yml` and the selected language file during every startup and `/nexdiscord reload`. No doctor command is required. Results are printed to the server console as `ERROR`, `WARNING`, or healthy summaries.

The configuration doctor checks YAML readability, missing and unknown settings, value types and ranges, bot configuration without exposing the token, Discord IDs, HTTP/HTTPS URLs, embed colors, button styles, proxy/MySQL compatibility, chat destinations, and feature dependencies. The language doctor checks YAML readability, missing and unknown keys, scalar/list type parity, English fallback availability, and missing or unexpected placeholders such as `{player}`.

Errors and warnings are diagnostic: the doctor does not rewrite files or disable the plugin by itself. Settings omitted from an older configuration continue to use bundled defaults and are reported as warnings so they can be copied into the server configuration when customization is needed.

- **The bot does not start:** Check the token, privileged intents, and network access.
- **Slash commands are missing:** Confirm that the plugin started successfully and that the token belongs to the expected Discord application.
- **Roles are not synchronized:** Check the bot role hierarchy, `Manage Roles` permission, Vault connection, and configured role IDs.
- **Proxy mode does not start:** `database-settings.type` must be set to `mysql` in proxy mode.
- **A link code is rejected:** It may have expired; run `/link` again in Minecraft.
- **A Discord DM is not delivered:** Check the member's privacy settings and confirm that the bot is enabled on the current node.
- **TOTP data cannot be read:** Verify that the correct `secret.key` or `NEXDISCORDLINK_MASTER_KEY` is in use.

## License

NexDiscordLink is released under the [MIT License](LICENSE).

# NexDiscordLink

[![Build](https://github.com/Nexuby/NexDiscordLink/actions/workflows/build.yml/badge.svg)](https://github.com/Nexuby/NexDiscordLink/actions/workflows/build.yml)

NexDiscordLink is a Discord–Minecraft bridge for Spigot and Paper servers. It combines account linking, Discord role and nickname synchronization, login verification, chat bridging, event logging, and configurable rewards in one plugin.

## Features

- Account linking through Discord direct messages, an interactive modal, or both
- Configurable one-time link rewards and recurring salary rewards
- Discord boost rewards for linked Minecraft players
- Linked-member and Vault group role synchronization
- Minecraft username to Discord nickname synchronization
- IP-change verification through Discord direct messages
- Optional TOTP two-factor authentication with an in-game QR map
- Bidirectional Minecraft–Discord chat bridge with optional webhooks
- Join, quit, death, and advancement event embeds
- Optional Discord console command and console log forwarding
- SQLite and MySQL storage backends
- English and Turkish language files

## Requirements

- Java 17 or newer
- Spigot or Paper 1.16.5+
- A Discord bot with Message Content and Server Members intents enabled
- Vault and a compatible permissions plugin when Vault group synchronization is used

## Building

Windows:

```powershell
.\gradlew.bat clean shadowJar
```

Linux or macOS:

```bash
./gradlew clean shadowJar
```

The shaded plugin artifact is generated at `build/libs/NexDiscordLink-<version>.jar`.

## Installation

1. Build the plugin or download a release artifact.
2. Copy the shaded JAR into the Minecraft server's `plugins` directory.
3. Start the server once to generate the default configuration.
4. Stop the server and configure the Discord bot token, channel IDs, roles, and storage backend.
5. Enable the required privileged intents in the Discord Developer Portal, then start the server again.
6. Run `/setup-link` in Discord to publish the interactive linking message when modal linking is enabled.

Never commit a live bot token, database password, webhook URL, or generated runtime configuration. The configuration under `src/main/resources` contains distributable defaults only.

### Sensitive data protection

NexDiscordLink encrypts TOTP secrets with AES-GCM and stores IP addresses as keyed, non-reversible fingerprints. On first startup it creates `plugins/NexDiscordLink/secret.key`. Back up this file securely: losing or replacing it makes existing encrypted TOTP secrets unreadable.

For managed deployments, provide a Base64-encoded 32-byte key through the `NEXDISCORDLINK_MASTER_KEY` environment variable instead of relying on the generated key file. Never commit either form of the key.

## Commands

### Minecraft

| Command | Turkish alternatives | Description |
| --- | --- | --- |
| `/link` | `/eşle`, `/hesapeşle`, `/esle`, `/hesapesle` | Generate a temporary account-linking code. |
| `/unlink` | `/eşlemesil`, `/eslemesil`, `/hesapayır`, `/hesapayir` | Remove the current Discord account link. |
| `/linkstatus` | `/eşledurum`, `/esledurum`, `/hesabım`, `/hesabim` | Show the linked Discord account, link date, and 2FA state. |
| `/2fa setup` | `/ikifaktör kur`, `/ikifaktor kur` | Begin TOTP setup and receive a QR map. |
| `/2fa login <code>` | `/ikifaktör giriş <code>`, `/ikifaktor giris <code>` | Complete TOTP verification after joining. |
| `/2fa disable <code>` | `/ikifaktör kapat <code>`, `/ikifaktor kapat <code>` | Disable TOTP after verification. |
| `/nexdiscord reload` | `/discordyönet yenile`, `/discordyonet yenile` | Reload configuration, database, scheduler, and Discord bot. |
| `/nexdiscord status` | `/discordyönet durum`, `/discordyonet kontrol` | Check database, Discord bot, guild, and security health. |
| `/nexdiscord sync [player\|all]` | `/discordyönet senkronize [oyuncu\|tümü]` | Start role synchronization immediately. |
| `/nexdiscord resetreward <player\|all>` | `/discordyönet ödülsıfırla <oyuncu\|all>` | Reset link-reward counters. |

### Discord

| Command | Description |
| --- | --- |
| `/profile` | Display the linked Minecraft profile. |
| `/hesap` | Display the linked Minecraft profile in Turkish. |
| `/link code:<code>` / `/eşle kod:<kod>` | Link a Minecraft account directly from Discord. |
| `/unlink` / `/eşlemeyi-kaldır` | Remove the current account link. |
| `/help` / `/yardım` | Display the available account commands. |
| `/setup-link` | Publish the modal-linking message. |
| `/console <command>` | Execute a server command when explicitly enabled. |

The Discord console command grants full server-console access. Keep it disabled unless it is intentionally required and Discord administrator access is tightly controlled.

When enabled, `console-command.whitelist` is mandatory. Only the root commands listed there can execute; an empty list denies every command.

## Configuration

The main configuration is distributed from `src/main/resources/config.yml`. Localized messages live under `src/main/resources/lang/`.

Account links, unlinks, 2FA changes, IP-verification outcomes, and administrator actions can be sent to the configured audit channel. Audit output automatically redacts named secrets, tokens, keys, and IPv4 addresses.

Supported link modes:

- `DM`
- `MODAL`
- `BOTH`

Supported database types:

- `sqlite`
- `mysql`

## License

NexDiscordLink is released under the [MIT License](LICENSE).

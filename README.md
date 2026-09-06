# NexDiscordLink

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

## Commands

### Minecraft

| Command | Description |
| --- | --- |
| `/link` | Generate a temporary account-linking code. |
| `/unlink` | Remove the current Discord account link. |
| `/2fa setup` | Begin TOTP setup and receive a QR map. |
| `/2fa login <code>` | Complete TOTP verification after joining. |
| `/2fa disable <code>` | Disable TOTP after verification. |
| `/nexdiscord reload` | Reload configuration and language files. |
| `/nexdiscord resetreward <player\|all>` | Reset link-reward counters. |

### Discord

| Command | Description |
| --- | --- |
| `/profile` | Display the linked Minecraft profile. |
| `/setup-link` | Publish the modal-linking message. |
| `/console <command>` | Execute a server command when explicitly enabled. |

The Discord console command grants full server-console access. Keep it disabled unless it is intentionally required and Discord administrator access is tightly controlled.

## Configuration

The main configuration is distributed from `src/main/resources/config.yml`. Localized messages live under `src/main/resources/lang/`.

Supported link modes:

- `DM`
- `MODAL`
- `BOTH`

Supported database types:

- `sqlite`
- `mysql`

## License

NexDiscordLink is released under the [MIT License](LICENSE).

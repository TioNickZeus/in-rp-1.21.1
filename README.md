# In-RP (Minecraft 1.21.1 NeoForge)

A lightweight, server-friendly **Roleplay (RP) Switch & Utility Mod** for **Minecraft 1.21.1** powered by **NeoForge**.

---

## ✨ Features

- **100% Server-Side Compatible**: Vanilla Minecraft clients can connect to servers running this mod without needing NeoForge or any client-side mods installed!
- **RP Switch (`/rp`)**: Easily toggle in and out of character. Status persists across player deaths, dimension changes, and server restarts using NeoForge Data Attachments.
- **AFK System (`/afk`)**:
  - Automatically detects inactive players (every 5 seconds, near-zero CPU cost) or manual toggle via `/afk`.
  - Automatically disables RP mode upon entering AFK to protect character immersion and avoid involuntary deaths.
  - Distinct visual markers: `[AFK]` tag on the Tab list and ` [AFK]` suffix on overhead nametag.
  - Instant wake-up upon movement, camera rotation, or typing in chat.
  - Optional configurable idle kick, applied only to players already marked as AFK.
- **Proximity Local Chat & Global Chat (`/g`)**:
  - Regular chat messages are routed locally by proximity with configurable radius (e.g. 40 blocks).
  - Subtle feedback when nobody is around to hear you: `(Nobody nearby heard you)`.
  - `/g <message>` or `/global <message>` to broadcast server-wide with anti-spam cooldown.
  - Chat Spy (`/rpadmin spy` / `/chatspy`) for staff to monitor out-of-range local chat and private messages (`/tell`, `/msg`, `/w`).
- **Dynamic Overhead & Chat Identifiers**:
  - Displays a unified roleplay suffix (e.g., `Player [in RP]`) above the player's head, in the tab list and in chat via native Minecraft Scoreboard Teams, without duplication.
  - The text comes from `nametagSuffix` in the server config; set it to `""` to disable the marker entirely.
- **Modular Dice Roller (`/roll`)**:
  - Standard dice: `/roll` (default 1d20), `/roll 20`, `/roll 100`.
  - Classic RPG notation: `/roll 2d6`, `/roll 3d20`, `/roll 1d100` with sum and individual die breakdown.
  - Proximity broadcast (configurable radius or global).
- **Staff Administration (`/rpadmin`)**:
  - Remotely set player RP modes using native entity selectors (`@a`, `@p`, distance filters, etc.).
  - Dynamically toggle roleplay rules on the fly:
    - **PvP in RP**: Enable or prevent combat while in RP, including indirect damage (arrows, tridents, thrown potions, TNT).
    - **Block Break Protection**: Prevent players from breaking blocks in RP.
    - **Block Place Protection**: Prevent players from placing blocks in RP.
    - **Operator Bypass**: Allow staff/OPs to bypass restrictions even when rules are active.
  - Built-in validation: Avoids redundant disk writes if a setting is already set.
- **Player Lives & Death Tracking (`/lives`)**:
  - Automatically tracks player deaths persistently.
  - Staff can assign max lives per player (or server-wide default).
  - When lives run out, the player is either placed into **Spectator mode** with a `[DEAD]` tag on Tab, or **kicked/banned** until revived by an admin.
  - Admins can revive both online and offline players (`/rpadmin lives revive <player>`).
- **Internationalization (i18n)**:
  - Supports client-side language switching (`Component.translatableWithFallback`).
  - Bundled with **English (`en_us`)** and **Brazilian Portuguese (`pt_br`)**.
  - Provides translated server fallbacks for vanilla clients.

---

## 📜 Commands

### Player Commands

| Command | Permission | Description |
| :--- | :---: | :--- |
| `/rp` | Everyone | Checks your current Roleplay status. |
| `/rp on` | Everyone | Enters Roleplay mode (activates nametag & chat tags). |
| `/rp off` | Everyone | Exits Roleplay mode. |
| `/rp toggle` | Everyone | Toggles between In-RP and Off-RP. |
| `/rp help` | Everyone | Shows a list of player commands (`/rp`, `/afk`, `/g`, `/roll`, `/lives`). |
| `/afk` | Everyone | Toggles AFK (away from keyboard) status with 3s anti-spam cooldown. |
| `/g <message>` | Everyone | Sends a message to global server chat (with anti-spam cooldown). |
| `/roll` | Everyone | Rolls a default 20-sided die (1-20). |
| `/roll <sides>` | Everyone | Rolls a die with a specified number of sides (e.g. `/roll 100`). |
| `/roll <dice>` | Everyone | Rolls dice using RPG notation (e.g. `/roll 2d6`, `/roll 3d20`). |
| `/lives` | Everyone | Checks your own death count, max lives, and remaining lives. |
| `/lives <player>` | Everyone | Checks another player's lives and death stats. |

### Staff Commands (OP Level 2+)

| Command | Permission | Description |
| :--- | :---: | :--- |
| `/rpadmin set <targets> <on\|off>` | OP (Level 2) | Sets RP mode for specified players or selectors (e.g. `/rpadmin set @a on`). |
| `/rpadmin config pvp <true\|false>` | OP (Level 2) | Enables or disables PvP between or against players in RP mode. |
| `/rpadmin config block_break <true\|false>` | OP (Level 2) | Enables or disables block breaking for players in RP mode. |
| `/rpadmin config block_place <true\|false>` | OP (Level 2) | Enables or disables block placing for players in RP mode. |
| `/rpadmin config op_bypass <true\|false>` | OP (Level 2) | Allows or prevents operators (OP level 2+) from bypassing RP restrictions. |
| `/rpadmin lives set <targets> <amount>` | OP (Level 2) | Sets maximum lives for players (-1 for unlimited). |
| `/rpadmin lives revive <targets>` | OP (Level 2) | Revives dead players (works for both online and offline players). |
| `/rpadmin lives setdeaths <targets> <amount>` | OP (Level 2) | Manually sets death count for players. |
| `/rpadmin lives action <spectator\|kick>` | OP (Level 2) | Sets elimination action when lives run out (spectator or kick). |
| `/rpadmin lives applydefault [targets]` | OP (Level 2) | Applies current default max lives to all online (or specified) players. |
| `/rpadmin confirm` | OP (Level 2) | Confirms a pending bulk action (required when affecting 5+ players). |
| `/rpadmin spy` | OP (Level 2) | Toggles Chat Spy mode to monitor out-of-range local chat and PMs. |
| `/chatspy` | OP (Level 2) | Quick shortcut to toggle Chat Spy mode. |
| `/rpadmin help` | OP (Level 2) | Shows a list of all admin commands. |

---

## ⚙️ Configuration

The configuration is a NeoForge `SERVER` config, so it is generated per world at
`<world>/serverconfig/inrp-server.toml` (on a dedicated server: `world/serverconfig/inrp-server.toml`):

```toml
[general]
    # Server fallback language for vanilla clients (e.g. en_us, pt_br)
    # Must match a bundled file in assets/inrp/lang/; invalid values fall back to en_us
    serverLanguage = "en_us"

    # Roleplay suffix shown after the player name above their head, in the tab list and in chat.
    # In-RP marks players with a native scoreboard team, which carries a single suffix, so the
    # same text is used in all three places. Leave empty "" to disable the marker entirely.
    nametagSuffix = " [in RP]"

    # Fallback for nametagSuffix, used only when nametagSuffix is empty.
    # Kept for compatibility with configs written before the suffixes were unified.
    chatSuffix = "[RP]"

[rules]
    # Whether PvP is allowed between or against players in RP mode
    # Covers melee attacks as well as indirect damage such as arrows, thrown potions and TNT
    pvpAllowedInRP = true

    # Whether players in RP mode can break blocks
    blockBreakAllowedInRP = true

    # Whether players in RP mode can place blocks
    blockPlaceAllowedInRP = true

    # Whether operators/staff (OP level 2+) bypass RP restrictions (block break, block place, PvP)
    opBypassRestrictions = true

[roll]
    # Default number of sides for /roll when no arguments are given
    # Range: 2 ~ 10000
    rollDefaultSides = 20

    # Radius in blocks to broadcast /roll results. Set to -1.0 for global broadcast.
    # Range: -1.0 ~ 1000.0
    rollProximityRadius = 30.0

[lives]
    # Action taken when a player loses all lives ('spectator' or 'kick')
    livesAction = "spectator"

    # Default max lives for players (-1 for unlimited/disabled)
    defaultMaxLives = -1

    # If true, only deaths while in RP mode count toward the lives system
    countDeathsOnlyInRP = false

[afk]
    # Enable or disable the AFK (inactivity) system
    afkEnabled = true

    # Idle time in seconds before a player is automatically marked as AFK
    afkTimeoutSeconds = 300

    # Idle time in seconds before an AFK player is kicked (-1 to disable kick)
    # Only players already marked as AFK are kicked, so values below afkTimeoutSeconds
    # behave as if they were equal to afkTimeoutSeconds.
    afkKickSeconds = -1

    # If true, entering AFK mode automatically disables RP mode
    autoDisableRPOnAFK = true

[chat]
    # Whether standard chat is converted into proximity local chat
    localChatEnabled = true

    # Proximity radius in blocks for local chat
    # Range: 5.0 ~ 500.0
    localChatRadius = 40.0

    # Cooldown in seconds between messages in /g or /global (0 to disable)
    # Range: 0 ~ 300
    globalChatCooldownSeconds = 3

    # Whether staff with Chat Spy active receive out-of-range local chat
    spyLocalChat = true

    # Whether staff with Chat Spy active receive copies of private messages (/tell, /msg, /w)
    spyPrivateMessages = false
```

---

## 📥 Installation

### Dedicated Server
1. Ensure your server is running **NeoForge 1.21.1** (NeoForge 21.1.249 or newer).
2. Place the compiled `inrp-1.0.6.jar` into the server's `mods/` directory.
3. Start the server. Players with pure **Vanilla Minecraft 1.21.1** clients can connect immediately!

### Singleplayer / Client
1. Place the `inrp-1.0.6.jar` in your `.minecraft/mods/` directory.
2. Launch Minecraft using the NeoForge 1.21.1 profile.

---

## 🌐 Localization & Translations

Translations are located in `src/main/resources/assets/inrp/lang/`:
- `en_us.json` — English (US)
- `pt_br.json` — Português (Brasil)

To add a new language, simply create `<language_code>.json` in the same directory and submit a pull request!

---

## 🛠️ Building from Source

To compile the mod yourself, you will need Java 21:

```bash
# Clone the repository
git clone https://github.com/TioNickZeus/in-rp-1.21.1.git
cd in-rp-1.21.1

# Build the mod JAR
./gradlew build
```

The resulting JAR file will be located in `build/libs/`.

---

## 📐 Architecture & Roadmap

- For detailed technical documentation, design decisions, data flows, and extension guidelines, see [`ARCHITECTURE.md`](ARCHITECTURE.md).
- For upcoming features, backlog, and planned ideas, see [`ROADMAP.md`](ROADMAP.md).

---

## 📄 License & Author

- **Author**: **TioNickZeus**
- **License**: [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](LICENSE)
  - You are free to share, copy, modify, and adapt this mod for personal and community servers.
  - **Non-Commercial**: You may not sell this mod or distribute it behind paid paywalls..


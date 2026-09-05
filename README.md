# In-RP (Minecraft 1.21.1 NeoForge)

A lightweight, server-friendly **Roleplay (RP) Switch & Utility Mod** for **Minecraft 1.21.1** powered by **NeoForge**.

---

## ✨ Features

- **100% Server-Side Compatible**: Vanilla Minecraft clients can connect to servers running this mod without needing NeoForge or any client-side mods installed!
- **RP Switch (`/rp`)**: Easily toggle in and out of character. Status persists across player deaths, dimension changes, and server restarts using NeoForge Data Attachments.
- **Dynamic Overhead & Chat Identifiers**:
  - Displays a clean suffix above the player's head (e.g., `Player [in RP]`) via native Minecraft Scoreboard Teams.
  - Formats chat with a roleplay suffix (e.g., `Player [RP]: message`).
- **Modular Dice Roller (`/roll`)**:
  - Standard dice: `/roll` (default 1d20), `/roll 20`, `/roll 100`.
  - Classic RPG notation: `/roll 2d6`, `/roll 3d20`, `/roll 1d100` with sum and individual die breakdown.
  - Proximity broadcast (configurable radius or global).
- **Staff Administration (`/rpadmin`)**:
  - Remotely set player RP modes using native entity selectors (`@a`, `@p`, distance filters, etc.).
  - Dynamically toggle roleplay rules on the fly:
    - **PvP in RP**: Enable or prevent combat while in RP.
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
| `/rp help` | Everyone | Shows a list of all player commands. |
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
| `/rpadmin help` | OP (Level 2) | Shows a list of all admin commands. |

---

## ⚙️ Configuration

The server configuration file is generated automatically at `config/inrp-server.toml`:

```toml
[general]
    # Server fallback language for vanilla clients (e.g. en_us, pt_br)
    serverLanguage = "en_us"

    # Suffix displayed after player name in chat when in RP mode. Leave empty "" to disable.
    chatSuffix = "[RP]"

    # Suffix displayed in player nametag above head and tablist when in RP mode.
    nametagSuffix = " [in RP]"

[rules]
    # Whether PvP is allowed between or against players in RP mode
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
```

---

## 📥 Installation

### Dedicated Server
1. Ensure your server is running **NeoForge 1.21.1** (NeoForge 21.1.249 or newer).
2. Place the compiled `inrp-1.0.3.jar` into the server's `mods/` directory.
3. Start the server. Players with pure **Vanilla Minecraft 1.21.1** clients can connect immediately!

### Singleplayer / Client
1. Place the `inrp-1.0.3.jar` in your `.minecraft/mods/` directory.
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

## 📐 Architecture

For detailed technical documentation, design decisions, data flows, and extension guidelines, see [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## 📄 License & Author

- **Author**: **TioNickZeus**
- **License**: [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](LICENSE)
  - You are free to share, copy, modify, and adapt this mod for personal and community servers.
  - **Non-Commercial**: You may not sell this mod or distribute it behind paid paywalls.

# Changelog

All notable changes to the In-RP mod will be documented in this file.

## [1.0.6] - 2026-09-10

### ✨ New Features

- **Proximity Local Chat (`[L]`)**:
  - Regular chat messages sent via standard key (`T`) are now delivered locally based on proximity (`localChatRadius`, default: 40 blocks).
  - Safe 100% server-side delivery via `player.sendSystemMessage(...)`, eliminating any client-side cryptographic chat signing/reporting errors on vanilla clients.
  - Subtle feedback notification if no one is nearby: `(Nobody nearby heard you)`.
  - Console logging preserved for server audit logs and bot integrations.
- **Global Server Chat (`/g` and `/global`)**:
  - Allows players to broadcast messages server-wide with the `[G]` prefix.
  - Configurable anti-spam cooldown (`globalChatCooldownSeconds`, default: 3s).
  - Automatically wakes up AFK players upon message transmission.
- **Staff Chat Spy (`/rpadmin spy` and `/chatspy`)**:
  - Toggle command for administrators (OP level 2+) to monitor server communications in real time.
  - State persists across deaths and disconnects via `IS_CHAT_SPY` data attachment.
  - Monitors out-of-range local chat with `[SPY:L]` tag.
  - Monitors private vanilla messages (`/tell`, `/msg`, `/w`) with `[SPY:PM]` tag (configurable toggle in TOML, disabled by default for player privacy).

### 🔴 Bug Fixes

- **Missing `/afk` in `/rp help`** — Resolved missing display entry for `/afk` in the player `/rp help` listing within `RPCommand.java`.

### 🗂️ New Files

- **`GlobalChatCommand.java`** (`commands/`) — Handles `/g` and `/global` registration, greedy string parsing, and per-UUID cooldowns.
- **`ChatSpyCommand.java`** (`commands/`) — Handles `/chatspy` shortcut toggle and permissions for staff.

### 🧹 Improvements & Tweaks

- **Event Handler Utilization** — Re-activated and expanded `ChatEventHandler.java` (`events/`) to handle both `ServerChatEvent` (local chat routing) and `CommandEvent` (chat spy interception).
- **AFK Wake-Up Resilience** — Configured `receiveCanceled = true` on `AFKEventHandler.onServerChat` to guarantee that chatting always wakes up an AFK player even when local chat cancels the default vanilla broadcast.
- **Updated Help Listings** — Added `/g` to `/rp help` and `/rpadmin spy` to `/rpadmin help`.

## [1.0.5] - 2026-09-06

### ✨ New Features

- **AFK (Inactivity) System** — Complete server-side AFK management:
  - **`/afk` command** — Voluntary toggle with 3-second anti-spam cooldown, global broadcast announcement, and soft click sound feedback.
  - **Inactivity Detection Timer** — Periodic batch check every 5 seconds (100 ticks) with near-zero CPU cost. Automatically marks players as AFK after `afkTimeoutSeconds` (default: 300s) without chat spam.
  - **Auto-Exit RP Mode** — Entering AFK (manually or via timer) automatically switches the player out of RP mode (`autoDisableRPOnAFK`, default: `true`). Upon waking up, the player remains in Off-RP mode until they deliberately re-enter RP with `/rp on`.
  - **Instant Wake-Up** — Moving, rotating the camera, or typing in chat immediately removes the AFK status with an action bar notification and sound feedback.
  - **Visual Markers (Tab List & Nametag)**:
    - Tab list displays `[AFK] PlayerName` in italic gray (with priority given to `[DEAD]`).
    - Overhead nametag displays ` [AFK]` suffix via dedicated scoreboard team `inrp_afk`.
  - **Configurable Idle Kick** — Optional `afkKickSeconds` (default: `-1`, disabled) to disconnect players after extended inactivity.

### 🛡️ Anti-Exploit & Balance

- **No Godmode / No Immunity** — AFK players remain fully vulnerable to damage and PvP; `/afk` cannot be abused as a combat escape or shield.
- **Silent Timer Entry** — Automatic AFK entry via timer does not send messages to global chat, preventing chat spam while maintaining full visibility via Tab list and nametags.
### 🔴 Bug Fixes

- **Immediate Wake-Up in Singleplayer/Dedicated** — Fixed a bug where entering AFK via `/afk` would instantly wake the player up on the next tick because sending the command packet refreshed `lastActionTime` to 0ms (triggering `< 1500ms` check). Replaced with coordinate and rotation delta tracking (`AFKPosition`) with a 1-second initial grace period.

### 🗂️ New Files

- **`AFKCommand.java`** (`commands/`) — Handles `/afk` registration, cooldowns, toggle logic, and sound feedback.
- **`AFKEventHandler.java`** (`events/`) — Handles server-tick idle monitoring, instant wake-up, login/logout cleanup, and disconnects.

### 🧹 Improvements & Tweaks

- **Unified RP Suffix & Chat Redundancy Fix** — Eliminated redundant `[in RP]` tag in chat (`<Dev [RP] [in RP]>`). Suffix is now handled cleanly and strictly by the `inrp_active` scoreboard team using `[RP]`, removing duplicate `NameFormat` event listeners.
- **Scoreboard Team Management** — Updated `ScoreboardHandler` to cleanly switch players between `inrp_active` and `inrp_afk` teams.
- **Tab List Tag Priority** — Updated `LivesEventHandler.onTabListNameFormat` to prioritize `[DEAD]` over `[AFK]`.
- **Data Attachments** — Added `IS_AFK` boolean attachment with `copyOnDeath()`.
- **Config Section `[afk]`** — Added `afkEnabled`, `afkTimeoutSeconds`, `afkKickSeconds`, and `autoDisableRPOnAFK` to server configuration.

### 📝 Localization

- Added 8 translation keys to both `en_us.json` and `pt_br.json`:
  - `inrp.help.afk`
  - `inrp.afk.tab.tag`
  - `inrp.afk.nametag.suffix`
  - `inrp.afk.enter.broadcast`
  - `inrp.afk.actionbar.return`
  - `inrp.afk.kick_message`
  - `inrp.afk.cooldown`
  - `inrp.afk.disabled`

---

## [1.0.3] - 2026-09-05

### 🔴 Critical Fixes

- **Thread Safety** — Added `synchronized(LOCK)` blocks to all `InRPLivesManager` methods, preventing `ConcurrentModificationException` and data corruption when commands and events access `DEAD_PLAYERS` simultaneously.
- **Atomic File Writes** — Refactored `InRPLivesManager.save()` to use a temporary file swap (`Files.move` with `REPLACE_EXISTING`), preventing data loss if the server crashes during write.
- **Config Validation & Crash Fix** — Changed `livesAction` from open `define()` to `defineInList("spectator", "kick")` using null-safe `Arrays.asList()` instead of `List.of()`, preventing server startup crashes (`NullPointerException` during NeoForge config validation/correction when checking null/invalid values).

### ✨ New Features

- **`/rp help`** — New help command listing all player commands (`/rp`, `/roll`, `/lives`).
- **`/rpadmin help`** — New help command listing all admin commands with usage syntax.
- **`/rpadmin lives applydefault [targets]`** — Applies the current `defaultMaxLives` config value to all online players (or specified targets). Auto-revives players who would no longer be dead under the new limit.
- **`/rpadmin confirm`** — Confirmation system for bulk actions. Commands affecting 5+ players now require confirmation via clickable `[CONFIRM]` button or `/rpadmin confirm` within 10 seconds. Console commands bypass confirmation.
- **`countDeathsOnlyInRP` config** — New boolean option (default: `false`) in `[lives]` section. When `true`, only deaths while in RP mode count toward the lives system; deaths outside RP are completely ignored.
- **Sound Feedback** — Entering RP mode plays a level-up sound; exiting plays an anvil sound. Only the player hears it (no broadcast).
- **Solo Roll Notification** — When using `/roll` with proximity broadcast and no other player is in range, the roller receives a subtle italic message: *(no one else heard your roll)*.

### 🧹 Cleanup & Improvements

- **Removed empty listener** — Deleted the empty `ChatEventHandler.onServerChat` method that was registered on the event bus without doing anything. The functional `onNameFormat` listener remains.
- **Localized error messages** — Replaced 3 hardcoded English strings ("Only players can execute this command") in `RPCommand` and `LivesCommand` with the localization key `inrp.error.players_only`.

### 🗂️ New Files

- **`ConfirmationManager.java`** (`util/`) — Thread-safe pending action manager with 10-second TTL, clickable chat confirmation, and automatic cleanup of expired entries.
- **`ARCHITECTURE.md`** — Single Source of Truth (SSOT) document covering package map, data flows, invariants, golden rules, and extension guidelines for future maintainers and AI agents.
- **`CHANGELOG.md`** — This file.

### 📝 Localization

- Added 22 new translation keys to both `en_us.json` and `pt_br.json`:
  - Help system (7 keys for `/rp help`, 9 keys for `/rpadmin help`)
  - Confirmation system (4 keys: pending, success, expired, click)
  - Confirmation descriptions (4 keys: setmode, setlives, setdeaths, applydefault)
  - Solo roll notification (1 key)
  - Console error (1 key)
  - Apply default feedback (2 keys)

### 📄 Documentation

- **README.md** — Updated command tables, added `countDeathsOnlyInRP` to config block, fixed JAR filename from `1.0.0` to `1.0.3`, added Architecture section with link to `ARCHITECTURE.md`.

---

## [1.0.2] - Initial Release

- RP toggle system (`/rp on|off|toggle`)
- Dice rolling with proximity broadcast (`/roll`)
- Lives and death tracking (`/lives`)
- Admin commands (`/rpadmin`)
- PvP, block break/place restrictions
- Scoreboard team nametag integration
- English and Brazilian Portuguese translations

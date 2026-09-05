# Changelog

All notable changes to the In-RP mod will be documented in this file.

## [1.0.3] - 2026-09-05

### 🔴 Critical Fixes

- **Thread Safety** — Added `synchronized(LOCK)` blocks to all `InRPLivesManager` methods, preventing `ConcurrentModificationException` and data corruption when commands and events access `DEAD_PLAYERS` simultaneously.
- **Atomic File Writes** — Refactored `InRPLivesManager.save()` to use a temporary file swap (`Files.move` with `REPLACE_EXISTING`), preventing data loss if the server crashes during write.
- **Config Validation** — Changed `livesAction` from open `define()` to `defineInList("spectator", "kick")`, preventing silent failures from invalid config values.

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

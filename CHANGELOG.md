# Changelog

All notable changes to the In-RP mod will be documented in this file.

## [1.0.7] - 2026-09-10

Comprehensive hardening pass, exploit mitigations, stability overhauls, and bug fixes across the entire codebase. No command, config key or save-data field was removed, so existing worlds and configurations continue working seamlessly.

### ⚠️ Behaviour Change

- **The roleplay suffix now comes from the config** — `general.nametagSuffix` (default `" [in RP]"`) is now read by the code and drives the scoreboard team suffix, which vanilla renders above the head, in the tab list and in chat. Previously the suffix was hardcoded to the `inrp.chat.suffix` language key, so it always showed `[RP]`. `general.chatSuffix` is kept as a fallback used when `nametagSuffix` is empty — set `nametagSuffix = "[RP]"` to keep the previous look, or `""` to disable the marker entirely.

### 🔴 Bug Fixes & Stability

- **Server Shutdown Crash & World Lock Fix** — Fixed an `IllegalStateException: Cannot get config value before config is loaded` during `ModConfigEvent.Unloading` on server shutdown when `LocalizationHelper` attempted to read an unloaded config spec. Server shutdown is now completely clean and properly releases the `session.lock` file, preventing worlds from becoming locked, disappearing from the singleplayer world list, or freezing the Java process with an `OverlappingFileLockException`.
- **Singleplayer/LAN Host Elimination Protection** — When `livesAction = "kick"`, the host player of a Singleplayer/LAN session is now gracefully placed into Spectator mode with an informative message instead of being disconnected, preventing the integrated server from abruptly terminating and disconnecting all friends playing on LAN.
- **Fluid Bucket Placement Leak Fix** — Fixed a bypass where players in RP mode could place water, lava, and mob buckets when `blockPlaceAllowedInRP = false`. Bucket fluid placement is now intercepted via `RightClickBlock` and `RightClickItem` with an action bar notification, while preserving legitimate container access (opening chests, barrels) and consumable item usage (drinking potions).
- **Cancelled deaths were counted** — Mods that keep a player alive by cancelling `LivingDeathEvent` (graves, second-chance and keep-alive mods) still cost the player a life, because the lives listener could run before the cancellation. It now runs at `EventPriority.LOWEST` and never sees a cancelled death.
- **Bulk confirmation could be bypassed** — While one action was pending, the next bulk command ran immediately without asking. `/rpadmin set @a off` followed by `/rpadmin lives set @a 1` executed the second command with no confirmation at all. Every bulk command is now staged on its own.
- **Staged actions wrote to stale players** — A confirmed action reused the `ServerPlayer` objects captured up to ten seconds earlier, so a target who disconnected in the meantime silently lost the change. Targets are now stored as UUIDs and re-resolved when the action actually runs.
- **`/rpadmin lives setdeaths` ignored `livesAction`** — It always forced spectator mode, even on servers configured to kick eliminated players.
- **`pvpAllowedInRP = false` only blocked melee** — Arrows, tridents, thrown potions and TNT still hurt (and could still be used by) players in RP mode. Indirect player-versus-player damage is now blocked as well.
- **`/roll 1d99999999999` broke the command** — The digit run overflowed `Integer.parseInt` and surfaced as a raw exception. Oversized values now report the normal bounds error.
- **`/rp toggle` from the console did nothing** — It returned silently instead of reporting that the command is players-only.
- **The idle kick hit non-AFK players** — `afkKickSeconds` was compared against raw idle time, so any value below `afkTimeoutSeconds` disconnected players who had never been marked AFK. Only AFK players can be kicked now.
- **AFK could re-trigger right after waking up** — Waking up did not refresh vanilla's `lastActionTime`, so the next inactivity sweep could flag the player again seconds later.
- **Players stayed stuck AFK** — When `afkEnabled` was switched off while they were flagged.
- **Entering RP mode destroyed the player's scoreboard team** — Vanilla's `addPlayerToTeam` evicts a player from their current team, so on a server that uses teams for rank prefixes (LuckPerms, datapacks, manual `/team join`) the first `/rp on` dropped that membership and `/rp off` left the player on no team at all. The previous team is now recorded in a new `PREVIOUS_TEAM` attachment and restored when the player leaves both the RP and AFK teams. Persisted, so it survives a logout, a death and a restart; if the team was deleted in the meantime, the player is simply left teamless.
- **`[DEAD]` and `[AFK]` tab list tags never refreshed** — A revived player kept their `[DEAD]` tag, and an AFK player kept `[AFK]`, until they reconnected. `refreshPlayerTabList` broadcast the tab list packet directly, but that packet only re-serialises the display name NeoForge cached the last time `PlayerEvent.TabListNameFormat` fired, so the tag was never recomputed. It now delegates to `ServerPlayer.refreshTabListName()`, which recomputes the name and broadcasts it itself &mdash; and only when it actually changed, which also removes a packet that used to go out on every login, respawn and dimension change.
- **Tab list names from other mods were overwritten** — The handler wrote `null` over the display name of every player who was neither AFK nor eliminated, clobbering tab list formatting set by any other mod.
- **State leaked between worlds** — AFK poses, `/afk` and `/g` cooldowns, pending confirmations and the eliminated-player list survived a world unload inside the same JVM (single player, or a server reload) and carried over into the next world.
- **`/g` and `/global` were two independent registrations** that could drift apart; `/g` is now a true alias.

### 🔒 Security

- **Path traversal in `serverLanguage`** — The value was interpolated straight into a classpath resource path. It is now validated against `[a-z0-9_-]{2,32}` and falls back to `en_us` when it does not match.
- **Chat spy survived a de-op** — The toggle lives in player save data, so a player who lost operator status kept receiving local chat and private message copies. The permission level is re-checked at delivery time.
- **Length caps** — Chat and `/g` messages are capped at 256 characters (vanilla's own limit, which a modified client can exceed) and roleplay suffixes at 64, so neither can bloat outgoing packets.
- **CI hardening** — The Gradle wrapper JAR is validated against known-good releases, and the workflow now runs with read-only repository permissions.

### ⚡ Performance

- **Scoreboard suffixes are no longer rewritten constantly** — Every login, respawn, dimension change and AFK toggle rewrote both team suffixes, and each write broadcasts a team update packet to every connected client. The value is now written only when the rendered text actually changed.
- **Local chat delivers in a single pass** — Recipients and out-of-range staff were resolved in two passes with a `HashSet` allocated per message. It is now one pass with no allocation, and the spy component is built only when somebody is actually watching.
- **Bulk revive writes once** — `/rpadmin lives revive` used to rewrite `inrp_dead_players.json` once per target.
- **Redundant disk writes skipped** — Marking or unmarking an already-correct player no longer touches the file.
- **Read-only attachment lookups no longer allocate** — Reading a flag used to materialise its default value into the player's attachment map, which matters for the AFK guard that runs on every player tick.
- **Teams are created lazily**, so a server that never uses RP or AFK keeps a clean scoreboard.
- **Duplicate tab list packet broadcasts removed**, and `ThreadLocalRandom.current()` hoisted out of the dice loop.

### 🧹 Code Quality & Tweaks

- **Defensive Config Fallback** — `LocalizationHelper.reloadTranslations` safely checks `InRPConfig.SPEC.isLoaded()` before querying values, falling back to default `"en_us"` if the configuration is unavailable or in the process of unloading.
- **Monotonic timings** — Cooldowns, the confirmation TTL and the AFK grace period use `Util.getMillis()` instead of wall-clock `System.currentTimeMillis()`, so a clock correction cannot skew them.
- **Thread-safe translations** — `LocalizationHelper` swaps immutable maps atomically instead of clearing and repopulating a `HashMap` that the server thread reads concurrently, and no longer touches the config during class initialization.
- **Correct file I/O** — `InRPLivesManager` uses `java.nio.file` with explicit UTF-8 instead of platform-default `FileReader`/`FileWriter`.
- **Deduplication** — The four near-identical `/rpadmin config` handlers collapsed into one parameterised path, and the new `ChatFormat` and `HelpText` utilities are shared by every chat channel and help listing.
- **Complete localization** — `[L]`, `[G]`, `[SPY:L]` and `[SPY:PM]` moved into the language files (they were the last hardcoded player-facing strings), as did the `ON`/`OFF` text inside confirmation prompts.
- **Conventions** — Static-only classes are `final` with private constructors, inline fully-qualified references replaced with imports, magic numbers named, and Javadoc added across the codebase explaining *why* each non-obvious decision was made.
- **Build** — `-Xlint` enabled, `options.release = 21` pinned, JAR manifest metadata added, archives made reproducible, MDK placeholder blocks removed, and the author declared once in `gradle.properties`.
- **CI** — Superseded runs are cancelled and the built JAR is uploaded as a workflow artifact.
- **Dead code removed** — Unused language keys and `ConfirmationManager.hasPending`.

### 🗂️ New Files

- **`ChatFormat.java`** (`util/`) — Shared sanitisation and layout for every In-RP chat channel.
- **`HelpText.java`** (`util/`) — Builds the `/rp help` and `/rpadmin help` listings from key/colour pairs.

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

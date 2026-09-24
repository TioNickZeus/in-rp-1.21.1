# ARCHITECTURE.md — In-RP Mod

> **Single Source of Truth (SSOT)** for the mod's technical architecture and design decisions.  
> Every AI agent, contributor, or maintainer must read this document before modifying code.  
> **Last updated**: September 2026 — version 1.0.8

---

## 1. Overview and Scope

| Field | Value |
|:---|:---|
| **Mod ID** | `inrp` |
| **Name** | In-RP |
| **Minecraft** | 1.21.1 |
| **Mod Loader** | NeoForge 21.1.x (FML) |
| **Config Type** | `SERVER` (TOML, authoritative — `<world>/serverconfig/inrp-server.toml`) |
| **License** | CC-BY-NC-4.0 |
| **Execution** | **100% Server-Side** — pure vanilla Minecraft clients can connect without any client mod |
| **Authentication** | Supports both **Online** (`online-mode=true`) and **Offline** (`online-mode=false`) servers |

### Purpose

In-RP provides a lightweight, modular, and server-friendly **Roleplay (RP) & Administration toolkit** for Minecraft 1.21.1 on NeoForge:

1. **Roleplay Status & Visual Identity** — Dynamic toggle for RP status (`/rp`), rendering visual markers above player heads, in chat, and in the player tab list without custom client renderers.
2. **AFK & Idle Management** — Automated sweep and voluntary idle status (`/afk`), automated RP exit on idle, scoreboard visual indicators, and configurable idle disconnection.
3. **Lives & Hardcore Elimination Engine** — Configurable maximum lives and death tracking; eliminated players transition to spectator mode or are disconnected, featuring an offline-compatible revival ledger.
4. **Proximity Local & Global Chat** — Proximity chat routing for vanilla speech, broadcast global chat (`/g`), and real-time staff surveillance (`/chatspy`).
5. **Dice Rolling Engine** — Flexible `/roll` command supporting both standard (`/roll 20`) and RPG dice notation (`/roll 2d6`) with proximity broadcast.
6. **Roleplay Gameplay Rules** — Configurable toggles restricting PvP, block breaking, and block placement (including fluid bucket placement) for players in RP mode, with OP bypass.
7. **Administrative Staging & Confirmation Engine** — `/rpadmin` administrative hub equipped with safety staging for bulk modifications affecting 5 or more players.

### Why 100% Server-Side?

Unlike dual-sided mods that require client installation, In-RP is architected to allow **unmodified vanilla Minecraft clients** to join dedicated NeoForge servers seamlessly. This architectural choice maximizes accessibility for public or casual roleplay communities.

Achieving rich immersion on pure vanilla clients requires specific technical approaches:
- **Native Scoreboard Teams**: Roleplay and AFK nametag/chat markers are driven by native scoreboard teams (`inrp_active` and `inrp_afk`). Vanilla clients naturally render team prefixes and suffixes above player models, in chat, and in tab lists without client-side modding.
- **Unsigned System Messages**: Vanilla 1.19+ enforces cryptographic chat signing. Re-routing signed player chat across proximity boundaries causes client-side signature verification errors. In-RP delivers proximity (`[L]`) and global (`[G]`) messages as unsigned system messages (`player.sendSystemMessage()`).
- **Action Bar Feedback & Sound Events**: Immediate interactive feedback (such as rule violations or AFK wake-up) is delivered via `player.displayClientMessage(..., true)` and `player.playNotifySound()`.

---

## 2. Package Map and Responsibilities

```
com.tio.inrp/
├── InRP.java                    ← Mod entrypoint (@Mod), lifecycle bootstrapping & bus registration
├── commands/                    ← Brigadier commands (server-side)
│   ├── RPCommand.java           ← /rp [on|off|help] (toggle RP mode, audio feedback)
│   ├── AFKCommand.java          ← /afk (voluntary idle toggle, broadcast, 3s anti-spam)
│   ├── RollCommand.java         ← /roll [NdS|N] (dice rolling with proximity broadcast)
│   ├── LivesCommand.java        ← /lives [player] (query remaining lives & status)
│   ├── GlobalChatCommand.java   ← /g, /global <message> (server-wide broadcast with cooldown)
│   ├── ChatSpyCommand.java      ← /chatspy (OP 2+ staff toggle for chat surveillance)
│   └── RPAdminCommand.java      ← /rpadmin (set|config|lives|spy|confirm|help)
├── config/
│   └── InRPConfig.java          ← TOML server configuration specifications & sanitizing accessors
├── data/
│   ├── InRPAttachments.java     ← NeoForge Data Attachments (persisted to playerdata/<UUID>.dat)
│   └── InRPLivesManager.java   ← Atomic JSON persistence for eliminated players (<world>/inrp_dead_players.json)
├── events/
│   ├── AFKEventHandler.java     ← Inactivity detection sweep, instant wake-up, optional idle kick
│   ├── ChatEventHandler.java    ← Proximity local chat routing, private message monitoring & chat spy
│   ├── LivesEventHandler.java   ← Death tracking (priority LOWEST), respawn, login reconciliation, tab tags
│   ├── RPGameplayRulesHandler.java ← Rule enforcement (PvP, indirect damage, block break, fluid placement)
│   └── ScoreboardHandler.java   ← Teams inrp_active & inrp_afk, team preservation, tab list refreshes
└── util/
    ├── ChatFormat.java          ← Layout & formatting for [L], [G], and [SPY:*] channels
    ├── ConfirmationManager.java ← Staged execution for bulk commands (5+ targets, 10s TTL)
    ├── HelpText.java            ← Help menu layout builder for /rp help and /rpadmin help
    └── LocalizationHelper.java  ← Server-side internationalization with en_us fallback

src/main/resources/assets/inrp/lang/
├── en_us.json                   ← English translations (mandatory fallback)
└── pt_br.json                   ← Brazilian Portuguese translations
```

### Side Distribution Rules

| Package | Side | Rule |
|:---|:---|:---|
| All packages (`commands/`, `config/`, `data/`, `events/`, `util/`) | **SERVER only** | In-RP is 100% server-side. No client-side code, packages, or assets exist. Never import `net.minecraft.client.*` or use `Dist.CLIENT`. |

---

## 3. Subsystem Architecture

### 3.1 Roleplay Status & Visual Identity (`ScoreboardHandler`, `InRPAttachments`)

- **Team Management**:
  - In-RP registers two native scoreboard teams dynamically: `inrp_active` (for active RP players) and `inrp_afk` (for idle players).
  - Teams are created lazily upon first use to keep the scoreboard clean if features are unused.
  - A player belongs to at most one In-RP team at any given time; AFK status takes precedence over RP mode.
- **Foreign Scoreboard Team Preservation**:
  - When joining `inrp_active` or `inrp_afk`, vanilla Minecraft forces the player out of any pre-existing team.
  - To prevent destroying player ranks, factions, or colors assigned by permissions plugins (e.g. LuckPerms) or datapacks, `ScoreboardHandler` records the original team name in the `PREVIOUS_TEAM` attachment *before* team assignment.
  - When the player leaves both RP and AFK modes, their original team membership is seamlessly restored.
- **Suffix Hierarchy & Sanitization**:
  - `InRPConfig.rpSuffix()` determines the marker text. It evaluates `nametagSuffix`, falling back to `chatSuffix` if empty.
  - Suffixes are stripped of excessive outer whitespace and strictly capped at 64 characters (`MAX_SUFFIX_LENGTH`) to protect client scoreboard packet bandwidth.
- **Tab List Name Synchronization**:
  - Status tags (`[DEAD]`, `[AFK]`) are appended to tab list display names via `LivesEventHandler.onTabListNameFormat` (`PlayerEvent.TabListNameFormat`).
  - **Critical Invariant**: Never manually construct and broadcast `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME)`. NeoForge caches the component returned by `TabListNameFormat`; manual packet broadcasts simply resend stale cached names. Call `ScoreboardHandler.refreshPlayerTabList(player)` instead, which delegates to `player.refreshTabListName()`.

### 3.2 Inactivity & AFK Engine (`AFKEventHandler`, `AFKCommand`)

- **Dual-Phase Architecture**:
  - **Inactivity Sweep (Phase 1)**: Runs once every 100 ticks (5 seconds) during `ServerTickEvent.Post`. It compares `player.getLastActionTime()` against `Util.getMillis()`. If elapsed time exceeds `afkTimeoutMillis()`, the player enters AFK silently without tick overhead.
  - **Per-Tick Wake-Up Guard (Phase 2)**: Runs during `PlayerTickEvent.Post`. It checks `!InRPAttachments.isAFK(player)` as an immediate guard clause, ensuring near-zero CPU overhead for active players.
- **Monotonic Clocks**:
  - Inactivity and timeouts strictly use `net.minecraft.Util.getMillis()`. This aligns directly with vanilla's `lastActionTime` and prevents clock corrections from resetting or triggering idle states unexpectedly.
- **Wake-Up Vectors**:
  - Wake-up is triggered by: physical movement exceeding 0.15 blocks (`distSq > 0.0225`) after a 1-second grace window, player chat, entity attacks, block interactions, or running `/afk` / `/g`.
  - When waking up, `player.resetLastActionTime()` is called immediately to prevent the next sweep from re-flagging the player.
- **Idle Kick Clamping**:
  - If `afkKickSeconds` is configured (> 0), players idle longer than `afkKickMillis()` are disconnected.
  - The kick threshold is strictly clamped to never execute below `afkTimeoutMillis()`.

### 3.3 Lives, Elimination & Revival Engine (`LivesEventHandler`, `InRPLivesManager`)

- **Death Event Interception**:
  - Intercepts `LivingDeathEvent` with **`EventPriority.LOWEST`**. This ensures third-party mods providing grave mechanics, keep-alive items, or totem effects have an opportunity to cancel death before In-RP counts it.
- **Elimination Lifecycle**:
  - When `death_count >= max_lives`, the player is marked as eliminated (`is_dead = true`) and recorded in `<world>/inrp_dead_players.json`.
  - The consequence is applied upon respawn (`PlayerRespawnEvent`) or login (`PlayerLoggedInEvent`):
    - `spectator`: Player is shifted to `GameType.SPECTATOR` and notified.
    - `kick`: Player is disconnected with a localized kick message.
- **Singleplayer & LAN Host Immunity**:
  - **Hard Invariant**: If `livesAction = "kick"`, the host of a Singleplayer or LAN game (`player.server.isSingleplayerOwner(player.getGameProfile())`) **must never be kicked**. Kicking the host aborts the integrated server and causes `session.lock` world lock issues. The host is automatically redirected to spectator mode.
- **Dual-Store Persistence & Offline Revival**:
  - While active state resides in `InRPAttachments`, an independent JSON ledger (`<world>/inrp_dead_players.json`) is maintained by `InRPLivesManager`.
  - When an admin executes `/rpadmin lives revive <player>` on an offline player, their UUID is removed from the JSON ledger.
  - Upon next login, `LivesEventHandler.onPlayerLoggedIn` detects the mismatch (JSON says alive, attachment says dead) and restores the player to survival mode automatically.

### 3.4 Proximity Local Chat, Global Chat & Chat Spy (`ChatEventHandler`, `GlobalChatCommand`, `ChatSpyCommand`)

- **Proximity Chat Routing**:
  - Standard chat (`ServerChatEvent`) is intercepted when `localChatEnabled = true`.
  - Eligible recipients are filtered using `distanceToSqr` (`localChatRadiusSq()`) within the sender's current `ServerLevel`.
  - Message format: `[L] <PlayerName> message` (via `ChatFormat.formatLocal()`).
  - If no other player is in proximity, the sender receives subtle feedback: `inrp.chat.local_no_one_heard`.
- **Bypassing Chat Signature Issues**:
  - Messages are broadcast to recipients as unsigned system messages (`recipient.sendSystemMessage()`). This avoids signature verification crashes on vanilla clients caused by altered recipient audiences.
- **Global Chat (`/g`, `/global`)**:
  - Server-wide broadcast prefixed with `[G]`.
  - Enforces a per-player cooldown (`globalChatCooldownSeconds`) tracked in a `ConcurrentHashMap` with monotonic timestamps.
- **Staff Chat Spy**:
  - Staff (OP level 2+) can toggle `/chatspy`.
  - When active, staff members receive:
    - Out-of-range local chat marked with `[SPY:L]`.
    - Vanilla private messages (`/tell`, `/msg`, `/w`) marked with `[SPY:PM]` (controlled by `spyPrivateMessages`).

### 3.5 Dice Rolling Engine (`RollCommand`)

- **Syntax & Parsing**:
  - Supports standard dice notation: `NdS` (e.g. `2d6`, `1d20`) and plain numbers: `N` (e.g. `20`).
  - Digit runs are bounded by regex (`^(\d{1,9})?[dD](\d{1,9})$`) to prevent arithmetic overflow during `Integer.parseInt`.
  - Number of dice is constrained between 1 and 100; sides between 2 and 10,000.
- **Distribution & Auditing**:
  - Results are broadcast locally within `rollProximityRadius` (or globally if radius is `-1.0`).
  - Every roll is simultaneously written to the server console log for post-incident auditing.

### 3.6 Roleplay Gameplay Restrictions (`RPGameplayRulesHandler`)

- **Combat Restrictions**:
  - Direct melee is cancelled early via `AttackEntityEvent` before swing animations or knockback occur.
  - Indirect combat (arrows, thrown tridents, splash potions of harming, ignited TNT) is caught and cancelled in `LivingIncomingDamageEvent`.
- **World Interaction Restrictions**:
  - Block breaking is intercepted in `BlockEvent.BreakEvent`.
  - Block placing is intercepted in `BlockEvent.EntityPlaceEvent`.
  - **Fluid Placement Bypass Mitigation**: Emptying fluid buckets (water/lava) into the world is intercepted in `PlayerInteractEvent.RightClickBlock` (denying item use while preserving container access) and `PlayerInteractEvent.RightClickItem` (cancelling placement when targeted at air/fluids).
- **Staff Exemption**:
  - Operators (OP level 2+) bypass all restrictions when `opBypassRestrictions = true`.

### 3.7 Administrative Staging & Confirmation Engine (`RPAdminCommand`, `ConfirmationManager`)

- **Threshold Protection**:
  - Administrative actions affecting 5 or more players (`CONFIRMATION_THRESHOLD = 5`) are intercepted and staged.
- **Staging Lifecycle**:
  - The action is wrapped into a pending execution record inside `ConfirmationManager` with a 10-second TTL measured with `Util.getMillis()`.
  - The administrator is prompted with an interactive, clickable `[CONFIRM]` component linking to `/rpadmin confirm`.
  - Targets are stored as **UUIDs** and re-resolved dynamically upon execution, preventing stale references or silent data loss if players disconnect during the confirmation window.
  - Console executions bypass confirmation and execute immediately.

---

## 4. Configuration Reference & Accessor Guarantees

All configuration lives in `<world>/serverconfig/inrp-server.toml` under `ModConfig.Type.SERVER`.

### Sanitizing Accessors (`InRPConfig`)

Raw `ConfigValue` objects should be accessed via safe wrapper methods in `InRPConfig`:

| Accessor | Behavior & Sanitization |
|:---|:---|
| `rpSuffix()` | Evaluates `nametagSuffix`, falls back to `chatSuffix`, strips whitespace, caps at 64 characters. |
| `afkTimeoutMillis()` | Converts `afkTimeoutSeconds` to milliseconds. |
| `afkKickMillis()` | Converts `afkKickSeconds` to milliseconds; clamped to never fall below `afkTimeoutMillis()`. Returns `-1` if disabled. |
| `localChatRadiusSq()` | Pre-squares `localChatRadius` for high-performance `distanceToSqr` comparisons. |
| `eliminatesByKick()` | Returns `true` if `livesAction` equals `"kick"`. |
| `logSuspiciousValues()` | Emits warnings during server startup for contradictory configuration settings. |

### Configuration Specification

| Section | Key | Type | Default | Domain / Constraints |
|:---|:---|:---|:---|:---|
| `general` | `serverLanguage` | String | `"en_us"` | Validated against regex `[a-z0-9_-]{2,32}`; falls back to `en_us`. |
| `general` | `nametagSuffix` | String | `" [in RP]"` | Stripped, capped at 64 chars; `""` disables nametag marker. |
| `general` | `chatSuffix` | String | `"[RP]"` | Fallback marker if `nametagSuffix` is empty. |
| `rules` | `pvpAllowedInRP` | bool | `true` | When `false`, blocks melee and indirect damage between/against RP players. |
| `rules` | `blockBreakAllowedInRP` | bool | `true` | When `false`, prevents RP players from breaking blocks. |
| `rules` | `blockPlaceAllowedInRP` | bool | `true` | When `false`, prevents RP players from placing blocks or emptying fluid buckets. |
| `rules` | `opBypassRestrictions` | bool | `true` | When `true`, OP level 2+ staff bypass all RP rules. |
| `roll` | `rollDefaultSides` | int | `20` | Range: `[2, 10000]` |
| `roll` | `rollProximityRadius` | double | `30.0` | Range: `[-1.0, 1000.0]` (`-1.0` = global broadcast) |
| `lives` | `livesAction` | String | `"spectator"` | Must be one of: `"spectator"`, `"kick"`. |
| `lives` | `defaultMaxLives` | int | `-1` | Range: `[-1, 100000]` (`-1` = unlimited). |
| `lives` | `countDeathsOnlyInRP` | bool | `false` | When `true`, deaths outside RP mode do not consume lives. |
| `afk` | `afkEnabled` | bool | `true` | Enables or disables the idle detection system. |
| `afk` | `afkTimeoutSeconds` | int | `300` | Range: `[10, 86400]` (5 minutes default). |
| `afk` | `afkKickSeconds` | int | `-1` | Range: `[-1, 86400]` (`-1` = disabled). |
| `afk` | `autoDisableRPOnAFK` | bool | `true` | When `true`, entering AFK automatically exits RP mode. |
| `chat` | `localChatEnabled` | bool | `true` | Converts normal chat into proximity local chat. |
| `chat` | `localChatRadius` | double | `40.0` | Range: `[5.0, 500.0]` blocks. |
| `chat` | `globalChatCooldownSeconds` | int | `3` | Range: `[0, 300]` seconds. |
| `chat` | `spyLocalChat` | bool | `true` | Mirrors out-of-range local chat to active staff spies. |
| `chat` | `spyPrivateMessages` | bool | `false` | Mirrors private messages (`/tell`, `/msg`, `/w`) to active staff spies. |

---

## 5. Invariants and Golden Rules

1. **Server Thread Stability**: No exception from any module is allowed to bubble up and crash the server tick thread.
2. **100% Server-Side Execution**: Never introduce client-side classes, renderers, keybindings, or custom network packets. Unmodified vanilla clients must always be able to join.
3. **Singleplayer & LAN Host Immunity**: Never kick the integrated server host when `livesAction = "kick"`. Always switch them to spectator mode to prevent `session.lock` world locking.
4. **Monotonic Timings**: All cooldowns, confirmation TTLs, and idle calculations must strictly use `Util.getMillis()`.
5. **Atomic File Writes**: Disk persistence (`inrp_dead_players.json`) must use `.tmp` temporary file swaps via `Files.move(..., REPLACE_EXISTING)` and explicit `UTF-8` encoding under `synchronized(LOCK)`.
6. **Complete Internationalization**: Every player-facing message must use `LocalizationHelper` and be defined in both `en_us.json` and `pt_br.json`.
7. **Scoreboard Team Isolation**: Foreign scoreboard teams must be preserved in `PREVIOUS_TEAM` and restored when players leave In-RP teams.
8. **No State Leak Across Worlds**: Reset all static registries and cooldown maps on `ServerStoppedEvent`.
9. **Surgical Additive Design**: Never rewrite functional code. Treat existing logic as load-bearing and introduce features additively with backwards-compatible defaults.

---

## 6. Testing Strategy

- **Build Verification**: Every modification must compile cleanly via `./gradlew compileJava --no-daemon` and build via `./gradlew build --stacktrace`.
- **Singleplayer / Integrated Server Testing**: Verify that dying with `livesAction = "kick"` places the host in spectator mode without disconnecting or locking the world.
- **Vanilla Client Testing**: Validate that unmodified vanilla clients can connect, receive scoreboard team suffixes, see actionbar notices, and communicate in proximity/global chat without signature errors.
- **Exploit & Edge Case Triage**: Follow the guidelines in [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) for verifying combat bypasses, fluid placement exploits, and boundary cases.
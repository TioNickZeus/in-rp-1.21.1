# CODE_REVIEW.md — In-RP Mod

> Automated review checklist for AI agents and human contributors. Based on `ARCHITECTURE.md` (SSOT) and `AGENT.md`.
> **Before reviewing any diff**: read `ARCHITECTURE.md` and `AGENT.md` in full. This document translates the mod's architectural invariants and operational standards into objective, actionable checks.

---

## How to use this document

1. Load [`ARCHITECTURE.md`](ARCHITECTURE.md) and [`AGENT.md`](AGENT.md) as primary reference context.
2. For each changed file in the diff, identify its package (`com.tio.inrp`, `commands/`, `config/`, `data/`, `events/`, `util/`, or `assets/inrp/lang/`) and apply the matching checklist sections below.
3. Verify compliance with the **Surgical Editing & Anti-Rewrite Policy** ([`AGENT.md` §4](AGENT.md#4-surgical-editing--anti-rewrite-policy)).
4. Tag every finding with a severity (see [Severities](#severities)).
5. Produce the review report following the [Expected Output Format](#expected-output-format).
6. **Never approve a PR with any `BLOCKER` item without explicit resolution/flagging.**

---

## Severities

| Level | Meaning | Action |
|:---|:---|:---|
| `BLOCKER` | Violates a core architectural invariant ([`ARCHITECTURE.md` §5](ARCHITECTURE.md#5-invariants-and-golden-rules), [`AGENT.md` §1](AGENT.md#1-core-mission--architectural-invariants)). | **Merge blocked** until resolved. |
| `HIGH` | Real risk of server crash, thread freeze, security exploit, rule bypass, or data corruption. | Requires immediate remediation. |
| `MEDIUM` | Deviation from established design patterns (e.g. business logic inside Brigadier commands, unconstrained configs, missing world-cleanup hooks). | Should be corrected before merge. |
| `LOW` | Minor edge cases, style inconsistencies, missing secondary translations (fallback exists). | Non-blocking cleanups. |
| `NIT` | Optional suggestions, comment clarifications, cosmetic readability improvements. | Informational only. |

---

## 1. 100% Server-Side Execution & Pure Vanilla Client Compatibility

Reference: [`ARCHITECTURE.md` §1, §2, §5 (*100% Server-Side Execution & Pure Vanilla Client Compatibility*)](ARCHITECTURE.md), [`AGENT.md` §1 (same invariant)](AGENT.md#1-core-mission--architectural-invariants).

- [ ] **Zero client code**: No import of `net.minecraft.client.*`, no `Dist.CLIENT`, no `@OnlyIn(Dist.CLIENT)`. Any client-side import or dependency outside development test code → **BLOCKER**.
- [ ] **Pure vanilla client compatibility**: No custom network packets, screens, renderers, custom entities, or client keybindings. Pure vanilla clients must always be able to connect and participate without client-side mods.
- [ ] **Unsigned system messages for chat**: Local chat (`[L]`), global chat (`[G]`), and staff chat spy (`[SPY:*]`) must be delivered as unsigned system messages (`sendSystemMessage()` / `PlayerList.broadcastSystemMessage`), never by re-broadcasting signed player chat (`ServerChatEvent`). Bypasses cryptographic chat signature validation crashes on vanilla clients → **BLOCKER** if violated.
- [ ] **Feedback delivery vectors**: Interactive feedback must be delivered exclusively via action bar (`player.displayClientMessage(..., true)`), sound events (`player.playNotifySound()`), native scoreboard teams (`inrp_active`, `inrp_afk`), or tab list display formatting.
- [ ] **Tab list display name updates**: Dynamic status tags (`[DEAD]`, `[AFK]`) must be appended via `PlayerEvent.TabListNameFormat`. **CRITICAL**: Never manually construct and broadcast `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME)` (stale cached component bug). Always invoke `ScoreboardHandler.refreshPlayerTabList(player)` (delegates to `player.refreshTabListName()`) → **HIGH** if manual packet is broadcast.
- [ ] **Other mods' tab names**: `LivesEventHandler.onTabListNameFormat` sets a display name only for eliminated or AFK players and never writes `null` over names set by other mods.

---

## 2. Server Tick Thread Stability & Performance

Reference: [`ARCHITECTURE.md` §5 (*Defensive Programming & Zero Server Crash Policy*, *Monotonic Timing*)](ARCHITECTURE.md), [`AGENT.md` §1 (same invariants)](AGENT.md).

- [ ] **Zero server crash policy**: No event listener (`PlayerTickEvent`, `ServerTickEvent`, `LivingDeathEvent`, `ServerChatEvent`, `BlockEvent`) may propagate an uncaught exception. All failure points must be handled defensively with graceful logging → **BLOCKER**.
- [ ] **Fast-path guard clauses on hot ticks**: Per-tick listeners (e.g. `AFKEventHandler.onPlayerTick` in `PlayerTickEvent.Post`) must exit immediately with early guard clauses (`if (!InRPAttachments.isAFK(player)) return;`) to guarantee near-zero CPU overhead for active players.
- [ ] **Sweep pacing**: Inactivity sweeps (`AFKEventHandler.onServerTick`) must not poll every tick; they must run at spaced intervals (e.g. modulo 100 ticks = 5 seconds) to prevent tick thread degradation on populated servers.
- [ ] **AFK wake-up**: `AFKEventHandler.wakeUp` calls `player.resetLastActionTime()` so the next sweep does not re-flag the player, and the chat wake-up listener uses `receiveCanceled = true` so local chat (which cancels the vanilla broadcast) still wakes the player.
- [ ] **Monotonic timing**: All durations, cooldowns (`/g`, `/afk`), confirmation TTLs, and idle calculations must strictly use `net.minecraft.Util.getMillis()`. Any use of `System.currentTimeMillis()` for elapsed time → **BLOCKER** (*Monotonic Timing* invariant).
- [ ] **Config safety**: Config reads that can run during mod construction, config lifecycle events or shutdown (`ModConfigEvent.Unloading`) must check `InRPConfig.SPEC.isLoaded()` first, as `LocalizationHelper.resolveConfiguredLanguage` does, to prevent `IllegalStateException`.
- [ ] **Thread safety & Concurrency**: In-memory registries and cooldown stores accessed across commands or events (`ConfirmationManager`, `GlobalChatCommand`, `AFKCommand`, `AFKEventHandler`) must use `ConcurrentHashMap`; `InRPLivesManager` uses explicit `synchronized(LOCK)`.
- [ ] **No state leak across worlds**: Static registries, caches, and cooldown collections must provide clean reset hooks invoked during `InRP.onServerStopped(ServerStoppedEvent)` (currently `InRPLivesManager.shutdown`, `AFKEventHandler.reset`, `AFKCommand.reset`, `GlobalChatCommand.reset`, `ConfirmationManager.reset`) → **BLOCKER** (*No State Leak Across Worlds* invariant).
- [ ] **Disk I/O on hot paths**: No new disk reads or writes in per-tick listeners. `InRPLivesManager` writes synchronously, and only when the eliminated-player set actually changes.

---

## 3. Subsystem Invariants & Security

### 3.1 Singleplayer & LAN Host Immunity (Integrated Server Safety)
Reference: [`ARCHITECTURE.md` §3.3, §5 (*Singleplayer & LAN Host Immunity from Disconnects*)](ARCHITECTURE.md#33-lives-elimination--revival-engine-liveseventhandler-inrplivesmanager), [`AGENT.md` §1 (same invariant)](AGENT.md#1-core-mission--architectural-invariants).

- [ ] **Immunity against kick**: When `livesAction = "kick"`, the host of a Singleplayer or LAN game (`player.server.isSingleplayerOwner(player.getGameProfile())`) **must never be kicked**. Kicking the host terminates the integrated server immediately and leaves `session.lock` corrupted. The host must always be gracefully switched to `GameType.SPECTATOR` with `inrp.lives.host_spectator_message` → **BLOCKER** if violated.
- [ ] **Idle kick**: In `AFKEventHandler.onServerTick`, the idle kick must always guard against kicking the singleplayer/LAN host (`player.server.isSingleplayerOwner(player.getGameProfile())`) to prevent aborting the session and corrupting `session.lock` → **BLOCKER** if violated.

### 3.2 Lives Engine, Elimination & Dual-Store Reconciliation
Reference: [`ARCHITECTURE.md` §3.3, §5 (*UUID-First Identification & Dual-Store Lives Reconciliation*, *Data Integrity & Atomic Persistence*)](ARCHITECTURE.md), [`AGENT.md` §1 (same invariants)](AGENT.md), `InRPLivesManager`.

- [ ] **Death event priority**: `LivingDeathEvent` listener must be registered with **`EventPriority.LOWEST`** so external grave, totem, or keep-alive mods have an opportunity to cancel death before In-RP processes elimination → **HIGH** if wrong priority.
- [ ] **UUID-first identification**: Player identity and death records must be keyed and persisted by `UUID`, never solely by player name.
- [ ] **Offline player revival & dual-store reconciliation**: Offline revivals (`/rpadmin lives revive <player>`) modify `<world>/inrp_dead_players.json`. Upon login, `LivesEventHandler.onPlayerLogin` must reconcile discrepancies (JSON says alive, attachment says dead) and restore the player to survival.
- [ ] **Admin death-count edits**: `/rpadmin lives setdeaths` applies the configured `livesAction` through `LivesEventHandler.enforceEliminationState`, not a hard-coded spectator switch.
- [ ] **Atomic file persistence**: Disk persistence (`<world>/inrp_dead_players.json`) must write to a temporary file (`.tmp`) first and atomic swap via `Files.move(..., REPLACE_EXISTING)`, guarded by `synchronized(LOCK)` and explicit `StandardCharsets.UTF_8`. Direct overwrite without atomic swap → **BLOCKER** (*Data Integrity & Atomic Persistence* invariant).

### 3.3 Scoreboard Teams & Foreign Team Isolation
Reference: [`ARCHITECTURE.md` §3.1, §5 (*Scoreboard Team Isolation & Foreign Team Preservation*)](ARCHITECTURE.md#31-roleplay-status--visual-identity-scoreboardhandler-inrpattachments), [`AGENT.md` §1 (same invariant)](AGENT.md).

- [ ] **Foreign team preservation**: Assigning `inrp_active` or `inrp_afk` evicts the player from their pre-existing team. `ScoreboardHandler` must preserve the original team name in `InRPAttachments.PREVIOUS_TEAM` and restore it when the player leaves both RP and AFK modes.
- [ ] **Lazy team initialization**: Scoreboard teams `inrp_active` and `inrp_afk` must be registered lazily upon first access.
- [ ] **Suffix length cap**: Marker strings evaluated by `InRPConfig.rpSuffix()` must be trimmed and capped at 64 characters (`MAX_SUFFIX_LENGTH`) to protect client scoreboard packet bandwidth.

### 3.4 Proximity Local Chat, Global Chat & Staff Spy
Reference: [`ARCHITECTURE.md` §3.4](ARCHITECTURE.md#34-proximity-local-chat-global-chat--chat-spy-chateventhandler-globalchatcommand-chatspycommand), [`KNOWN_ISSUES.md` #1](KNOWN_ISSUES.md#1--pending-testing-chat--roll-sonar-radar-detection-of-spectators--vanished-staff).

- [ ] **Level isolation & squared distance**: Proximity recipients must be filtered strictly within the sender's current `ServerLevel` using `distanceToSqr` against pre-squared radius (`localChatRadiusSq()`).
- [ ] **Local feedback**: If no other player is within the radius, the sender receives `inrp.chat.local.no_one_heard`.
- [ ] **Spectator radar**: A diff that changes how recipients are counted for `no_one_heard` (`ChatEventHandler.onServerChat`, `RollCommand.broadcastRoll`) must keep, or consciously correct, how spectators are counted: see KNOWN_ISSUES.md — Chat & Roll "Sonar Radar": Detection of Spectators / Vanished Staff (#1).
- [ ] **Global chat throttling**: `/g` and `/global` enforce `globalChatCooldownSeconds` via monotonic tracking (`Util.getMillis()`).
- [ ] **Staff surveillance**: `/chatspy` toggles must enforce OP level 2+ permission and properly format spy channels (`[SPY:L]`, `[SPY:PM]`); delivery re-checks the operator level (`ChatFormat.isActiveChatSpy`).
- [ ] **Message length cap**: Chat and `/g` messages go through `ChatFormat.sanitize` with a 256-character limit.

### 3.5 Dice Rolling Engine (`/roll`)
Reference: [`ARCHITECTURE.md` §3.5](ARCHITECTURE.md#35-dice-rolling-engine-rollcommand), `RollCommand`.

- [ ] **Input sanitization & overflow prevention**: Dice notation (`NdS`) must be bounded by regex (`^(\d{1,9})?[dD](\d{1,9})$`) and parsed with range checks before `Integer.parseInt` to avoid integer overflow → **HIGH** if unconstrained.
- [ ] **Bound constraints**: Dice count constrained to `[1, 100]`; sides constrained to `[2, 10000]`.
- [ ] **Audit logging**: Every executed roll must be logged to the server console log for post-incident administrative auditing.
- [ ] **Proximity & radius**: Results broadcast locally within `rollProximityRadius` (or globally if `-1.0`).

### 3.6 Roleplay Gameplay Rules & Exploit Mitigations
Reference: [`ARCHITECTURE.md` §3.6](ARCHITECTURE.md#36-roleplay-gameplay-restrictions-rpgameplayruleshandler), [`KNOWN_ISSUES.md` #2, #3](KNOWN_ISSUES.md), `RPGameplayRulesHandler`.

- [ ] **Combat restrictions**: Direct melee attacks cancelled in `AttackEntityEvent`. Indirect damage (arrows, tridents, splash potions of harming, ignited TNT) intercepted and cancelled in `LivingIncomingDamageEvent`.
- [ ] **Block interaction restrictions**: Block breaking cancelled in `BlockEvent.BreakEvent`. Block placing cancelled in `BlockEvent.EntityPlaceEvent`.
- [ ] **Fluid bucket placement mitigation**: Emptying water/lava buckets must be intercepted in both `PlayerInteractEvent.RightClickBlock` (setting `event.setUseItem(TriState.FALSE)` to prevent fluid placement while preserving block/container access) and `PlayerInteractEvent.RightClickItem` (cancelling use of fluid-placing items). Must support both vanilla `BucketItem` and NeoForge `FluidUtil` → **HIGH** if fluid placement exploit is re-opened.
- [ ] **OP bypass**: Staff (OP level 2+) bypass all RP gameplay restrictions when `opBypassRestrictions = true`.

### 3.7 Administrative Staging & Confirmation Engine
Reference: [`ARCHITECTURE.md` §3.7](ARCHITECTURE.md#37-administrative-staging--confirmation-engine-rpadmincommand-confirmationmanager), `ConfirmationManager`, `RPAdminCommand`.

- [ ] **Bulk threshold safeguard**: Bulk operations affecting 5 or more players (`RPAdminCommand.CONFIRMATION_THRESHOLD = 5`) must be staged into `ConfirmationManager` rather than executed directly. `lives revive` does not go through staging: see KNOWN_ISSUES.md — `/rpadmin lives revive` Bypasses Bulk Confirmation Staging (#9).
- [ ] **Monotonic TTL**: Staged actions must expire after 10 seconds (`ConfirmationManager.EXPIRY_SECONDS = 10L`) measured with `Util.getMillis()`.
- [ ] **Dynamic UUID resolution**: Target players must be staged by `UUID` and re-resolved at execution time to prevent executing against stale player instances or disconnections.
- [ ] **Each bulk command staged on its own**: A pending action must never let another bulk command run unconfirmed.
- [ ] **Non-player exemption**: Sources without a player UUID (console, command blocks) bypass staging and execute immediately.

---

## 4. General Invariants Checklist (AGENT.md §1)

Direct, item-by-item check — any violation is a **BLOCKER**:

- [ ] **1. 100% Server-Side Execution & Pure Vanilla Client Compatibility** — Pure vanilla Minecraft clients can connect without installing any client mod. Zero client packages, renderers, packets, or keybindings.
- [ ] **2. Singleplayer & LAN Host Immunity from Disconnects** — Host is never kicked when `livesAction = "kick"`, switching to spectator mode instead to avoid `session.lock` corruption.
- [ ] **3. Defensive Programming & Zero Server Crash Policy** — No uncaught exception can bubble up and crash the server tick thread; inputs are bounded and config values are typed.
- [ ] **4. UUID-First Identification & Dual-Store Lives Reconciliation** — UUIDs are used for all player tracking; offline revivals sync cleanly on login.
- [ ] **5. Monotonic Timing (`Util.getMillis()`)** — All cooldowns, confirmation TTLs, and idle calculations use `Util.getMillis()`, never `System.currentTimeMillis()`.
- [ ] **6. Data Integrity & Atomic Persistence** — Disk persistence uses `.tmp` swap + `Files.move(..., REPLACE_EXISTING)` under `synchronized(LOCK)` with explicit UTF-8.
- [ ] **7. Scoreboard Team Isolation & Foreign Team Preservation** — Foreign scoreboard teams are preserved in `PREVIOUS_TEAM` and restored when leaving RP/AFK.
- [ ] **8. No State Leak Across Worlds** — All static registries and cooldown collections are cleared in `InRP.onServerStopped`.
- [ ] **9. Zero Hardcoded Player-Facing Text** — All player-facing messages use `LocalizationHelper` and are defined in **both** `en_us.json` and `pt_br.json`.
- [ ] **10. Additive & Backward-Compatible Design** — Existing commands, configs, and save-data fields stay backward-compatible; new features are additive.

---

## 5. Surgical Editing & Code Hygiene (AGENT.md §4)

Reference: [`AGENT.md` §4 "Surgical Editing & Anti-Rewrite Policy"](AGENT.md#4-surgical-editing--anti-rewrite-policy).

- [ ] **Minimal diff principle**: Diff touches only lines strictly required for the feature/fix. No cosmetic reformatting, unnecessary import reordering, or arbitrary variable renaming.
- [ ] **No drive-by refactoring**: Unrelated code smells or dead code are logged in [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) or [`ROADMAP.md`](ROADMAP.md), not modified in the same diff.
- [ ] **Preserve public contracts**: Method signatures, attachments (`InRPAttachments`), config keys, and command syntax remain intact unless explicitly required.
- [ ] **Brigadier argument bounds**: Commands enforce strict bounds on all arguments (e.g. `IntegerArgumentType.integer(-1, 100000)`).
- [ ] **Config accessors**: New config options expose sanitizing accessors in `InRPConfig` and use typed bounds (`defineInRange`, `defineInList`).
- [ ] **`defineInList` values**: Use `Arrays.asList`, not `List.of`, because NeoForge's config correction calls `contains(null)` (startup NPE fixed in 1.0.8; covered by `InRPConfigTest`).
- [ ] **Config strings in resource paths**: Values such as `serverLanguage` are validated (`LocalizationHelper.SAFE_LANGUAGE_CODE`) before being interpolated into a classpath path.
- [ ] **Clean compilation and tests**: Code compiles with `./gradlew compileJava --no-daemon` with 0 errors and `./gradlew test` passes.

---

## Expected Output Format

Upon completing a code review, produce the report using this markdown structure:

```markdown
## Summary
<1-3 sentences summarizing the diff, purpose of changes, and components affected>

## Findings

### BLOCKER
- `path/File.java:line` — Description of violation, referenced invariant by name (e.g. AGENT.md §1 — Singleplayer & LAN Host Immunity from Disconnects)

### HIGH
- `path/File.java:line` — Description of risk, failure scenario, or security bypass

### MEDIUM
- `path/File.java:line` — Architectural deviation or missing edge-case handling

### LOW / NIT
- `path/File.java:line` — Minor improvement, style, or optional suggestion

## Invariant Verification
- [x] 1. 100% Server-Side Execution & Pure Vanilla Client Compatibility
- [x] 2. Singleplayer & LAN Host Immunity from Disconnects
- [x] 3. Defensive Programming & Zero Server Crash Policy
- [x] 4. UUID-First Identification & Dual-Store Lives Reconciliation
- [x] 5. Monotonic Timing (`Util.getMillis()`)
- [x] 6. Data Integrity & Atomic Persistence
- [x] 7. Scoreboard Team Isolation & Foreign Team Preservation
- [x] 8. No State Leak Across Worlds
- [x] 9. Zero Hardcoded Player-Facing Text
- [x] 10. Additive & Backward-Compatible Design

## Verdict
[ ] Approved with no reservations
[ ] Approved with comments (no BLOCKER)
[ ] Blocked — requires fixes before merge
```

*(Omit any severity section that contains no findings.)*

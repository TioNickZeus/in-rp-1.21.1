# AGENT.md — AI Agent & Developer Operating Manual

> **Scope**: Operating standards, Git Flow conventions, and development invariants for AI coding agents and contributors working on **In-RP** (`in-rp-1.21.1`).
> **Repository**: [TioNickZeus/in-rp-1.21.1](https://github.com/TioNickZeus/in-rp-1.21.1)
> **Mod Loader**: NeoForge 1.21.1 (FML) | **Java**: 21 (Temurin)

---

## 1. Core Mission & Architectural Invariants

Every AI agent operating in this repository **must strictly preserve** these core invariants:

1. **100% Server-Side Execution & Pure Vanilla Client Compatibility**:
   - Pure vanilla Minecraft clients **must always** be able to connect without installing NeoForge or any client-side mod.
   - Never register client-side screens, GUIs, renderers, client network packets, or client keybindings.
   - Deliver UI/UX exclusively through vanilla mechanics: `player.sendSystemMessage()`, `player.displayClientMessage(..., true)` (action bar), native scoreboard teams (`inrp_active`, `inrp_afk`), and tab list formatting via `TabListNameFormat`.
   - Deliver proximity and global chat as unsigned system messages (`sendSystemMessage()`) rather than re-broadcasting player chat, avoiding client-side chat signature validation failures on vanilla clients.
2. **Singleplayer & LAN Host Immunity from Disconnects**:
   - When `livesAction = "kick"`, the host of a Singleplayer/LAN session (`player.server.isSingleplayerOwner(player.getGameProfile())`) **must never be kicked**.
   - Kicking the host terminates the integrated server immediately and risks leaving `session.lock` in a corrupted state.
   - The host must always be gracefully switched to `GameType.SPECTATOR` with `inrp.lives.host_spectator_message` instead.
3. **Defensive Programming & Zero Server Crash Policy**:
   - The mod must **never crash the server tick thread** under any circumstance.
   - Bound and sanitize all inputs: Brigadier commands must enforce strict bounds (`IntegerArgumentType.integer(-1, 100000)`), and config specs must use typed limits (`defineInRange` or `defineInList`).
   - Always check `InRPConfig.SPEC.isLoaded()` before reading config values to avoid `IllegalStateException` during server bootstrapping or shutdown (`ModConfigEvent.Unloading`).
   - Catch and log I/O failures gracefully in `InRPLivesManager`.
4. **UUID-First Identification & Dual-Store Lives Reconciliation**:
   - Player identity must **always** be resolved and persisted by `UUID`, never solely by player name.
   - Offline revival works via `inrp_dead_players.json` independently of online player data attachments.
   - During login, state reconciliation must ensure that if the external JSON file marks a player as alive while the player's attachment marks them as dead, the player is restored cleanly.
5. **Monotonic Timing (`Util.getMillis()`)**:
   - All durations, cooldowns (e.g. `/g` cooldown, `/afk` anti-spam), confirmation TTL (10s in `ConfirmationManager`), and idle detection must use `net.minecraft.Util.getMillis()`, never wall-clock `System.currentTimeMillis()`.
6. **Data Integrity & Atomic Persistence**:
   - Storage files (`<world>/inrp_dead_players.json`) must be written atomically using temporary file swaps (`Files.move` with `REPLACE_EXISTING`) and guarded by thread synchronization (`synchronized(LOCK)`).
   - Character sets must explicitly be `StandardCharsets.UTF_8`.
7. **Scoreboard Team Isolation & Foreign Team Preservation**:
   - Vanilla's `addPlayerToTeam` evicts a player from their current team. `ScoreboardHandler` must preserve the pre-existing team in the `PREVIOUS_TEAM` attachment and restore it upon leaving RP/AFK.
8. **No State Leak Across Worlds**:
   - A single JVM can load several worlds in sequence (single player, or a server reload). Every static collection holding world- or session-scoped state must expose a reset hook called by `InRP.onServerStopped`.
9. **Zero Hardcoded Player-Facing Text**:
   - All messages rendered to players or staff must go through `LocalizationHelper.getMessage()` or `LocalizationHelper.getPrefixedMessage()`.
   - Every translation key must exist in both `src/main/resources/assets/inrp/lang/en_us.json` and `pt_br.json`.
10. **Additive & Backward-Compatible Design**:
    - Existing commands, configs, and save-data fields must remain backwards-compatible. New features must be additive.

---

## 2. Git Flow & GitHub Collaboration Standards

All contributions follow this Git Flow standard:

### 2.1 Branch Naming Conventions

Never commit directly to `main`. Always branch off from the latest `origin/main`.

| Branch Type | Prefix | Example | Purpose |
|:---|:---|:---|:---|
| **Feature** | `feat/` | `feat/expression-commands`, `feat/life-transfer` | New user-facing mechanics, commands, or configs. |
| **Bug Fix** | `fix/` | `fix/water-afk-loop`, `fix/roll-cooldown` | Resolving bugs, edge cases, crashes, or exploits. |
| **Chore / Maintenance** | `chore/` | `chore/hardening-pass`, `chore/bump-deps` | Refactoring, build scripts, workflows, or cleanup. |
| **Documentation** | `docs/` | `docs/update-architecture` | Documentation additions or revisions without code changes. |
| **Performance** | `perf/` | `perf/optimize-local-chat` | Measurable performance or memory allocation improvements. |

### 2.2 Commit Message Standards (Conventional Commits)

```text
<type>(<scope>): <imperative summary in present tense>

- Detailed bullet point explaining the "why" and "what"
- Reference to any issue or config key affected
```

**Allowed Types**: `feat`, `fix`, `docs`, `refactor`, `perf`, `chore`, `test`.  
**Examples**:
- `fix(lives): protect singleplayer host from elimination kick and world lock`
- `feat(chat): add proximity-based local chat routing and staff chat spy`
- `fix(protection): intercept bucket fluid placement when block placing is disabled`

---

## 3. Pull Request (PR) Workflow

### 3.1 Pre-PR Verification Checklist

Before creating a Pull Request, the agent must verify:

- [ ] **Compilation**: `./gradlew compileJava --no-daemon` passes with 0 errors.
- [ ] **Full Build & Artifact**: `./gradlew build --stacktrace` passes.
- [ ] **Server-Side Safety**: No client imports or dependencies added (`Dist.CLIENT`, renderers, client screens).
- [ ] **Translations**: Any new message key exists in both `en_us.json` and `pt_br.json`.
- [ ] **No Stale References**: Config names and permissions match `InRPConfig.java` and `ARCHITECTURE.md`.
- [ ] **Changelog**: An entry is added to `CHANGELOG.md` under `## [Unreleased]` or the targeted version.
- [ ] **Documentation**: `README.md`, `ROADMAP.md`, and `ARCHITECTURE.md` are updated if public behavior changed.

### 3.2 Pull Request Structure

Every PR must be opened against `main` and include:

```markdown
### Summary
Brief 1-3 sentence summary of the problem solved and the approach taken.

### Changes Made
- **[Component/File]**: Description of change.

### Verification & Testing
- How this change was verified (e.g. `./gradlew compileJava`, manual testing with LAN host).

### Breaking Changes / Invariants
- State whether any config defaults or behaviors changed (should be "None").
```

---

## 4. Surgical Editing & Anti-Rewrite Policy

Working code is a liability to break, not an invitation to improve. Every agent must treat existing functional code as **load-bearing until proven otherwise**, and edit it with the smallest possible footprint.

1. **Minimal Diff Principle**:
   - Touch only the lines strictly required to satisfy the task. Do not reformat, reorder imports, rename variables, or "clean up" code that isn't part of the requested change.
   - Prefer targeted patches (find-and-replace on the exact block) over regenerating a whole file, class, or method — even when it would be faster to write from scratch.
   - If the required change touches more than ~30% of a file, stop and confirm scope with the maintainer before proceeding.
2. **No Drive-By Refactors**:
   - Noticing an unrelated smell, dead code, or a "better way to do it" while working on a feature/fix is **not** license to change it in the same commit.
   - Log it instead: add an entry to `KNOWN_ISSUES.md` (if it's a risk) or `ROADMAP.md` (if it's an improvement), and leave the code untouched.
   - Refactors are only performed in a dedicated `refactor/` or `chore/` branch, requested explicitly.
3. **Read Before You Write**:
   - Always read the full method/class — and, when relevant, its call sites — before editing it. Never patch code you haven't fully read in the current session.
   - Re-read a file immediately before editing it if any prior edit in the session may have changed its state.
4. **Preserve Public Contracts**:
   - Method signatures, attachment types (`InRPAttachments`), config keys, OP level requirements, and command syntax must not change unless the task explicitly requires it.
   - If a signature or config key must change, document every call site updated and flag it under "Breaking Changes / Invariants" in the PR.
5. **Match Existing Patterns, Don't Impose New Ones**:
   - Follow the file's existing style, naming, and idioms even if the agent would personally structure it differently. Consistency with the surrounding codebase outranks personal/model preference.
   - New patterns are only introduced with explicit approval, and then applied consistently.
6. **One Concern Per Change**:
   - A single commit/PR addresses one feature, one fix, or one refactor — never a mix.
7. **Justify Behavioral Changes**:
   - If a change alters observable behavior (timing, message wording, default config value, command output), state the "why" explicitly in the commit body and PR description — never as a silent side effect of a rewrite.
8. **When In Doubt, Ask — Don't Rewrite**:
   - If the correct minimal edit is unclear, ask the maintainer rather than defaulting to a broader rewrite "to be safe." A rewrite is never the safe option for functional code.

---

## 5. Exploit & Bug Triage Policy (`KNOWN_ISSUES.md`)

When handling potential game exploits, combat balance issues, or complex moderation edge cases:

1. **Reproduction Before Fixing**:
   - Do **not** blindly jump to code changes for complex game mechanics (such as spectator detection or AFK evasion).
   - Document the issue in [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) under `[Pending Testing]` with step-by-step reproduction instructions for live testing with friends.
2. **Post-Testing Confirmation**:
   - Once verified in-game, update status to `[Confirmed]`, design the mitigation, create a dedicated `fix/*` branch, and implement the fix.
3. **Resolve and Document**:
   - After testing the fix, update `KNOWN_ISSUES.md` to `[Resolved]` and record the fix in `CHANGELOG.md`.

---

## 6. General Operating Discipline

1. **Fail Loud, Not Silent**: if requirements, config defaults, or expected behavior are ambiguous, ask before proceeding. Never guess silently and ship a plausible-looking assumption.
2. **No Scope Creep**: implement exactly what was asked. Additional features, extra config options, or "while I'm here" additions belong in `ROADMAP.md`, not in the diff.
3. **Invariant Guard**: before finalizing any change, re-check it against the invariants in §1. A change that satisfies the immediate request but violates an invariant is not acceptable.
4. **Verify, Don't Assume**: run `./gradlew compileJava --no-daemon` after any non-trivial edit, not only before opening the PR. Don't assume a change compiles because it "looks right."
5. **Idempotent, Reversible Actions**: prefer changes that are easy to revert cleanly (a self-contained commit) over changes entangled with unrelated edits.
6. **Traceability**: every non-obvious decision gets one line in the commit body or PR description.
7. **Respect the SSOT**: `ARCHITECTURE.md` is authoritative for structure, `AGENT.md` for process. If code and docs disagree, flag the discrepancy rather than silently trusting either one.

---

## 7. Key File Index

| File | Purpose | Rule for Agents |
|:---|:---|:---|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Single Source of Truth (SSOT) for technical architecture | **Read first** before writing code. Update when packages/data change. |
| [`ROADMAP.md`](ROADMAP.md) | Backlog and planned feature board | Check before suggesting "new" features. Mark done items. |
| [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) | Triage board for edge cases and exploits | Log reproduction guides for unconfirmed bugs here. |
| [`CHANGELOG.md`](CHANGELOG.md) | Version history and release notes | Document every user-facing or architectural change. |
| [`gradle.properties`](gradle.properties) | Mod metadata and version | Update `mod_version` upon release preparation. |
| [`InRP.java`](src/main/java/com/tio/inrp/InRP.java) | Mod entrypoint (`@Mod`) | Keep lightweight; delegate logic to handlers and commands. |
| [`InRPConfig.java`](src/main/java/com/tio/inrp/config/InRPConfig.java) | Server configuration specifications | Check `.isLoaded()`; prefer sanitizing accessors. |
| [`InRPAttachments.java`](src/main/java/com/tio/inrp/data/InRPAttachments.java) | NeoForge player data attachments | Ensure `copyOnDeath()`; null-safe accessors. |
| [`InRPLivesManager.java`](src/main/java/com/tio/inrp/data/InRPLivesManager.java) | External JSON persistence for dead players | Thread-safe (`synchronized(LOCK)`) with `.tmp` atomic writes. |
| [`ScoreboardHandler.java`](src/main/java/com/tio/inrp/events/ScoreboardHandler.java) | Native scoreboard team & tab list renderer | Preserves `PREVIOUS_TEAM`; never send manual update packets. |
| [`AFKEventHandler.java`](src/main/java/com/tio/inrp/events/AFKEventHandler.java) | Idle detection and wake-up listeners | 100-tick sweep interval; monotonic timing (`Util.getMillis()`). |
| [`ChatEventHandler.java`](src/main/java/com/tio/inrp/events/ChatEventHandler.java) | Proximity local chat & staff chat spy | Unsigned system messages; respects dimension boundaries. |
| [`LivesEventHandler.java`](src/main/java/com/tio/inrp/events/LivesEventHandler.java) | Death cycle, respawn, login reconciliation | `EventPriority.LOWEST` on death; host immunity on kick. |
| [`RPGameplayRulesHandler.java`](src/main/java/com/tio/inrp/events/RPGameplayRulesHandler.java) | Rule enforcement (PvP, block break/place) | Intercepts indirect combat & fluid placement bypasses. |
| [`ConfirmationManager.java`](src/main/java/com/tio/inrp/util/ConfirmationManager.java) | Staged execution for bulk commands (5+ targets) | 10s monotonic TTL; stages targets as UUIDs. |
# 🤖 AGENT.md — AI Agent & Developer Operating Manual

> **Scope**: Operating standards, Git Flow conventions, and development invariants for AI coding agents and contributors working on **In-RP** (`in-rp-1.21.1`).
> **Repository**: [TioNickZeus/in-rp-1.21.1](https://github.com/TioNickZeus/in-rp-1.21.1)
> **Mod Loader**: NeoForge 1.21.1 (FML) | **Java**: 21 (Temurin)

---

## 🎯 1. Core Mission & Architectural Invariants

Every AI agent operating in this repository **must strictly preserve** these core invariants:

1. **100% Server-Side Execution**:
   - Pure vanilla Minecraft clients **must always** be able to connect without installing NeoForge or any client-side mod.
   - Never register client-side screens, GUIs, renderers, client network packets, or client keybindings.
   - Deliver UI/UX via vanilla mechanics: `player.sendSystemMessage()`, `player.displayClientMessage(..., true)` (actionbar), `ScoreboardHandler` teams, and tab list headers.
2. **Defensive Programming & Zero Server Crashes**:
   - The mod must **never crash the server thread** under any circumstance.
   - Bound and sanitize all inputs: Brigadier commands use strict bounds (`IntegerArgumentType.integer(-1, 100000)`), and config specs use `defineInRange` or `defineInList`.
   - Always check `InRPConfig.SPEC.isLoaded()` before reading config values to avoid `IllegalStateException` during server bootstrapping or shutdown (`ModConfigEvent.Unloading`).
   - Catch and log I/O failures gracefully in `InRPLivesManager`.
3. **Monotonic Timing (`Util.getMillis()`)**:
   - All elapsed-time comparisons (cooldowns, confirmation TTL, idle detection) must use `net.minecraft.Util.getMillis()`, never `System.currentTimeMillis()`.
4. **Singleplayer & LAN Host Immunity from Disconnects**:
   - When `livesAction = "kick"`, the host of a Singleplayer/LAN session (`player.server.isSingleplayerOwner(player.getGameProfile())`) **must never be kicked**. The host must be gracefully placed into `GameType.SPECTATOR` with `inrp.lives.host_spectator_message` to keep the integrated server running and prevent `session.lock` world locking.
5. **Zero Hardcoded Player-Facing Text**:
   - All text rendered to players must use `LocalizationHelper.getMessage()` or `LocalizationHelper.getPrefixedMessage()`.
   - Every new key must be defined in **both** `src/main/resources/assets/inrp/lang/en_us.json` and `pt_br.json`.
6. **Additive Design (No Destructive Refactoring)**:
   - Existing commands, configs, and save-data fields must remain backwards compatible. New features must be additive.

---

## 🌿 2. Git Flow & GitHub Collaboration Standards

All contributions, whether from AI agents or human contributors, follow this Git Flow standard tailored for GitHub.

### 2.1 Branch Naming Conventions

Never commit directly to `main`. Always branch off from the latest `origin/main`.

| Branch Type | Prefix | Example | Purpose |
|:---|:---|:---|:---|
| **Feature** | `feat/` | `feat/expression-commands` | New user-facing mechanics, commands, or configs. |
| **Bug Fix** | `fix/` | `fix/worllockfix`, `fix/sonar-radar` | Resolving bugs, edge cases, crashes, or exploits. |
| **Chore / Maintenance** | `chore/` | `chore/hardening-pass`, `chore/bump-deps` | Refactoring, build scripts, workflows, or cleanup. |
| **Documentation** | `docs/` | `docs/update-architecture` | Documentation additions or revisions without code changes. |
| **Performance** | `perf/` | `perf/optimize-local-chat` | Measurable performance or memory allocations improvements. |

### 2.2 Commit Message Standards (Conventional Commits)

Commit messages must be concise, informative, and follow the Conventional Commits structure:

```text
<type>(<scope>): <imperative summary in present tense>

- Detailed bullet point explaining the "why" and "what"
- Reference to any issue or config key affected
```

**Allowed Types**: `feat`, `fix`, `docs`, `refactor`, `perf`, `chore`, `test`.  
**Examples**:
- `fix(lives): protect singleplayer host from elimination kick and world lock`
- `feat(chat): add proximity-based local chat routing and staff chat spy`
- `docs(triage): add known issues ledger with reproduction guides`

---

## 🔄 3. Pull Request (PR) Workflow

### 3.1 Pre-PR Verification Checklist

Before creating a Pull Request or proposing merge commits, the agent must verify:

- [ ] **Compilation**: `./gradlew compileJava --no-daemon` passes with 0 errors.
- [ ] **Full Build & Artifact**: `./gradlew build --stacktrace` passes (verifies resource processing and JAR packaging).
- [ ] **Translations**: Any new message key exists in both `en_us.json` and `pt_br.json`.
- [ ] **No Stale References**: Verify that config names and permissions match `InRPConfig.java` and `ARCHITECTURE.md`.
- [ ] **Changelog**: An entry is added to `CHANGELOG.md` under `## [Unreleased]` or the targeted semantic version.
- [ ] **Documentation**: `README.md`, `ROADMAP.md`, and `ARCHITECTURE.md` are updated if public behavior changed.

### 3.2 Pull Request Structure

Every PR must be opened against `main` and include:

```markdown
### 📝 Summary
Brief 1-3 sentence summary of the problem solved and the approach taken.

### 🔍 Changes Made
- **[Component/File]**: Description of change.
- **[Component/File]**: Description of change.

### 🧪 Verification & Testing
- How this change was verified (e.g., `./gradlew compileJava`, manual testing with LAN host).
- Reference to test steps in `KNOWN_ISSUES.md` if applicable.

### ⚠️ Breaking Changes / Invariants
- State whether any config defaults or behaviors changed (should be "None").
```

---

## 🧪 4. Exploit & Bug Triage Policy (`KNOWN_ISSUES.md`)

When handling potential game exploits, combat balance issues, or complex moderation edge cases:

1. **Reproduction Before Fixing**:
   - Do **not** blindly jump to code changes for complex game mechanics (such as spectator detection or AFK evasion).
   - Document the issue in [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) under `[Pending Testing]` with step-by-step reproduction instructions for live testing with friends.
2. **Post-Testing Confirmation**:
   - Once verified in-game, update status to `[Confirmed]`, design the mitigation, create a dedicated `fix/*` branch, and implement the fix.
3. **Resolve and Document**:
   - After testing the fix, update `KNOWN_ISSUES.md` to `[Resolved]` and record the fix in `CHANGELOG.md`.

---

## 🗂️ 5. Key File Index

| File | Purpose | Rule for Agents |
|:---|:---|:---|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Single Source of Truth (SSOT) for technical architecture | **Read first** before writing code. Update when packages/data change. |
| [`ROADMAP.md`](ROADMAP.md) | Backlog and planned feature board | Check before suggesting "new" features. Mark done items. |
| [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) | Triage board for edge cases and exploits | Log reproduction guides for unconfirmed bugs here. |
| [`CHANGELOG.md`](CHANGELOG.md) | Version history and release notes | Document every user-facing or architectural change. |
| [`gradle.properties`](gradle.properties) | Mod metadata and version | Update `mod_version` upon release preparation. |
| [`InRP.java`](src/main/java/com/tio/inrp/InRP.java) | Mod entrypoint (`@Mod`) | Keep lightweight; delegate logic to handlers and commands. |

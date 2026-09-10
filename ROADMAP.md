# 🗺️ Roadmap & Future Ideas — In-RP

This document serves as a central board to track ideas, suggestions, quality-of-life improvements, and planned features for upcoming versions of **In-RP**.

---

## 📌 Immediate Backlog

- [x] **Include `/afk` in `/rp help`**: Added display entry for `inrp.help.afk` in the `showHelp` method of [RPCommand.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/commands/RPCommand.java) (bundled with `en_us.json` and `pt_br.json`).
- [x] **Tab List Synchronization after Revive**: fixed, and it was never a vanilla client cache problem. `PlayerEvent.TabListNameFormat` is fired from `ServerPlayer.refreshTabListName()`, which caches the result in `tabListDisplayName`; `ScoreboardHandler.refreshPlayerTabList` used to broadcast `ClientboundPlayerInfoUpdatePacket(UPDATE_DISPLAY_NAME, ...)` directly, and that packet only re-serialises the **cached** value — so the tag never recomputed and a revived player kept `[DEAD]` until reconnecting. `refreshPlayerTabList` now delegates to `refreshTabListName()`, which recomputes the name and broadcasts it itself, only when it actually changed.
- [x] **Preserve foreign scoreboard teams**: done. `ScoreboardHandler` records the player's team in the `PREVIOUS_TEAM` attachment before moving them onto `inrp_active` / `inrp_afk`, and restores it when they leave both. Servers using teams for rank prefixes (LuckPerms, datapacks, manual `/team join`) no longer lose that membership.
- [ ] **Bugfix & Exploit Hardening (v1.0.7)**: Active testing ledger in [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for live server validation (Spectator/Vanish Sonar Radar, indirect PvP damage vectors, block interaction edge cases, and `/roll` rate limiting).

---

## 💬 Local & Global Chat

- [x] **Default Local Chat (Proximity)**:
  - Standard in-game chat (pressing `T` and sending a message) is delivered locally based on proximity (configurable radius in TOML, default: 40 blocks).
  - Only players within the radius receive the chat message (e.g. `[L] Player: message`).
  - Subtle feedback notification when nobody is around: `(Nobody nearby heard you)`.
  - Config toggles and options: `localChatEnabled`, `localChatRadius`.
- [x] **`/g <message>` (or `/global`)**: Global server chat when local chat is enabled by default. Broadcasts to all server players with `[G]` prefix and configurable anti-flood cooldown.
- [x] **Moderation Chat Spy (`/rpadmin spy` or `/chatspy`)**:
  - Dedicated tool for staff and administration to monitor channels in real time.
  - Toggle command per admin (OP level 2+ permission).
  - Monitors:
    - Local chat messages outside admin proximity range (with discrete tag `[SPY:L]`).
    - Vanilla private messages (`/tell`, `/msg`, `/w`) between players to deter metagaming and rule-breaking (`[SPY:PM] PlayerA -> PlayerB: text`).
  - Dedicated TOML configurations for staff control (`spyLocalChat = true`, `spyPrivateMessages = true`).

---

## 💡 Roleplay Mechanics Ideas (Gameplay)

### 1. Expression Commands
- [ ] **`/off <message>` (or `/b`)**: Speak Out-Of-Character (OOC) while remaining in RP mode. The message displays with a distinct neutral tag (e.g. `(( [OFF] Player: message ))` in gray), ideal for quick player notes without having to toggle `/rp`.
- [ ] **`/do <description>`**: Third-person narration of scene or environment events (e.g. `[SCENE] The horse appears exhausted after the long journey.`).
- [ ] **`/sussurro <message>` (Whisper)**: Low-voice speech with an ultra-short proximity radius (e.g. 3 to 5 blocks) for secretive nearby conversations (unlike vanilla `/msg`/`/tell` which is global and private).

### 2. Character & Identity System
- [ ] **Character Name (RP Name)**: Allow setting a fictional character name (e.g. `/rp name <Firstname Lastname>`), replacing or complementing the Minecraft username in chat and overhead nametag.
- [ ] **Quick Profile (`/rp profile [player]`)**: Formatted message or book/chest displaying basic character info: age, occupation/profession, brief bio, and status.
- [ ] **Mood / State Status**: Short player status indicator (e.g. `Injured`, `Busy`, `Traveling`).

### 3. Lives & Survival
- [ ] **Life Transfer (`/lives transfer <player> [amount]`)**: Allow players to donate or share lives with each other (ideal for sacrifice mechanics, ritual healing, or clans).
- [ ] **Audio/Visual Effect on Life Loss**: Dramatic sound and particle effect to alert nearby players when someone loses one of their limited lives.
- [ ] **Revival Items**: Support for a custom consumable item (or totem/heart) that revives an eliminated player when used by another player.

---

## 🛡️ Administration & Moderation

- [ ] **Audit Logging (`inrp_audit.log`)**: Dedicated log file recording critical admin actions (revives, life alterations, resets) for easy staff monitoring.
- [ ] **LuckPerms / NeoForge Permissions Integration**: Fine-grained permission nodes (`inrp.command.roll`, `inrp.command.lives`, `inrp.admin.*`) beyond default vanilla OP level 2.
- [ ] **Command `/rpadmin inspect <player>`**: Comprehensive admin panel to inspect everything about a player in one view (RP status, AFK, lives, deaths, current coordinates).

---

## 🚀 Future Vision: "In-RP Companion / Addon" Ecosystem

> **Planning Phase:** To be developed **only after** the core mod (`In-RP Core`) is mature, complete, and stable.

The vision of this future project is to expand roleplay boundaries when both the server and players opt for a full modded experience:

* **Role of `In-RP` (Core):** Remains the foundational mod for rules, logic, lives, dice, and commands, maintaining 100% server-side operation for vanilla clients.
* **Role of `In-RP Companion` (Client & Server Mod):**
  - Installed **on both server and client** for players opting for the extended experience.
  - **Custom Blocks & Items:** Decorative roleplay blocks, consumables (e.g. life contracts, revival potions/totems, RPG coins).
  - **Graphical User Interfaces (GUIs):** Clean, modern native screens for character sheet creation/editing, visual dice rolling, and visual admin panels.
  - **HUD & Immersion:** On-screen indicators for RP status, remaining lives, and floating speech bubbles above characters in local chat.
  - **Audiovisual Effects:** Custom sound effects and animations integrated with Core events.

---

## ⚙️ Implementation Guidelines

When selecting and implementing any feature from this document:
1. Maintain the **100% Server-Side** premise for the base `In-RP Core` mod (vanilla clients connect without needing client mods).
2. Adhere to invariant and testing guidelines in [ARCHITECTURE.md](ARCHITECTURE.md).
3. All player-facing messages must have matching keys in both `en_us.json` and `pt_br.json`.

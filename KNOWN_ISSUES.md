# 🛡️ Known Issues & Exploit Investigation — In-RP

This document acts as a dedicated testing ledger and triage board for potential bugs, game mechanics exploits, and edge cases in **In-RP**. 

The purpose of this file is to document suspected issues thoroughly with **step-by-step reproduction instructions**, allowing the development team to test with real players and confirm gameplay impact before committing fixes to code.

---

## 📋 Status Key

* `[Pending Testing]` — Suspected behavior or architectural theoretical exploit; needs verification in a live multiplayer session.
* `[Confirmed]` — Reproduced and verified in-game; scheduled for mitigation in an upcoming patch (e.g., v1.0.7 or future release).
* `[False Positive]` — Tested and found harmless or intended by design.
* `[Resolved]` — Fixed, tested, and released in a specific version.

---

## 🔍 Active Investigation Queue

### 1. 🧪 [Pending Testing] Chat & Roll "Sonar Radar": Detection of Spectators / Vanished Staff
* **Affected Areas:** Proximity Local Chat ([ChatEventHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/ChatEventHandler.java)) & Dice Roller ([RollCommand.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/commands/RollCommand.java))
* **Severity:** Medium (Information Disclosure / Anti-Moderation Exploit)
* **Description:**
  - Both local chat and `/roll` notify the player with `(Nobody nearby heard you)` when `recipientCount <= 1`.
  - Currently, any player within the Euclidean radius (`nearby.distanceToSqr(sender) <= radiusSq`) is added to the recipient set, regardless of their game mode (`SPECTATOR`) or invisibility status.
  - **The Exploit:** A player can repeatedly send dummy chat messages or rolls in isolated locations (e.g., deep underground or in secret bases). If the `(Nobody nearby heard you)` feedback suddenly fails to appear, the player knows an administrator is invisibly spectating them nearby.
* **Reproduction Guide (To test with friends):**
  1. Have Friend A move to an isolated location far from any other player (> 60 blocks away).
  2. Friend A types a message in local chat (`T`). Confirm that `(Nobody nearby heard you)` is displayed.
  3. The Host/Admin enters Spectator mode (`/gamemode spectator`) and flies invisibly right next to Friend A.
  4. Friend A types another message in local chat.
  5. **Observation Check:** Does `(Nobody nearby heard you)` stop appearing for Friend A?
  6. Repeat steps with `/roll` to test dice rolling behavior.
* **Proposed Mitigation (For v1.0.7):**
  - **Decouple Delivery from Audience Count:** Allow spectators/vanished moderators to receive the message (so staff can moderate), but exclude them from the `validAudienceCount`:
    ```java
    if (!nearby.isSpectator() && !InRPAttachments.isChatSpy(nearby)) {
        legitimateAudienceCount++;
    }
    ```
  - **Config Toggles:** Provide `chatNoOneHeardFeedback = true/false` and `rollNoOneHeardFeedback = true/false` in `inrp-server.toml` for server administrators who prefer disabling the notification entirely.

---

### 2. 🧪 [Pending Testing] Indirect PvP Damage Bypass (Projectiles, Splash Potions, Explosives)
* **Affected Areas:** [RPGameplayRulesHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/RPGameplayRulesHandler.java) (`onAttackEntity`)
* **Severity:** High (Gameplay Rule Bypass)
* **Description:**
  - Currently, `pvpAllowedInRP = false` only cancels `AttackEntityEvent`.
  - In Minecraft / NeoForge, `AttackEntityEvent` only triggers on direct melee left-click attacks.
  - **The Exploit:** Players in RP mode (or against RP players) might still deal/take damage through non-melee vectors:
    1. Projectiles (Bows, Crossbows, Tridents, Wind Charges).
    2. Splash and Lingering Potions (Harming, Poison, Wither).
    3. Explosives (TNT, End Crystals, Respawn Anchors).
    4. Sweeping edge collateral damage when hitting an adjacent mob or pet.
* **Reproduction Guide (To test with friends):**
  1. Ensure `pvpAllowedInRP = false` in `inrp-server.toml` (or via `/rpadmin config pvp false`).
  2. Player A and Player B both toggle `/rp on`.
  3. Player A tries to punch Player B. Confirm that melee attack is cancelled with the warning message.
  4. Player A shoots Player B with a bow. Check if damage is dealt.
  5. Player A throws a Splash Potion of Harming at Player B. Check if damage is dealt.
  6. Player A attacks a cow standing directly next to Player B with a sweeping sword. Check if Player B receives sweep damage.
* **Proposed Mitigation:**
  - Subscribe to `LivingIncomingDamageEvent` or `LivingDamageEvent`.
  - If attacker/cause entity resolves to a `Player`, verify RP states and cancel event if either participant is in RP mode and PvP is restricted.

---

### 3. 🧪 [Pending Testing] Block Protection Bypasses via Fluid Buckets & Tool Interactions
* **Affected Areas:** [RPGameplayRulesHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/RPGameplayRulesHandler.java) (`onBlockPlace`, `onBlockBreak`)
* **Severity:** Medium (World Protection Bypass)
* **Description:**
  - `blockPlaceAllowedInRP = false` and `blockBreakAllowedInRP = false` intercept `BlockEvent.BreakEvent` and `BlockEvent.EntityPlaceEvent`.
  - **Edge Case to Verify:** Check whether fluid bucket placement (water/lava), ignition with Flint & Steel, or block modifications via right-click tools (stripping wood with axes, tilling soil with hoes, flattening dirt with shovels) bypass these checks.
* **Reproduction Guide:**
  1. Set `blockPlaceAllowedInRP = false` and `blockBreakAllowedInRP = false`.
  2. Enter RP mode (`/rp on`).
  3. Attempt to place a block (e.g., Cobblestone). Confirm cancellation.
  4. Attempt to empty a Water Bucket or Lava Bucket on the ground.
  5. Attempt to use Flint & Steel to start a fire.
  6. Attempt to strip an oak log with an axe or till dirt with a hoe.
* **Proposed Mitigation:**
  - If leaks are confirmed, subscribe to `PlayerInteractEvent.RightClickBlock` and validate item actions against block place/break rules.

---

### 4. 🧪 [Pending Testing] `/roll` Command Flooding / Macro Spam
* **Affected Areas:** [RollCommand.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/commands/RollCommand.java)
* **Severity:** Medium (Chat Spam / Minor DoS)
* **Description:**
  - `/g` (global chat) and `/afk` have built-in cooldowns (`globalChatCooldownSeconds` and 3-second cooldown).
  - `/roll` has no rate limiter. A player using a rapid macro running `/roll 100d10000` could spam the chat and burden server text processing.
* **Reproduction Guide:**
  1. Execute `/roll` 5 times in rapid succession.
  2. Observe that all 5 rolls are broadcast without any throttle or cooldown.
* **Proposed Mitigation:**
  - Add a configurable cooldown (e.g. `rollCooldownSeconds = 2`) to `InRPConfig` and apply rate-limiting in `RollCommand`.

---

### 5. 🧪 [Pending Testing] AFK Wakeup Loop in Water Currents & External Displacement
* **Affected Areas:** [AFKEventHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/AFKEventHandler.java)
* **Severity:** Low (Audio / Notification Annoyance)
* **Description:**
  - `AFKPosition` checks `distSq > 0.0225` (moved > 0.15 blocks).
  - If an idle player is pushed by a water stream, conveyor, or mob, they wake up (`wakeUp(player)`), playing sound and displaying actionbar return text.
  - Since the player is actually away from the keyboard, after `afkTimeoutSeconds` elapses, they enter AFK again, only to be immediately woken up by water displacement, creating an infinite cycle.
* **Reproduction Guide:**
  1. Step into a flowing water stream.
  2. Run `/afk` to manually enter AFK.
  3. Observe if the water movement immediately wakes the player up.
* **Proposed Mitigation:**
  - Filter out environmental movement (check if player is in fluid, riding vehicle, or verify packet-driven user input).

---

### 6. 🧪 [Pending Testing] Dimension Boundary & Proximity Calculation
* **Affected Areas:** [ChatEventHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/ChatEventHandler.java) & [RollCommand.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/commands/RollCommand.java)
* **Severity:** Low (Edge Case)
* **Description:**
  - When calculating nearby players, `sender.serverLevel().players()` is used, which correctly scopes players to the current dimension (Overworld, Nether, End).
  - **Edge Case to Verify:** Check if players standing directly across active Nether portals or near world boundaries produce any coordinate wrapping or silent recipient drops.
* **Reproduction Guide:**
  1. Have Player A stand inside an active Nether portal frame in the Overworld.
  2. Have Player B stand on the other side in the Nether at matching translated coordinates.
  3. Verify that local chat remains strictly dimension-bound and does not leak cross-dimension coordinates.

---

### 7. 🧪 [Pending Testing] AFK Involuntary Evasion & Mob Targeting
* **Affected Areas:** [AFKEventHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/AFKEventHandler.java) & [RPGameplayRulesHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/RPGameplayRulesHandler.java)
* **Severity:** Low (Balance Verification)
* **Description:**
  - When a player enters AFK, `autoDisableRPOnAFK` toggles them out of RP mode (`/rp off`).
  - If `pvpAllowedInRP = false`, entering AFK (Off-RP) actually *enables* PvP vulnerability (preventing AFK from being abused as a combat shield).
  - **Edge Case to Verify:** Ensure that automated AFK timer entry cannot be weaponized during combat by letting a player remain motionless to force RP exit.
* **Reproduction Guide:**
  1. Set `pvpAllowedInRP = false` in `inrp-server.toml`.
  2. Player A enters RP (`/rp on`) and waits until AFK is triggered.
  3. Verify that Player B can attack Player A once AFK status triggers, proving no invulnerability exploit exists.

---

## 📝 Testing Notes Template

When conducting testing sessions with friends, use the format below to log findings:

```markdown
### Test Log: [Date - YYYY-MM-DD]
- **Tested Issue:** [Issue Title / Number]
- **Participants:** [Host, Players]
- **Result:** [Confirmed / Not Reproduced / Needs Revision]
- **Observations:** [Describe exact in-game behavior observed]
- **Next Steps:** [Schedule fix for 1.0.7 or discard]
```

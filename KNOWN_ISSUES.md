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

### 2. 🛡️ [Resolved] Indirect PvP Damage Bypass (Projectiles, Splash Potions, Explosives)
* **Affected Areas:** [RPGameplayRulesHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/RPGameplayRulesHandler.java) (`onAttackEntity`, `onIncomingDamage`)
* **Severity:** High (Gameplay Rule Bypass)
* **Status:** Resolved in `v1.0.7`
* **Description:**
  - Originally, `pvpAllowedInRP = false` only cancelled `AttackEntityEvent` (melee left-click attacks).
  - Theoretical exploit allowed non-melee vectors (projectiles, TNT, harming potions, sweeping edge) to bypass the protection.
* **Resolution:**
  - Implemented `LivingIncomingDamageEvent` listener in `RPGameplayRulesHandler.onIncomingDamage`. If `pvpAllowedInRP = false` and the responsible entity resolves to a player, damage is cancelled and the attacker is notified.
* **Verification & Testing:**
  - Verified in live multiplayer: direct attacks, bows, crossbows, tridents, and splash potions of harming are completely blocked.
* **Known Caveat / Edge Case:**
  - Status effect damage over time from **Poison** (e.g. Splash Potion of Poison) can still apply damage ticks because Minecraft attributes periodic effect ticks to the status effect rather than directly to the player entity. However, Poison is strictly non-lethal in vanilla (stops at 0.5 hearts / 1 HP) and cannot eliminate or kill a player.
  - In heavily modded modpacks, certain custom damage types or magic mods might similarly bypass standard player attribution, but all core vanilla combat vectors are fully protected.

---

### 3. 🛡️ [Resolved] Fluid Bucket Placement Bypass (Water & Lava)
* **Affected Areas:** [RPGameplayRulesHandler.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/events/RPGameplayRulesHandler.java) (`onBlockPlace`, `onRightClickBlock`, `onRightClickItem`)
* **Severity:** Medium (World Protection Bypass)
* **Status:** Resolved in `v1.0.7`
* **Description:**
  - Originally, `blockPlaceAllowedInRP = false` only intercepted `BlockEvent.EntityPlaceEvent`. Emptying water and lava buckets into the world bypassed protection because bucket emptying was processed as an item interaction.
* **Resolution:**
  - Added `onRightClickBlock` listener: sets `event.setUseItem(TriState.FALSE)` when a restricted player holds a fluid bucket, preventing fluid placement while allowing block interactions (opening chests, barrels, doors) to proceed normally.
  - Added `onRightClickItem` listener: cancels fluid placement when aiming at fluids or air (`event.setCanceled(true)` with `InteractionResult.FAIL`) and displays the action bar notification `inrp.rule.block_place_disabled`.
  - Added `isFluidPlacementItem(stack)` supporting both vanilla `BucketItem` instances (water, lava, mob buckets) and modded fluid containers via NeoForge `FluidUtil`.

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

## 📜 Session Test Logs

### Test Log: 2026-09-10
- **Tested Issue:** #2 Indirect PvP Damage Bypass (Projectiles, Splash Potions, Explosives)
- **Participants:** Host & Players
- **Result:** Confirmed & Resolved
- **Observations:** All direct attacks, bows, tridents, sweep attacks, and splash potions of harming are blocked with action bar notification. The only effect capable of inflicting damage is Splash Potion of Poison, which ticks via vanilla status effect magic damage (null direct entity). Because Poison cannot drop health below 0.5 hearts (1 HP), it is non-lethal and cannot eliminate or kill players.
- **Next Steps:** Closed as `[Resolved]` with the poison caveat noted.

### Test Log: 2026-09-10
- **Tested Issue:** #3 Fluid Bucket Placement Bypass (Water & Lava)
- **Participants:** Host & Players
- **Result:** Confirmed & Resolved in v1.0.7
- **Observations:** Shovels, hoes, axes, bone meal, and flint & steel were verified and are properly blocked. Consumable items (potions) and opening containers (chests) work normally. Pouring water or lava from buckets is now intercepted via `RightClickBlock` and `RightClickItem`, successfully preventing fluid placement while keeping container interactions open.
- **Next Steps:** Closed as `[Resolved]`.

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

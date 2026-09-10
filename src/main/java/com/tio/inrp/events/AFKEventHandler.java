package com.tio.inrp.events;

import com.tio.inrp.commands.AFKCommand;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inactivity detection.
 *
 * <p>Entering AFK is driven by a coarse sweep over the player list (once every {@value #SWEEP_INTERVAL_TICKS} ticks)
 * that compares vanilla's own {@code lastActionTime}, so idle players cost nothing until the sweep runs. Leaving AFK
 * is driven per tick, but only for players already flagged as AFK, which keeps the per-tick cost at one attachment
 * lookup for everybody else.
 *
 * <p>All timings use {@link Util#getMillis()} &mdash; the same monotonic clock vanilla stamps {@code lastActionTime}
 * with, so the comparison stays correct even if the machine's wall clock is adjusted.
 */
public final class AFKEventHandler {

    /** How often the inactivity sweep runs. 100 ticks is 5 seconds at the nominal tick rate. */
    public static final int SWEEP_INTERVAL_TICKS = 100;

    /** Grace window after entering AFK, so the packets of the {@code /afk} command itself do not wake the player. */
    private static final long GRACE_PERIOD_MS = 1000L;

    /** Squared movement threshold for a wake-up: roughly 0.15 blocks, above sub-tick jitter. */
    private static final double WAKE_MOVE_THRESHOLD_SQ = 0.0225D;

    /** Head rotation, in degrees, that counts as a wake-up. */
    private static final float WAKE_ROTATION_THRESHOLD = 2.0F;

    private static final Map<UUID, AFKPosition> AFK_POSITIONS = new ConcurrentHashMap<>();

    private static int tickCounter;

    /** Pose recorded when a player entered AFK, used to detect that they came back. */
    public record AFKPosition(double x, double y, double z, float yRot, float xRot, long enteredAt) {
    }

    private AFKEventHandler() {
    }

    /** Records the pose a player entered AFK with. */
    public static void trackAFK(ServerPlayer player) {
        if (player == null) {
            return;
        }
        AFK_POSITIONS.put(player.getUUID(), new AFKPosition(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                Util.getMillis()
        ));
    }

    /** Forgets a player's AFK pose. */
    public static void untrackAFK(UUID uuid) {
        if (uuid != null) {
            AFK_POSITIONS.remove(uuid);
        }
    }

    /**
     * Marks a player as AFK, applying the configured side effects. Does nothing if they already are.
     *
     * <p>Deliberately silent: the caller decides whether the transition is announced (the {@code /afk} command
     * broadcasts it, the inactivity sweep does not).
     */
    public static void enterAFK(ServerPlayer player) {
        if (player == null || InRPAttachments.isAFK(player)) {
            return;
        }
        InRPAttachments.setAFK(player, true);
        if (InRPConfig.AUTO_DISABLE_RP_ON_AFK.get()) {
            InRPAttachments.setInRP(player, false);
        }
        trackAFK(player);
        ScoreboardHandler.updatePlayerScoreboard(player);
    }

    /**
     * Clears a player's AFK state and tells them so. Safe to call unconditionally &mdash; it returns immediately if
     * the player is not AFK, so overlapping wake-up sources cannot produce duplicate feedback.
     */
    public static void wakeUp(ServerPlayer player) {
        if (player == null || !InRPAttachments.isAFK(player)) {
            return;
        }
        untrackAFK(player.getUUID());
        InRPAttachments.setAFK(player, false);

        // Vanilla does not stamp lastActionTime for every wake-up trigger; without this the next sweep would
        // immediately mark the player AFK again.
        player.resetLastActionTime();
        ScoreboardHandler.updatePlayerScoreboard(player);

        player.displayClientMessage(
                LocalizationHelper.getPrefixedMessage("inrp.afk.actionbar.return").withStyle(ChatFormatting.GREEN),
                true
        );
        player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    /** Drops all tracked state. Called on server shutdown so nothing leaks into the next world load. */
    public static void reset() {
        AFK_POSITIONS.clear();
        tickCounter = 0;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < SWEEP_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        if (!InRPConfig.AFK_ENABLED.get()) {
            // The system was switched off while players were flagged: release them instead of leaving them stuck.
            if (!AFK_POSITIONS.isEmpty()) {
                clearAllAFK(event.getServer());
            }
            return;
        }

        long now = Util.getMillis();
        long afkTimeoutMillis = InRPConfig.afkTimeoutMillis();
        long kickMillis = InRPConfig.afkKickMillis();

        // Kicking removes players from the live list, so iterate a snapshot when the idle kick is enabled.
        List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
        if (kickMillis > 0) {
            players = List.copyOf(players);
        }

        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            long idleMillis = now - player.getLastActionTime();

            if (!InRPAttachments.isAFK(player)) {
                if (idleMillis >= afkTimeoutMillis) {
                    enterAFK(player);
                }
                continue;
            }

            if (kickMillis > 0 && idleMillis >= kickMillis) {
                player.connection.disconnect(LocalizationHelper.getMessage("inrp.afk.kick_message")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !InRPAttachments.isAFK(player)) {
            return;
        }

        AFKPosition pose = AFK_POSITIONS.get(player.getUUID());
        if (pose == null) {
            // Flagged as AFK without a recorded pose (e.g. state restored from disk): start tracking from here.
            trackAFK(player);
            return;
        }

        if (Util.getMillis() - pose.enteredAt() < GRACE_PERIOD_MS) {
            return;
        }

        double dx = player.getX() - pose.x();
        double dy = player.getY() - pose.y();
        double dz = player.getZ() - pose.z();
        boolean moved = (dx * dx + dy * dy + dz * dz) > WAKE_MOVE_THRESHOLD_SQ;
        boolean looked = Math.abs(player.getYRot() - pose.yRot()) > WAKE_ROTATION_THRESHOLD
                || Math.abs(player.getXRot() - pose.xRot()) > WAKE_ROTATION_THRESHOLD;

        if (moved || looked) {
            wakeUp(player);
        }
    }

    /** Receives cancelled events too, so chatting still wakes a player up when local chat cancels the broadcast. */
    @SubscribeEvent(receiveCanceled = true)
    public static void onServerChat(ServerChatEvent event) {
        wakeUp(event.getPlayer());
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            wakeUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            wakeUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean wasFlagged = InRPAttachments.isAFK(player);
        clearAFKState(player);

        // ScoreboardHandler is registered first, so its login handler already ran and saw the stale flag from the
        // player's save data (possible after an unclean shutdown), putting them on the AFK team. Refresh the marker
        // now that the flag is cleared, otherwise they stay visibly AFK until some unrelated event refreshes it.
        if (wasFlagged) {
            ScoreboardHandler.updatePlayerScoreboard(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAFKState(player);
        }
    }

    /** Resets AFK bookkeeping around a connection change, without the player-facing wake-up feedback. */
    private static void clearAFKState(ServerPlayer player) {
        InRPAttachments.setAFK(player, false);
        untrackAFK(player.getUUID());
        AFKCommand.clearCooldown(player.getUUID());
    }

    private static void clearAllAFK(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (InRPAttachments.isAFK(player)) {
                InRPAttachments.setAFK(player, false);
                ScoreboardHandler.updatePlayerScoreboard(player);
            }
        }
        AFK_POSITIONS.clear();
    }
}

package com.tio.inrp.events;

import com.tio.inrp.commands.AFKCommand;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKEventHandler {

    public record AFKPosition(double x, double y, double z, float yRot, float xRot, long enteredAt) {}

    private static final Map<UUID, AFKPosition> AFK_POSITIONS = new ConcurrentHashMap<>();
    private static final long GRACE_PERIOD_MS = 1000L; // 1 second grace period to prevent self-wakeup from command packet
    private static int tickCounter = 0;

    public static void trackAFK(ServerPlayer player) {
        if (player == null) return;
        AFK_POSITIONS.put(player.getUUID(), new AFKPosition(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                System.currentTimeMillis()
        ));
    }

    public static void untrackAFK(UUID uuid) {
        if (uuid != null) {
            AFK_POSITIONS.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < 100) { // Check every 5 seconds (100 ticks)
            return;
        }
        tickCounter = 0;

        if (!InRPConfig.AFK_ENABLED.get()) {
            return;
        }

        long now = Util.getMillis();
        long afkTimeoutMillis = InRPConfig.AFK_TIMEOUT_SECONDS.get() * 1000L;
        int kickSeconds = InRPConfig.AFK_KICK_SECONDS.get();
        long kickTimeoutMillis = kickSeconds > 0 ? (kickSeconds * 1000L) : -1L;
        boolean autoDisableRP = InRPConfig.AUTO_DISABLE_RP_ON_AFK.get();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player == null) continue;

            long idleMillis = now - player.getLastActionTime();

            // 1. Kick check
            if (kickTimeoutMillis > 0 && idleMillis >= kickTimeoutMillis) {
                Component kickReason = LocalizationHelper.getMessage("inrp.afk.kick_message")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                player.connection.disconnect(kickReason);
                continue;
            }

            // 2. AFK timeout check
            if (idleMillis >= afkTimeoutMillis) {
                if (!InRPAttachments.isAFK(player)) {
                    InRPAttachments.setAFK(player, true);
                    if (autoDisableRP) {
                        InRPAttachments.setInRP(player, false);
                    }
                    trackAFK(player);
                    ScoreboardHandler.updatePlayerScoreboard(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Only inspect players currently in AFK state
            if (!InRPAttachments.isAFK(player)) {
                return;
            }

            UUID uuid = player.getUUID();
            AFKPosition pos = AFK_POSITIONS.get(uuid);
            if (pos == null) {
                // If not tracked yet, track now
                trackAFK(player);
                return;
            }

            // Honor grace period immediately after entering AFK
            if (System.currentTimeMillis() - pos.enteredAt() < GRACE_PERIOD_MS) {
                return;
            }

            // Detect real player movement or head rotation
            double dx = player.getX() - pos.x();
            double dy = player.getY() - pos.y();
            double dz = player.getZ() - pos.z();
            double distSq = dx * dx + dy * dy + dz * dz;

            float dYaw = Math.abs(player.getYRot() - pos.yRot());
            float dPitch = Math.abs(player.getXRot() - pos.xRot());

            // Moved more than 0.15 blocks or rotated head more than 2 degrees
            if (distSq > 0.0225 || dYaw > 2.0f || dPitch > 2.0f) {
                wakeUp(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null && InRPAttachments.isAFK(player)) {
            wakeUp(player);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && InRPAttachments.isAFK(player)) {
            wakeUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && InRPAttachments.isAFK(player)) {
            wakeUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InRPAttachments.setAFK(player, false);
            untrackAFK(player.getUUID());
            AFKCommand.clearCooldown(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InRPAttachments.setAFK(player, false);
            untrackAFK(player.getUUID());
            AFKCommand.clearCooldown(player.getUUID());
        }
    }

    public static void wakeUp(ServerPlayer player) {
        if (player == null) return;
        untrackAFK(player.getUUID());
        InRPAttachments.setAFK(player, false);
        ScoreboardHandler.updatePlayerScoreboard(player);

        player.displayClientMessage(
                LocalizationHelper.getPrefixedMessage("inrp.afk.actionbar.return").withStyle(ChatFormatting.GREEN),
                true
        );
        player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
    }
}

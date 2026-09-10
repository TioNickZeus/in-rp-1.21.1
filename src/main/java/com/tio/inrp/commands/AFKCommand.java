package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.AFKEventHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /afk} &mdash; voluntary AFK toggle.
 *
 * <p>Entering AFK is announced server-wide, unlike the automatic inactivity sweep, which is silent. A short
 * per-player cooldown keeps the announcement from being used as a chat spam vector.
 */
public final class AFKCommand {

    private static final long COOLDOWN_MS = 3000L;
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    private AFKCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
                .executes(context -> toggleAFK(context.getSource()))
        );
    }

    /** Forgets a player's cooldown, e.g. when they disconnect. */
    public static void clearCooldown(UUID uuid) {
        if (uuid != null) {
            COOLDOWNS.remove(uuid);
        }
    }

    /** Drops every cooldown. Called on server shutdown so nothing leaks into the next world load. */
    public static void reset() {
        COOLDOWNS.clear();
    }

    private static int toggleAFK(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(LocalizationHelper.getMessage("inrp.error.players_only"));
            return 0;
        }

        if (!InRPConfig.AFK_ENABLED.get()) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.afk.disabled").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!tryUseCooldown(player.getUUID())) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.afk.cooldown").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (InRPAttachments.isAFK(player)) {
            AFKEventHandler.wakeUp(player);
            return 1;
        }

        AFKEventHandler.enterAFK(player);

        Component announcement = LocalizationHelper
                .getPrefixedMessage("inrp.afk.enter.broadcast", player.getScoreboardName())
                .withStyle(ChatFormatting.GRAY);
        player.server.getPlayerList().broadcastSystemMessage(announcement, false);

        player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7F, 0.9F);
        return 1;
    }

    /** @return {@code true} when the command may run, recording the use; {@code false} while still cooling down. */
    private static boolean tryUseCooldown(UUID uuid) {
        long now = Util.getMillis();
        Long lastUsed = COOLDOWNS.get(uuid);
        if (lastUsed != null && now - lastUsed < COOLDOWN_MS) {
            return false;
        }
        COOLDOWNS.put(uuid, now);
        return true;
    }
}

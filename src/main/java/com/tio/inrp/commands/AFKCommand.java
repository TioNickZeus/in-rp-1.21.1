package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKCommand {

    private static final long COOLDOWN_MS = 3000L;
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
                .executes(context -> toggleAFK(context.getSource()))
        );
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

        long now = System.currentTimeMillis();
        UUID uuid = player.getUUID();
        Long lastUsed = COOLDOWNS.get(uuid);
        if (lastUsed != null && (now - lastUsed) < COOLDOWN_MS) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.afk.cooldown").withStyle(ChatFormatting.RED));
            return 0;
        }
        COOLDOWNS.put(uuid, now);

        boolean currentAFK = InRPAttachments.isAFK(player);
        if (!currentAFK) {
            // Enter AFK
            InRPAttachments.setAFK(player, true);
            if (InRPConfig.AUTO_DISABLE_RP_ON_AFK.get()) {
                InRPAttachments.setInRP(player, false);
            }
            com.tio.inrp.events.AFKEventHandler.trackAFK(player);
            ScoreboardHandler.updatePlayerScoreboard(player);

            // Global chat announcement for voluntary /afk
            Component broadcastMsg = LocalizationHelper.getPrefixedMessage("inrp.afk.enter.broadcast", player.getScoreboardName())
                    .withStyle(ChatFormatting.GRAY);
            player.server.getPlayerList().broadcastSystemMessage(broadcastMsg, false);

            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 0.9f);
        } else {
            // Exit AFK
            com.tio.inrp.events.AFKEventHandler.wakeUp(player);
        }

        return 1;
    }

    public static void clearCooldown(UUID uuid) {
        if (uuid != null) {
            COOLDOWNS.remove(uuid);
        }
    }
}

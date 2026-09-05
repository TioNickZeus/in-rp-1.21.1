package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import com.tio.inrp.data.InRPLivesManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class LivesEventHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Feature #5: Skip death counting if configured to only count RP deaths
            if (InRPConfig.COUNT_DEATHS_ONLY_IN_RP.get() && !InRPAttachments.isInRP(player)) {
                return;
            }

            InRPAttachments.incrementDeathCount(player);

            // If player doesn't have custom max lives, apply default if configured
            if (!InRPAttachments.hasLivesLimit(player)) {
                int defaultMax = InRPConfig.DEFAULT_MAX_LIVES.get();
                if (defaultMax > 0) {
                    InRPAttachments.setMaxLives(player, defaultMax);
                }
            }

            if (InRPAttachments.hasLivesLimit(player)) {
                int deaths = InRPAttachments.getDeathCount(player);
                int maxLives = InRPAttachments.getMaxLives(player);

                if (deaths >= maxLives) {
                    InRPAttachments.setDead(player, true);
                    InRPLivesManager.markDead(player.getUUID());

                    Component broadcastMsg = LocalizationHelper.getPrefixedMessage(
                            "inrp.lives.eliminated.broadcast",
                            player.getDisplayName()
                    ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);

                    player.server.getPlayerList().broadcastSystemMessage(broadcastMsg, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleDeadPlayerState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if player was revived while offline
            if (!InRPLivesManager.isMarkedDead(player.getUUID()) && InRPAttachments.isDead(player)) {
                InRPAttachments.setDead(player, false);
                InRPAttachments.setDeathCount(player, 0);
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    player.setGameMode(GameType.SURVIVAL);
                }
                ScoreboardHandler.updatePlayerScoreboard(player);
                return;
            }

            handleDeadPlayerState(player);
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (InRPAttachments.isDead(player) || InRPLivesManager.isMarkedDead(player.getUUID())) {
                Component deadTag = LocalizationHelper.getMessage("inrp.lives.tab.dead")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);

                event.setDisplayName(Component.empty()
                        .append(deadTag)
                        .append(Component.literal(" "))
                        .append(player.getName()));
            } else {
                event.setDisplayName(null);
            }
        }
    }

    private static void handleDeadPlayerState(ServerPlayer player) {
        if (InRPAttachments.isDead(player) || InRPLivesManager.isMarkedDead(player.getUUID())) {
            InRPAttachments.setDead(player, true);
            String action = InRPConfig.LIVES_ACTION.get();

            if ("kick".equalsIgnoreCase(action)) {
                Component kickReason = LocalizationHelper.getMessage("inrp.lives.kick_message")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                player.connection.disconnect(kickReason);
            } else {
                player.setGameMode(GameType.SPECTATOR);
                player.displayClientMessage(
                        LocalizationHelper.getPrefixedMessage("inrp.lives.spectator_message")
                                .withStyle(ChatFormatting.DARK_RED),
                        false
                );
                ScoreboardHandler.updatePlayerScoreboard(player);
                ScoreboardHandler.refreshPlayerTabList(player);
            }
        } else {
            ScoreboardHandler.refreshPlayerTabList(player);
        }
    }
}

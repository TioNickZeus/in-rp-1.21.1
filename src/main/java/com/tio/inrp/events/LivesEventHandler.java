package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.data.InRPLivesManager;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The lives system: death counting, elimination and revival.
 *
 * <p>Elimination is recorded in two places on purpose. The {@code IS_DEAD} attachment travels with the player's save
 * data, while {@link InRPLivesManager} keeps a world-level list that can be changed while the player is offline. On
 * login the two are reconciled: file says alive but attachment says dead means an admin revived them in the
 * meantime.
 *
 * <p>The elimination action itself is applied on respawn or login rather than at the moment of death, so the player
 * still sees the normal death screen and no game mode change is attempted on a dying entity.
 */
public final class LivesEventHandler {

    private LivesEventHandler() {
    }

    /**
     * Counts the death after every other listener has had its say.
     *
     * <p>{@link EventPriority#LOWEST} matters: mods that keep a player alive (graves, second-chance and
     * keep-alive mods) do so by cancelling {@code LivingDeathEvent}, and a cancelled event is not delivered here,
     * so a death that never actually happened is never counted. Running last is what guarantees those
     * cancellations are already in effect. (A vanilla totem of undying needs no handling: it is applied inside
     * {@code hurt()} before {@code die()}, so the event is never fired at all.)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (InRPConfig.COUNT_DEATHS_ONLY_IN_RP.get() && !InRPAttachments.isInRP(player)) {
            return;
        }

        InRPAttachments.incrementDeathCount(player);

        // A player with no personal limit inherits the server default the first time they die.
        if (!InRPAttachments.hasLivesLimit(player)) {
            int defaultMaxLives = InRPConfig.DEFAULT_MAX_LIVES.get();
            if (defaultMaxLives > 0) {
                InRPAttachments.setMaxLives(player, defaultMaxLives);
            }
        }

        if (InRPAttachments.hasRunOutOfLives(player) && !InRPAttachments.isDead(player)) {
            markEliminated(player);
            player.server.getPlayerList().broadcastSystemMessage(
                    LocalizationHelper.getPrefixedMessage("inrp.lives.eliminated.broadcast", player.getDisplayName())
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reconcileEliminationState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Revived while offline: the world-level list was cleared but the save data still says eliminated.
        if (InRPAttachments.isDead(player) && !InRPLivesManager.isMarkedDead(player.getUUID())) {
            revive(player);
            return;
        }

        reconcileEliminationState(player);
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (isEliminated(player)) {
            event.setDisplayName(taggedName(player, "inrp.lives.tab.dead",
                    ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        } else if (InRPAttachments.isAFK(player)) {
            event.setDisplayName(taggedName(player, "inrp.afk.tab.tag",
                    ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        // Otherwise the display name is left untouched: the event already defaults to "no override", and writing
        // null here would clobber a tab list name set by another mod.
    }

    /** @return whether the player is recorded as eliminated by either store. */
    public static boolean isEliminated(ServerPlayer player) {
        return InRPAttachments.isDead(player) || InRPLivesManager.isMarkedDead(player.getUUID());
    }

    /** Records a player as eliminated in both stores without applying the elimination action. */
    public static void markEliminated(ServerPlayer player) {
        if (player == null) {
            return;
        }
        InRPAttachments.setDead(player, true);
        InRPLivesManager.markDead(player.getUUID());
        ScoreboardHandler.refreshPlayerTabList(player);
    }

    /** Applies the configured elimination action to a player who is already marked as eliminated. */
    public static void enforceEliminationState(ServerPlayer player) {
        if (player == null || !InRPAttachments.isDead(player)) {
            return;
        }

        if (InRPConfig.eliminatesByKick()) {
            if (player.server.isSingleplayerOwner(player.getGameProfile())) {
                if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                    player.setGameMode(GameType.SPECTATOR);
                }
                player.displayClientMessage(
                        LocalizationHelper.getPrefixedMessage("inrp.lives.host_spectator_message")
                                .withStyle(ChatFormatting.DARK_RED),
                        false);
                ScoreboardHandler.updatePlayerScoreboard(player);
                return;
            }

            player.connection.disconnect(LocalizationHelper.getMessage("inrp.lives.kick_message")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            return;
        }

        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
        }
        player.displayClientMessage(
                LocalizationHelper.getPrefixedMessage("inrp.lives.spectator_message").withStyle(ChatFormatting.DARK_RED),
                false);
        ScoreboardHandler.updatePlayerScoreboard(player);
    }

    /** Full revival: clears the elimination in both stores, resets the death count and notifies the player. */
    public static void revive(ServerPlayer player) {
        if (player == null) {
            return;
        }
        InRPAttachments.setDeathCount(player, 0);
        clearElimination(player, true);
    }

    /**
     * Clears the elimination flags and restores survival mode while leaving the death count untouched, for admin
     * edits that lower a player's death count below their limit.
     *
     * @param notify whether the player is told they were revived
     */
    public static void clearElimination(ServerPlayer player, boolean notify) {
        if (player == null) {
            return;
        }
        InRPAttachments.setDead(player, false);
        InRPLivesManager.unmarkDead(player.getUUID());

        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(GameType.SURVIVAL);
        }
        ScoreboardHandler.updatePlayerScoreboard(player);

        if (notify) {
            player.sendSystemMessage(LocalizationHelper.getPrefixedMessage("inrp.lives.revived_notification")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
    }

    private static void reconcileEliminationState(ServerPlayer player) {
        if (isEliminated(player)) {
            InRPAttachments.setDead(player, true);
            enforceEliminationState(player);
        } else {
            ScoreboardHandler.refreshPlayerTabList(player);
        }
    }

    private static Component taggedName(ServerPlayer player, String tagKey, ChatFormatting... tagStyles) {
        return Component.empty()
                .append(LocalizationHelper.getMessage(tagKey).withStyle(tagStyles))
                .append(Component.literal(" "))
                .append(player.getName());
    }
}

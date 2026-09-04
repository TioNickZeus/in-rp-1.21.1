package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class RPGameplayRulesHandler {

    private static boolean shouldBypass(Player player) {
        return InRPConfig.OP_BYPASS_RESTRICTIONS.get() && player.hasPermissions(2);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!InRPConfig.PVP_ALLOWED_IN_RP.get()) {
            Player attacker = event.getEntity();
            if (attacker instanceof ServerPlayer serverAttacker) {
                if (shouldBypass(serverAttacker)) {
                    return;
                }

                if (event.getTarget() instanceof Player targetPlayer) {
                    boolean attackerInRP = InRPAttachments.isInRP(serverAttacker);
                    boolean targetInRP = InRPAttachments.isInRP(targetPlayer);

                    if (attackerInRP || targetInRP) {
                        event.setCanceled(true);
                        serverAttacker.displayClientMessage(
                                LocalizationHelper.getPrefixedMessage("inrp.rule.pvp_disabled").withStyle(ChatFormatting.RED),
                                true
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP.get()) {
            Player player = event.getPlayer();
            if (player instanceof ServerPlayer serverPlayer && InRPAttachments.isInRP(serverPlayer)) {
                if (shouldBypass(serverPlayer)) {
                    return;
                }

                event.setCanceled(true);
                serverPlayer.displayClientMessage(
                        LocalizationHelper.getPrefixedMessage("inrp.rule.block_break_disabled").withStyle(ChatFormatting.RED),
                        true
                );
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP.get()) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer && InRPAttachments.isInRP(serverPlayer)) {
                if (shouldBypass(serverPlayer)) {
                    return;
                }

                event.setCanceled(true);
                serverPlayer.displayClientMessage(
                        LocalizationHelper.getPrefixedMessage("inrp.rule.block_place_disabled").withStyle(ChatFormatting.RED),
                        true
                );
            }
        }
    }
}

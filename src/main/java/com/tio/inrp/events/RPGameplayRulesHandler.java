package com.tio.inrp.events;

import com.tio.inrp.InRP;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Enforces the roleplay rules configured under {@code [rules]}.
 *
 * <p>Each listener starts with the config check so the rules cost nothing while they are switched off, which is the
 * default. Operators can be granted a blanket exemption through {@code opBypassRestrictions}.
 */
public final class RPGameplayRulesHandler {

    private RPGameplayRulesHandler() {
    }

    /**
     * Cancels melee attacks early, before knockback and the swing animation, so the attacker gets immediate
     * feedback. Damage that never goes through a melee swing is caught by
     * {@link #onIncomingDamage(LivingIncomingDamageEvent)}.
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (InRPConfig.PVP_ALLOWED_IN_RP.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer attacker) || !(event.getTarget() instanceof Player target)) {
            return;
        }
        if (isProtectedPvP(attacker, target)) {
            event.setCanceled(true);
            notifyBlocked(attacker, "inrp.rule.pvp_disabled");
        }
    }

    /**
     * Blocks indirect player-versus-player damage &mdash; arrows, tridents, thrown potions, TNT and anything else
     * that names a player as its responsible entity. Without this, {@code pvpAllowedInRP = false} could be defeated
     * with a bow.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (InRPConfig.PVP_ALLOWED_IN_RP.get()) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        Entity responsible = event.getSource().getEntity();
        if (!(responsible instanceof Player attacker) || attacker == target) {
            return;
        }

        if (isProtectedPvP(attacker, target)) {
            event.setCanceled(true);
            if (attacker instanceof ServerPlayer serverAttacker) {
                notifyBlocked(serverAttacker, "inrp.rule.pvp_disabled");
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP.get()) {
            return;
        }
        if (event.getPlayer() instanceof ServerPlayer player && isRestricted(player)) {
            event.setCanceled(true);
            notifyBlocked(player, "inrp.rule.block_break_disabled");
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && isRestricted(player)) {
            event.setCanceled(true);
            notifyBlocked(player, "inrp.rule.block_place_disabled");
        }
    }

    /** @return whether the rule applies: the player is in RP mode and is not an exempt operator. */
    private static boolean isRestricted(Player player) {
        return InRPAttachments.isInRP(player) && !hasBypass(player);
    }

    /** @return whether the fight must be blocked because either side is roleplaying. */
    private static boolean isProtectedPvP(Player attacker, Player target) {
        if (hasBypass(attacker)) {
            return false;
        }
        return InRPAttachments.isInRP(attacker) || InRPAttachments.isInRP(target);
    }

    private static boolean hasBypass(Player player) {
        return InRPConfig.OP_BYPASS_RESTRICTIONS.get() && player.hasPermissions(InRP.STAFF_PERMISSION_LEVEL);
    }

    private static void notifyBlocked(ServerPlayer player, String messageKey) {
        player.displayClientMessage(
                LocalizationHelper.getPrefixedMessage(messageKey).withStyle(ChatFormatting.RED),
                true);
    }
}

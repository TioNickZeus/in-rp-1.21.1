package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.InRP;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /chatspy} &mdash; staff shortcut for the toggle also exposed as {@code /rpadmin spy}.
 *
 * <p>The toggle is stored in the player's save data, so it survives reconnects. Delivery re-checks the operator
 * level, meaning a player who loses their permissions stops receiving copies without the toggle being reset.
 */
public final class ChatSpyCommand {

    private ChatSpyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chatspy")
                .requires(source -> source.hasPermission(InRP.STAFF_PERMISSION_LEVEL))
                .executes(context -> toggleSpy(context.getSource()))
        );
    }

    /** Flips the executing player's chat spy state. Shared with {@code /rpadmin spy}. */
    public static int toggleSpy(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(LocalizationHelper.getMessage("inrp.error.players_only"));
            return 0;
        }

        boolean enabled = !InRPAttachments.isChatSpy(player);
        InRPAttachments.setChatSpy(player, enabled);

        String messageKey = enabled ? "inrp.chat.spy.enabled" : "inrp.chat.spy.disabled";
        ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(messageKey).withStyle(color), false);
        return 1;
    }
}

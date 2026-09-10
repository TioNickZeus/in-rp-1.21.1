package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class ChatSpyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chatspy")
                .requires(source -> source.hasPermission(2))
                .executes(context -> toggleSpy(context.getSource()))
        );
    }

    public static int toggleSpy(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean current = InRPAttachments.isChatSpy(player);
            boolean next = !current;
            InRPAttachments.setChatSpy(player, next);

            if (next) {
                source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage("inrp.chat.spy.enabled")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage("inrp.chat.spy.disabled")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
            return 1;
        }

        source.sendFailure(LocalizationHelper.getMessage("inrp.error.players_only"));
        return 0;
    }
}

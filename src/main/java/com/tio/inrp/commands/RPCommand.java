package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class RPCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rp")
                .executes(context -> checkStatus(context.getSource()))
                .then(Commands.literal("on")
                        .executes(context -> setStatus(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setStatus(context.getSource(), false)))
                .then(Commands.literal("toggle")
                        .executes(context -> toggleStatus(context.getSource())))
        );
    }

    private static int checkStatus(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean inRP = InRPAttachments.isInRP(player);
            String key = inRP ? "inrp.status.current_on" : "inrp.status.current_off";
            ChatFormatting color = inRP ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
            source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(key).withStyle(color), false);
            return 1;
        }
        source.sendFailure(LocalizationHelper.getMessage("Only players can execute this command."));
        return 0;
    }

    private static int toggleStatus(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean current = InRPAttachments.isInRP(player);
            return setStatus(source, !current);
        }
        return 0;
    }

    public static int setStatus(CommandSourceStack source, boolean enable) {
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean current = InRPAttachments.isInRP(player);
            if (current == enable) {
                String key = enable ? "inrp.status.already_on" : "inrp.status.already_off";
                source.sendFailure(LocalizationHelper.getPrefixedMessage(key).withStyle(ChatFormatting.RED));
                return 0;
            }

            InRPAttachments.setInRP(player, enable);
            ScoreboardHandler.updatePlayerScoreboard(player);

            String key = enable ? "inrp.status.turned_on" : "inrp.status.turned_off";
            ChatFormatting color = enable ? ChatFormatting.GREEN : ChatFormatting.AQUA;
            source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(key).withStyle(color), false);
            return 1;
        }
        source.sendFailure(LocalizationHelper.getMessage("Only players can execute this command."));
        return 0;
    }
}

package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class RPAdminCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("on")
                                        .executes(context -> setMode(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                true
                                        )))
                                .then(Commands.literal("off")
                                        .executes(context -> setMode(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                false
                                        )))
                        )
                )
                .then(Commands.literal("config")
                        .then(Commands.literal("pvp")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigPvp(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("block_break")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigBlockBreak(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("block_place")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigBlockPlace(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("op_bypass")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigOpBypass(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                )
        );
    }

    private static int setMode(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enable) {
        int count = 0;
        for (ServerPlayer player : targets) {
            InRPAttachments.setInRP(player, enable);
            ScoreboardHandler.updatePlayerScoreboard(player);

            String playerMsgKey = enable ? "inrp.status.turned_on" : "inrp.status.turned_off";
            ChatFormatting color = enable ? ChatFormatting.GREEN : ChatFormatting.AQUA;
            player.sendSystemMessage(LocalizationHelper.getPrefixedMessage(playerMsgKey).withStyle(color));
            count++;
        }

        String statusKey = enable ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);
        final int finalCount = count;

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.set.success",
                statusComponent,
                finalCount
        ).withStyle(ChatFormatting.GOLD), true);

        return count;
    }

    private static int setConfigPvp(CommandSourceStack source, boolean value) {
        if (InRPConfig.PVP_ALLOWED_IN_RP.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.pvp.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.PVP_ALLOWED_IN_RP.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.pvp",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setConfigBlockBreak(CommandSourceStack source, boolean value) {
        if (InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.block_break.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.block_break",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setConfigBlockPlace(CommandSourceStack source, boolean value) {
        if (InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.block_place.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.block_place",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setConfigOpBypass(CommandSourceStack source, boolean value) {
        if (InRPConfig.OP_BYPASS_RESTRICTIONS.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.op_bypass.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.OP_BYPASS_RESTRICTIONS.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.op_bypass",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }
}

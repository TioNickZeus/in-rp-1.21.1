package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.HelpText;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/** {@code /rp} &mdash; the player-facing roleplay switch and help listing. */
public final class RPCommand {

    private static final List<HelpText.Entry> HELP_ENTRIES = List.of(
            new HelpText.Entry("inrp.help.rp", ChatFormatting.YELLOW),
            new HelpText.Entry("inrp.help.rp_on", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.help.rp_off", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.help.rp_toggle", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.help.roll", ChatFormatting.YELLOW),
            new HelpText.Entry("inrp.help.lives", ChatFormatting.YELLOW),
            new HelpText.Entry("inrp.help.afk", ChatFormatting.YELLOW),
            new HelpText.Entry("inrp.help.global", ChatFormatting.YELLOW)
    );

    private RPCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rp")
                .executes(context -> checkStatus(context.getSource()))
                .then(Commands.literal("on")
                        .executes(context -> setStatus(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setStatus(context.getSource(), false)))
                .then(Commands.literal("toggle")
                        .executes(context -> toggleStatus(context.getSource())))
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
        );
    }

    /** Sets a player's RP mode, refreshing their marker and giving audible feedback. */
    public static int setStatus(CommandSourceStack source, boolean enable) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return playersOnly(source);
        }

        if (InRPAttachments.isInRP(player) == enable) {
            source.sendFailure(LocalizationHelper
                    .getPrefixedMessage(enable ? "inrp.status.already_on" : "inrp.status.already_off")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPAttachments.setInRP(player, enable);
        ScoreboardHandler.updatePlayerScoreboard(player);

        if (enable) {
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        } else {
            player.playNotifySound(SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.3F, 1.0F);
        }

        String messageKey = enable ? "inrp.status.turned_on" : "inrp.status.turned_off";
        ChatFormatting color = enable ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(messageKey).withStyle(color), false);
        return 1;
    }

    private static int checkStatus(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return playersOnly(source);
        }

        boolean inRP = InRPAttachments.isInRP(player);
        String messageKey = inRP ? "inrp.status.current_on" : "inrp.status.current_off";
        ChatFormatting color = inRP ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(messageKey).withStyle(color), false);
        return 1;
    }

    private static int toggleStatus(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return playersOnly(source);
        }
        return setStatus(source, !InRPAttachments.isInRP(player));
    }

    private static int showHelp(CommandSourceStack source) {
        Component help = HelpText.build("inrp.help.header", HELP_ENTRIES);
        source.sendSuccess(() -> help, false);
        return 1;
    }

    private static int playersOnly(CommandSourceStack source) {
        source.sendFailure(LocalizationHelper.getMessage("inrp.error.players_only"));
        return 0;
    }
}

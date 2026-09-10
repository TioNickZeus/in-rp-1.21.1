package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** {@code /lives} &mdash; read-only view of a player's death count, limit and status. */
public final class LivesCommand {

    private LivesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lives")
                .executes(context -> checkOwnLives(context.getSource()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> displayLivesInfo(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "target")
                        )))
        );
    }

    private static int checkOwnLives(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return displayLivesInfo(source, player);
        }
        source.sendFailure(LocalizationHelper.getMessage("inrp.error.players_only"));
        return 0;
    }

    private static int displayLivesInfo(CommandSourceStack source, ServerPlayer target) {
        boolean hasLimit = InRPAttachments.hasLivesLimit(target);
        boolean isDead = InRPAttachments.isDead(target);

        String unlimited = LocalizationHelper.getRaw("inrp.lives.unlimited");
        String maxLives = hasLimit ? String.valueOf(InRPAttachments.getMaxLives(target)) : unlimited;
        String remaining = hasLimit ? String.valueOf(InRPAttachments.getRemainingLives(target)) : unlimited;

        Component status = Component
                .literal(LocalizationHelper.getRaw(isDead ? "inrp.lives.status_dead" : "inrp.lives.status_alive"))
                .withStyle(isDead ? ChatFormatting.RED : ChatFormatting.GREEN);

        MutableComponent info = Component.empty()
                .append(LocalizationHelper.getPrefixedMessage("inrp.lives.header", target.getDisplayName())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        appendLine(info, "inrp.lives.info_deaths", ChatFormatting.GRAY, InRPAttachments.getDeathCount(target));
        appendLine(info, "inrp.lives.info_max", ChatFormatting.GRAY, maxLives);
        appendLine(info, "inrp.lives.info_remaining", ChatFormatting.YELLOW, remaining);
        appendLine(info, "inrp.lives.info_status", ChatFormatting.GRAY, status);

        source.sendSuccess(() -> info, false);
        return 1;
    }

    private static void appendLine(MutableComponent target, String key, ChatFormatting color, Object argument) {
        target.append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage(key, argument).withStyle(color));
    }
}

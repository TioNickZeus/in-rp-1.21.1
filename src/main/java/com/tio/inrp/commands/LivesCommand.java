package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LivesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lives")
                .executes(context -> checkOwnLives(context.getSource()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> checkTargetLives(
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

    private static int checkTargetLives(CommandSourceStack source, ServerPlayer target) {
        return displayLivesInfo(source, target);
    }

    private static int displayLivesInfo(CommandSourceStack source, ServerPlayer target) {
        int deaths = InRPAttachments.getDeathCount(target);
        boolean hasLimit = InRPAttachments.hasLivesLimit(target);
        boolean isDead = InRPAttachments.isDead(target);

        String maxLivesStr = hasLimit ? String.valueOf(InRPAttachments.getMaxLives(target)) : LocalizationHelper.getRaw("inrp.lives.unlimited");
        String remainingStr = hasLimit ? String.valueOf(InRPAttachments.getRemainingLives(target)) : LocalizationHelper.getRaw("inrp.lives.unlimited");
        String statusStr = isDead ? LocalizationHelper.getRaw("inrp.lives.status_dead") : LocalizationHelper.getRaw("inrp.lives.status_alive");
        ChatFormatting statusColor = isDead ? ChatFormatting.RED : ChatFormatting.GREEN;

        Component info = Component.empty()
                .append(LocalizationHelper.getPrefixedMessage("inrp.lives.header", target.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.lives.info_deaths", deaths).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.lives.info_max", maxLivesStr).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.lives.info_remaining", remainingStr).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.lives.info_status", Component.literal(statusStr).withStyle(statusColor)).withStyle(ChatFormatting.GRAY));

        source.sendSuccess(() -> info, false);
        return 1;
    }
}

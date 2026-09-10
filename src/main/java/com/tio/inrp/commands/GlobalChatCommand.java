package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.AFKEventHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalChatCommand {
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("g")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "message")
                        )))
        );

        dispatcher.register(Commands.literal("global")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "message")
                        )))
        );
    }

    private static int execute(CommandSourceStack source, String message) {
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            source.sendFailure(LocalizationHelper.getMessage("inrp.chat.global.empty").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            long now = System.currentTimeMillis();
            int cooldownSeconds = InRPConfig.GLOBAL_CHAT_COOLDOWN_SECONDS.get();

            if (cooldownSeconds > 0) {
                Long nextAllowed = COOLDOWNS.get(player.getUUID());
                if (nextAllowed != null && now < nextAllowed) {
                    long remaining = Math.max(1, (nextAllowed - now + 999) / 1000);
                    source.sendFailure(LocalizationHelper.getMessage("inrp.chat.global.cooldown", remaining).withStyle(ChatFormatting.RED));
                    return 0;
                }
                COOLDOWNS.put(player.getUUID(), now + (cooldownSeconds * 1000L));
            }

            // Wake up player if they were AFK
            if (InRPAttachments.isAFK(player)) {
                AFKEventHandler.wakeUp(player);
            }

            MutableComponent formatted = Component.empty()
                    .append(Component.literal("[G] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(player.getDisplayName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(trimmed).withStyle(ChatFormatting.WHITE));

            player.server.getPlayerList().broadcastSystemMessage(formatted, false);
            return 1;
        } else {
            // Console broadcast
            MutableComponent formatted = Component.empty()
                    .append(Component.literal("[G] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(source.getDisplayName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(trimmed).withStyle(ChatFormatting.WHITE));

            source.getServer().getPlayerList().broadcastSystemMessage(formatted, false);
            return 1;
        }
    }
}

package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.events.AFKEventHandler;
import com.tio.inrp.util.ChatFormat;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /global} (aliased as {@code /g}) &mdash; server-wide chat, used when proximity local chat is enabled.
 *
 * <p>Messages are sent as system messages so vanilla clients accept them, and a configurable per-player cooldown
 * keeps the channel from being flooded.
 */
public final class GlobalChatCommand {

    /** Vanilla's own chat length limit; guards against oversized messages from modified clients. */
    private static final int MAX_MESSAGE_LENGTH = 256;

    /** Earliest time, per player, at which the next global message is allowed. */
    private static final Map<UUID, Long> COOLDOWN_EXPIRY = new ConcurrentHashMap<>();

    private GlobalChatCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> global = dispatcher.register(Commands.literal("global")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "message")
                        )))
        );

        // Alias rather than a second definition, so both spellings always behave identically.
        dispatcher.register(Commands.literal("g").redirect(global));
    }

    /** Forgets a player's cooldown, e.g. when they disconnect. */
    public static void clearCooldown(UUID uuid) {
        if (uuid != null) {
            COOLDOWN_EXPIRY.remove(uuid);
        }
    }

    /** Drops every cooldown. Called on server shutdown so nothing leaks into the next world load. */
    public static void reset() {
        COOLDOWN_EXPIRY.clear();
    }

    private static int execute(CommandSourceStack source, String rawMessage) {
        String message = ChatFormat.sanitize(rawMessage, MAX_MESSAGE_LENGTH);
        if (message.isEmpty()) {
            source.sendFailure(LocalizationHelper.getMessage("inrp.chat.global.empty").withStyle(ChatFormatting.RED));
            return 0;
        }

        Component senderName = source.getDisplayName();
        if (source.getEntity() instanceof ServerPlayer player) {
            long remainingSeconds = remainingCooldownSeconds(player.getUUID());
            if (remainingSeconds > 0) {
                source.sendFailure(LocalizationHelper
                        .getMessage("inrp.chat.global.cooldown", remainingSeconds)
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            AFKEventHandler.wakeUp(player);
            senderName = player.getDisplayName();
        }

        Component formatted = ChatFormat.channelMessage(
                ChatFormat.tag("inrp.chat.global.tag", ChatFormatting.GOLD, ChatFormatting.BOLD),
                senderName, message, ChatFormatting.WHITE);

        source.getServer().getPlayerList().broadcastSystemMessage(formatted, false);
        return 1;
    }

    /**
     * Starts the cooldown for the caller.
     *
     * @return {@code 0} when the message may be sent, otherwise the whole seconds still to wait.
     */
    private static long remainingCooldownSeconds(UUID uuid) {
        int cooldownSeconds = InRPConfig.GLOBAL_CHAT_COOLDOWN_SECONDS.get();
        if (cooldownSeconds <= 0) {
            return 0L;
        }

        long now = Util.getMillis();
        Long expiresAt = COOLDOWN_EXPIRY.get(uuid);
        if (expiresAt != null && now < expiresAt) {
            return Math.max(1L, (expiresAt - now + 999L) / 1000L);
        }

        COOLDOWN_EXPIRY.put(uuid, now + cooldownSeconds * 1000L);
        return 0L;
    }
}

package com.tio.inrp.events;

import com.mojang.brigadier.ParseResults;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.util.ChatFormat;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Turns ordinary chat into proximity local chat, and feeds the staff chat spy.
 *
 * <p>Messages are delivered as system messages rather than player chat. That is what keeps the mod usable by
 * vanilla clients: re-broadcasting signed player chat to a different audience than the one the client signed it for
 * makes those clients raise a chat validation error, whereas system messages carry no signature.
 *
 * <p>AFK wake-up on chat is owned by {@link AFKEventHandler}, which listens with {@code receiveCanceled = true} so
 * it still fires after this handler cancels the vanilla broadcast.
 */
public final class ChatEventHandler {

    /** Vanilla's own chat length limit; guards against oversized messages from modified clients. */
    private static final int MAX_MESSAGE_LENGTH = 256;

    /** Vanilla private-message commands mirrored to staff when {@code spyPrivateMessages} is enabled. */
    private static final Set<String> PRIVATE_MESSAGE_COMMANDS = Set.of("tell", "msg", "w");

    private ChatEventHandler() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!InRPConfig.LOCAL_CHAT_ENABLED.get()) {
            return;
        }

        ServerPlayer sender = event.getPlayer();
        if (sender == null) {
            return;
        }

        // Take over delivery: the vanilla broadcast would reach the whole server.
        event.setCanceled(true);

        String message = ChatFormat.sanitize(event.getRawText(), MAX_MESSAGE_LENGTH);
        if (message.isEmpty()) {
            return;
        }

        MutableComponent localMessage = ChatFormat.channelMessage(
                ChatFormat.tag("inrp.chat.local.tag", ChatFormatting.GRAY),
                sender.getDisplayName(), message, ChatFormatting.WHITE);

        ServerLevel senderLevel = sender.serverLevel();
        double radiusSq = InRPConfig.localChatRadiusSq();
        boolean spyEnabled = InRPConfig.SPY_LOCAL_CHAT.get();
        MutableComponent spyMessage = null;
        int heardBy = 0;

        // One pass over the player list: everyone in range hears the message, watching staff out of range get a copy.
        for (ServerPlayer recipient : sender.server.getPlayerList().getPlayers()) {
            boolean inRange = recipient.serverLevel() == senderLevel
                    && recipient.distanceToSqr(sender) <= radiusSq;

            if (inRange) {
                recipient.sendSystemMessage(localMessage);
                heardBy++;
            } else if (spyEnabled && recipient != sender && ChatFormat.isActiveChatSpy(recipient)) {
                if (spyMessage == null) {
                    spyMessage = ChatFormat.channelMessage(
                            ChatFormat.tag("inrp.chat.spy.local.tag", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                            sender.getDisplayName(), message, ChatFormatting.DARK_AQUA);
                }
                recipient.sendSystemMessage(spyMessage);
            }
        }

        // The sender always hears themselves, so anything above one means somebody else did too.
        if (heardBy <= 1) {
            sender.sendSystemMessage(LocalizationHelper.getMessage("inrp.chat.local.no_one_heard")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        // Keep the server log complete for moderation and log-scraping integrations.
        sender.server.sendSystemMessage(localMessage);
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!InRPConfig.SPY_PRIVATE_MESSAGES.get()) {
            return;
        }

        ParseResults<CommandSourceStack> parseResults = event.getParseResults();
        if (parseResults == null || parseResults.getReader() == null) {
            return;
        }
        if (!(parseResults.getContext().getSource().getEntity() instanceof ServerPlayer sender)) {
            return;
        }

        String input = parseResults.getReader().getString();
        if (input == null || input.isBlank()) {
            return;
        }
        if (input.startsWith("/")) {
            input = input.substring(1);
        }

        String[] parts = input.strip().split("\\s+", 3);
        if (parts.length < 3 || !PRIVATE_MESSAGE_COMMANDS.contains(commandName(parts[0]))) {
            return;
        }

        String targetName = parts[1];
        String message = ChatFormat.sanitize(parts[2], MAX_MESSAGE_LENGTH);
        if (message.isEmpty()) {
            return;
        }

        MutableComponent spyMessage = null;
        for (ServerPlayer recipient : sender.server.getPlayerList().getPlayers()) {
            if (recipient == sender
                    || recipient.getScoreboardName().equalsIgnoreCase(targetName)
                    || !ChatFormat.isActiveChatSpy(recipient)) {
                continue;
            }
            if (spyMessage == null) {
                spyMessage = ChatFormat.directedMessage(
                        ChatFormat.tag("inrp.chat.spy.pm.tag", ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                        sender.getDisplayName(), targetName, message, ChatFormatting.LIGHT_PURPLE);
            }
            recipient.sendSystemMessage(spyMessage);
        }
    }

    /** Strips an explicit namespace so {@code /minecraft:tell} is treated like {@code /tell}. */
    private static String commandName(String literal) {
        String name = literal.toLowerCase(Locale.ROOT);
        int namespaceEnd = name.indexOf(':');
        return namespaceEnd >= 0 ? name.substring(namespaceEnd + 1) : name;
    }
}

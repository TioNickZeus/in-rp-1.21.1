package com.tio.inrp.events;

import com.mojang.brigadier.ParseResults;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class ChatEventHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!InRPConfig.LOCAL_CHAT_ENABLED.get()) {
            return;
        }

        ServerPlayer sender = event.getPlayer();
        if (sender == null) {
            return;
        }

        // Cancel vanilla broadcast to handle local proximity routing safely
        event.setCanceled(true);

        // Wake up player if they were AFK
        if (InRPAttachments.isAFK(sender)) {
            AFKEventHandler.wakeUp(sender);
        }

        String rawText = event.getRawText();
        if (rawText == null || rawText.trim().isEmpty()) {
            return;
        }
        String trimmed = rawText.trim();

        // [L] Player: message
        MutableComponent localMessage = Component.empty()
                .append(Component.literal("[L] ").withStyle(ChatFormatting.GRAY))
                .append(sender.getDisplayName())
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(trimmed).withStyle(ChatFormatting.WHITE));

        double radius = InRPConfig.LOCAL_CHAT_RADIUS.get();
        double radiusSq = radius * radius;

        Set<ServerPlayer> recipients = new HashSet<>();
        for (ServerPlayer nearby : sender.serverLevel().players()) {
            if (nearby.distanceToSqr(sender) <= radiusSq) {
                nearby.sendSystemMessage(localMessage);
                recipients.add(nearby);
            }
        }

        // Feedback when no one is nearby
        if (recipients.size() <= 1) {
            sender.sendSystemMessage(
                    LocalizationHelper.getMessage("inrp.chat.local.no_one_heard")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            );
        }

        // Log to server console
        sender.server.sendSystemMessage(localMessage);

        // Chat Spy for local chat
        if (InRPConfig.SPY_LOCAL_CHAT.get()) {
            MutableComponent spyMessage = Component.empty()
                    .append(Component.literal("[SPY:L] ").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))
                    .append(sender.getDisplayName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(trimmed).withStyle(ChatFormatting.DARK_AQUA));

            for (ServerPlayer admin : sender.server.getPlayerList().getPlayers()) {
                if (admin != sender && !recipients.contains(admin) && InRPAttachments.isChatSpy(admin)) {
                    admin.sendSystemMessage(spyMessage);
                }
            }
        }
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

        CommandSourceStack source = parseResults.getContext().getSource();
        if (source == null || !(source.getEntity() instanceof ServerPlayer sender)) {
            return;
        }

        String input = parseResults.getReader().getString();
        if (input == null || input.isEmpty()) {
            return;
        }

        if (input.startsWith("/")) {
            input = input.substring(1);
        }

        String[] parts = input.trim().split("\\s+", 3);
        if (parts.length < 3) {
            return;
        }

        String cmd = parts[0].toLowerCase();
        if (cmd.equals("tell") || cmd.equals("msg") || cmd.equals("w")) {
            String targetName = parts[1];
            String message = parts[2];

            MutableComponent spyPm = Component.empty()
                    .append(Component.literal("[SPY:PM] ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC))
                    .append(sender.getDisplayName())
                    .append(Component.literal(" -> " + targetName + ": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE));

            for (ServerPlayer admin : sender.server.getPlayerList().getPlayers()) {
                if (admin != sender && !admin.getScoreboardName().equalsIgnoreCase(targetName) && InRPAttachments.isChatSpy(admin)) {
                    admin.sendSystemMessage(spyPm);
                }
            }
        }
    }
}

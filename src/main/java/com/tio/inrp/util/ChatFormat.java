package com.tio.inrp.util;

import com.tio.inrp.InRP;
import com.tio.inrp.data.InRPAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared composition rules for the chat channels In-RP delivers itself ({@code [L]}, {@code [G]} and the staff spy
 * copies), so every channel is sanitised and laid out the same way.
 */
public final class ChatFormat {

    private ChatFormat() {
    }

    /**
     * Normalises player input before it is turned into a component.
     *
     * @return the trimmed message, truncated to {@code maxLength}, or an empty string when there is nothing to send.
     */
    public static String sanitize(String raw, int maxLength) {
        if (raw == null) {
            return "";
        }
        String message = raw.strip();
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }

    /** @return a localized channel tag such as {@code [L]} or {@code [G]}. */
    public static MutableComponent tag(String key, ChatFormatting... styles) {
        return LocalizationHelper.getMessage(key).withStyle(styles);
    }

    /** @return {@code <tag> <name>: <message>}. */
    public static MutableComponent channelMessage(Component tag, Component name, String message, ChatFormatting messageStyle) {
        return Component.empty()
                .append(tag)
                .append(Component.literal(" "))
                .append(name)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(message).withStyle(messageStyle));
    }

    /** @return {@code <tag> <name> -> <target>: <message>}, used for mirrored private messages. */
    public static MutableComponent directedMessage(Component tag, Component name, String targetName, String message,
                                                   ChatFormatting messageStyle) {
        return Component.empty()
                .append(tag)
                .append(Component.literal(" "))
                .append(name)
                .append(Component.literal(" -> " + targetName + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(message).withStyle(messageStyle));
    }

    /**
     * Chat spy is a staff tool whose toggle persists in the player's save data, so the permission level is
     * re-checked at delivery time: a player who lost their operator status must stop receiving copies even if their
     * toggle was never turned off.
     */
    public static boolean isActiveChatSpy(ServerPlayer player) {
        return player != null
                && InRPAttachments.isChatSpy(player)
                && player.hasPermissions(InRP.STAFF_PERMISSION_LEVEL);
    }
}

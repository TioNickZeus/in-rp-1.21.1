package com.tio.inrp.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfirmationManager {

    private static final long EXPIRY_MS = 10_000; // 10 seconds
    private static final Map<UUID, PendingAction> PENDING_ACTIONS = new ConcurrentHashMap<>();

    public static void requestConfirmation(CommandSourceStack source, UUID adminUUID, String description, Runnable action) {
        // Clean up any expired entries
        PENDING_ACTIONS.entrySet().removeIf(entry -> entry.getValue().isExpired());

        PENDING_ACTIONS.put(adminUUID, new PendingAction(action, System.currentTimeMillis(), description));

        MutableComponent confirmButton = LocalizationHelper.getMessage("inrp.admin.confirm.click")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rpadmin confirm"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("/rpadmin confirm").withStyle(ChatFormatting.GRAY)))
                );

        source.sendSystemMessage(
                LocalizationHelper.getPrefixedMessage("inrp.admin.confirm.pending", description)
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(" "))
                        .append(confirmButton)
        );
    }

    public static boolean confirm(UUID adminUUID) {
        PendingAction pending = PENDING_ACTIONS.remove(adminUUID);
        if (pending == null || pending.isExpired()) {
            return false;
        }
        pending.action.run();
        return true;
    }

    public static boolean hasPending(UUID adminUUID) {
        PendingAction pending = PENDING_ACTIONS.get(adminUUID);
        return pending != null && !pending.isExpired();
    }

    private static class PendingAction {
        final Runnable action;
        final long timestamp;
        final String description;

        PendingAction(Runnable action, long timestamp, String description) {
            this.action = action;
            this.timestamp = timestamp;
            this.description = description;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRY_MS;
        }
    }
}

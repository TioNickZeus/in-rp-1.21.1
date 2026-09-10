package com.tio.inrp.util;

import com.tio.inrp.InRP;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-step confirmation for administrative commands that affect many players at once.
 *
 * <p>Each administrator may have a single pending action, keyed by UUID. Console sources have no UUID and therefore
 * never go through this manager &mdash; their commands run immediately.
 */
public final class ConfirmationManager {

    /** How long a pending action stays confirmable. Mirrored by the {@code inrp.admin.confirm.pending} message. */
    public static final long EXPIRY_SECONDS = 10L;

    private static final long EXPIRY_MS = EXPIRY_SECONDS * 1000L;
    private static final Map<UUID, PendingAction> PENDING_ACTIONS = new ConcurrentHashMap<>();

    private ConfirmationManager() {
    }

    /**
     * Stores {@code action} as the administrator's pending action, replacing any previous one, and sends them a
     * clickable confirmation prompt.
     */
    public static void requestConfirmation(CommandSourceStack source, UUID adminUUID, String description, Runnable action) {
        purgeExpired();
        PENDING_ACTIONS.put(adminUUID, new PendingAction(action, Util.getMillis(), description));

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

    /**
     * Runs and clears the administrator's pending action.
     *
     * @return {@code false} when there is nothing pending or it already expired.
     */
    public static boolean confirm(UUID adminUUID) {
        if (adminUUID == null) {
            return false;
        }
        PendingAction pending = PENDING_ACTIONS.remove(adminUUID);
        if (pending == null || pending.isExpired()) {
            return false;
        }
        InRP.LOGGER.info("Confirmed pending In-RP admin action for {}: {}", adminUUID, pending.description());
        pending.action().run();
        return true;
    }

    /** Drops any pending action for a single administrator, e.g. when they disconnect. */
    public static void clear(UUID adminUUID) {
        if (adminUUID != null) {
            PENDING_ACTIONS.remove(adminUUID);
        }
    }

    /** Drops every pending action. Called on server shutdown so state never leaks into the next world. */
    public static void reset() {
        PENDING_ACTIONS.clear();
    }

    private static void purgeExpired() {
        PENDING_ACTIONS.values().removeIf(PendingAction::isExpired);
    }

    private record PendingAction(Runnable action, long requestedAt, String description) {
        boolean isExpired() {
            return Util.getMillis() - requestedAt > EXPIRY_MS;
        }
    }
}

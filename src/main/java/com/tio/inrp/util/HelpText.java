package com.tio.inrp.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Builds the multi-line help listings shown by {@code /rp help} and {@code /rpadmin help}.
 *
 * <p>The listing is rooted in an empty component so each line's own colour applies cleanly instead of inheriting
 * the bold header style.
 */
public final class HelpText {

    private HelpText() {
    }

    /** One help line: a translation key and the colour it is rendered in. */
    public record Entry(String key, ChatFormatting color) {
    }

    /** @return the header followed by one line per entry. */
    public static Component build(String headerKey, List<Entry> entries) {
        MutableComponent help = Component.empty().append(LocalizationHelper.getPrefixedMessage(headerKey)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        for (Entry entry : entries) {
            help.append(Component.literal("\n"))
                    .append(LocalizationHelper.getMessage(entry.key()).withStyle(entry.color()));
        }
        return help;
    }
}

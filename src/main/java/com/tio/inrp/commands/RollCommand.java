package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RollCommand {
    private static final Pattern DICE_PATTERN = Pattern.compile("^(\\d+)?d(\\d+)$", Pattern.CASE_INSENSITIVE);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roll")
                .executes(context -> executeDefaultRoll(context.getSource()))
                .then(Commands.argument("dice", StringArgumentType.word())
                        .executes(context -> executeCustomRoll(
                                context.getSource(),
                                StringArgumentType.getString(context, "dice")
                        )))
        );
    }

    private static int executeDefaultRoll(CommandSourceStack source) {
        int sides = InRPConfig.ROLL_DEFAULT_SIDES.get();
        return performSimpleRoll(source, sides);
    }

    private static int executeCustomRoll(CommandSourceStack source, String input) {
        // Try parsing as simple integer (e.g. "20", "100")
        try {
            int sides = Integer.parseInt(input);
            if (sides < 2 || sides > 10000) {
                source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.roll.error.number_bounds")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }
            return performSimpleRoll(source, sides);
        } catch (NumberFormatException ignored) {
        }

        // Try parsing as dice notation (e.g. "2d6", "d20", "4d10")
        Matcher matcher = DICE_PATTERN.matcher(input);
        if (matcher.matches()) {
            String countStr = matcher.group(1);
            int count = (countStr == null || countStr.isEmpty()) ? 1 : Integer.parseInt(countStr);
            int sides = Integer.parseInt(matcher.group(2));

            if (count < 1 || count > 100 || sides < 2 || sides > 10000) {
                source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.roll.error.number_bounds")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            return performDiceRoll(source, count, sides);
        }

        source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.roll.error.invalid_format")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int performSimpleRoll(CommandSourceStack source, int sides) {
        int result = ThreadLocalRandom.current().nextInt(1, sides + 1);
        Component playerName = source.getDisplayName();

        Component message = LocalizationHelper.getPrefixedMessage(
                "inrp.roll.result.simple",
                playerName,
                result,
                sides
        ).withStyle(ChatFormatting.YELLOW);

        broadcastRoll(source, message);
        return result;
    }

    private static int performDiceRoll(CommandSourceStack source, int count, int sides) {
        int total = 0;
        List<Integer> rolls = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int r = ThreadLocalRandom.current().nextInt(1, sides + 1);
            rolls.add(r);
            total += r;
        }

        Component playerName = source.getDisplayName();
        String individualRolls = rolls.toString();

        Component message = LocalizationHelper.getPrefixedMessage(
                "inrp.roll.result.dice",
                playerName,
                total,
                individualRolls,
                count,
                sides
        ).withStyle(ChatFormatting.YELLOW);

        broadcastRoll(source, message);
        return total;
    }

    private static void broadcastRoll(CommandSourceStack source, Component message) {
        double radius = InRPConfig.ROLL_PROXIMITY_RADIUS.get();

        if (radius <= 0 || !(source.getEntity() instanceof ServerPlayer player)) {
            // Global broadcast
            source.getServer().getPlayerList().broadcastSystemMessage(message, false);
            return;
        }

        double radiusSq = radius * radius;
        int recipientCount = 0;
        for (ServerPlayer nearbyPlayer : player.serverLevel().players()) {
            if (nearbyPlayer.distanceToSqr(player) <= radiusSq) {
                nearbyPlayer.sendSystemMessage(message);
                recipientCount++;
            }
        }

        if (recipientCount <= 1) {
            player.sendSystemMessage(
                    LocalizationHelper.getMessage("inrp.roll.no_one_heard")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            );
        }
    }
}

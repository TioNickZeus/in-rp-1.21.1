package com.tio.inrp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code /roll} &mdash; dice rolls in either plain ({@code /roll 20}) or RPG ({@code /roll 2d6}) notation.
 *
 * <p>Results are broadcast to everyone within {@code rollProximityRadius} blocks, or server-wide when the radius is
 * disabled, and are always written to the server log so a roll can be audited after the fact.
 */
public final class RollCommand {

    private static final int MIN_SIDES = 2;
    private static final int MAX_SIDES = 10_000;
    private static final int MIN_DICE = 1;
    private static final int MAX_DICE = 100;

    /**
     * Digit runs are length-capped so a value such as {@code 1d99999999999} cannot overflow {@code parseInt} and
     * surface as a raw exception to the player.
     */
    private static final Pattern DICE_PATTERN = Pattern.compile("^(\\d{1,9})?[dD](\\d{1,9})$");
    private static final Pattern SIDES_PATTERN = Pattern.compile("^\\d{1,9}$");

    private static final String[] SUGGESTIONS = {"20", "100", "2d6", "3d20"};

    private RollCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roll")
                .executes(context -> performSimpleRoll(context.getSource(), InRPConfig.ROLL_DEFAULT_SIDES.get()))
                .then(Commands.argument("dice", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(SUGGESTIONS, builder))
                        .executes(context -> executeCustomRoll(
                                context.getSource(),
                                StringArgumentType.getString(context, "dice")
                        )))
        );
    }

    private static int executeCustomRoll(CommandSourceStack source, String input) {
        Matcher diceNotation = DICE_PATTERN.matcher(input);
        if (diceNotation.matches()) {
            String count = diceNotation.group(1);
            return performDiceRoll(source,
                    count == null ? 1 : parseOrInvalid(count),
                    parseOrInvalid(diceNotation.group(2)));
        }

        if (SIDES_PATTERN.matcher(input).matches()) {
            return performSimpleRoll(source, parseOrInvalid(input));
        }

        source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.roll.error.invalid_format")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    /** @return the rolled value, so command blocks and {@code /execute store} can read the result. */
    private static int performSimpleRoll(CommandSourceStack source, int sides) {
        if (sides < MIN_SIDES || sides > MAX_SIDES) {
            return outOfBounds(source);
        }

        int result = ThreadLocalRandom.current().nextInt(1, sides + 1);
        broadcastRoll(source, LocalizationHelper.getPrefixedMessage(
                "inrp.roll.result.simple", source.getDisplayName(), result, sides).withStyle(ChatFormatting.YELLOW));
        return result;
    }

    /** @return the sum of the dice. */
    private static int performDiceRoll(CommandSourceStack source, int count, int sides) {
        if (count < MIN_DICE || count > MAX_DICE || sides < MIN_SIDES || sides > MAX_SIDES) {
            return outOfBounds(source);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Integer> rolls = new ArrayList<>(count);
        int total = 0;
        for (int i = 0; i < count; i++) {
            int roll = random.nextInt(1, sides + 1);
            rolls.add(roll);
            total += roll;
        }

        broadcastRoll(source, LocalizationHelper.getPrefixedMessage(
                "inrp.roll.result.dice", source.getDisplayName(), total, rolls.toString(), count, sides)
                .withStyle(ChatFormatting.YELLOW));
        return total;
    }

    private static void broadcastRoll(CommandSourceStack source, Component message) {
        double radius = InRPConfig.ROLL_PROXIMITY_RADIUS.get();

        if (radius <= 0 || !(source.getEntity() instanceof ServerPlayer player)) {
            // Global broadcast; also reaches the server log.
            source.getServer().getPlayerList().broadcastSystemMessage(message, false);
            return;
        }

        double radiusSq = radius * radius;
        ServerLevel level = player.serverLevel();
        int heardBy = 0;
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(player) <= radiusSq) {
                nearby.sendSystemMessage(message);
                heardBy++;
            }
        }

        // The roller always hears themselves, so anything above one means somebody else did too.
        if (heardBy <= 1) {
            player.sendSystemMessage(LocalizationHelper.getMessage("inrp.roll.no_one_heard")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        player.server.sendSystemMessage(message);
    }

    private static int outOfBounds(CommandSourceStack source) {
        source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.roll.error.number_bounds")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    /** @return the parsed value, or {@code -1} when the digits overflow an {@code int} so bounds checks reject it. */
    private static int parseOrInvalid(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

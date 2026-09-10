package com.tio.inrp.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tio.inrp.InRP;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.data.InRPLivesManager;
import com.tio.inrp.events.LivesEventHandler;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.ConfirmationManager;
import com.tio.inrp.util.HelpText;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * {@code /rpadmin} &mdash; staff administration for roleplay state, gameplay rules and the lives system.
 *
 * <p>Commands that would touch {@value #CONFIRMATION_THRESHOLD} players or more are staged through
 * {@link ConfirmationManager} first. Because a staged action may run up to ten seconds later, targets are stored as
 * UUIDs and re-resolved when it executes: writing to a {@code ServerPlayer} that disconnected in the meantime would
 * silently discard the change.
 */
public final class RPAdminCommand {

    /** Number of affected players from which an explicit confirmation is required. */
    private static final int CONFIRMATION_THRESHOLD = 5;

    private static final int MIN_LIVES = -1;
    private static final int MAX_LIVES = 100_000;

    private static final List<HelpText.Entry> HELP_ENTRIES = List.of(
            new HelpText.Entry("inrp.admin.help.set", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.config", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.lives_set", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.lives_revive", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.lives_setdeaths", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.lives_action", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.lives_applydefault", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.confirm", ChatFormatting.GRAY),
            new HelpText.Entry("inrp.admin.help.spy", ChatFormatting.GRAY)
    );

    /** An action applied to a freshly resolved set of online targets. */
    @FunctionalInterface
    private interface TargetAction {
        int run(List<ServerPlayer> targets);
    }

    private RPAdminCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpadmin")
                .requires(source -> source.hasPermission(InRP.STAFF_PERMISSION_LEVEL))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("on")
                                        .executes(context -> setMode(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> setMode(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"), false)))
                        )
                )
                .then(Commands.literal("config")
                        .then(ruleNode("pvp", InRPConfig.PVP_ALLOWED_IN_RP, "inrp.admin.config.pvp"))
                        .then(ruleNode("block_break", InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP, "inrp.admin.config.block_break"))
                        .then(ruleNode("block_place", InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP, "inrp.admin.config.block_place"))
                        .then(ruleNode("op_bypass", InRPConfig.OP_BYPASS_RESTRICTIONS, "inrp.admin.config.op_bypass"))
                )
                .then(Commands.literal("lives")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(MIN_LIVES, MAX_LIVES))
                                                .executes(context -> setLives(context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("revive")
                                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                        .executes(context -> revive(context.getSource(),
                                                GameProfileArgument.getGameProfiles(context, "targets")))))
                        .then(Commands.literal("setdeaths")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, MAX_LIVES))
                                                .executes(context -> setDeaths(context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("action")
                                .then(Commands.literal(InRPConfig.LIVES_ACTION_SPECTATOR)
                                        .executes(context -> setLivesAction(context.getSource(),
                                                InRPConfig.LIVES_ACTION_SPECTATOR)))
                                .then(Commands.literal(InRPConfig.LIVES_ACTION_KICK)
                                        .executes(context -> setLivesAction(context.getSource(),
                                                InRPConfig.LIVES_ACTION_KICK))))
                        .then(Commands.literal("applydefault")
                                .executes(context -> applyDefaultLives(context.getSource(), null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> applyDefaultLives(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")))))
                )
                .then(Commands.literal("confirm")
                        .executes(context -> executeConfirm(context.getSource())))
                .then(Commands.literal("spy")
                        .executes(context -> ChatSpyCommand.toggleSpy(context.getSource())))
                .then(Commands.literal("help")
                        .executes(context -> showAdminHelp(context.getSource())))
        );
    }

    // ------------------------------------------------------------------- RP mode

    private static int setMode(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enable) {
        String status = LocalizationHelper.getRaw(enable ? "inrp.admin.status_on" : "inrp.admin.status_off");
        return stage(source, targets,
                LocalizationHelper.format("inrp.admin.confirm.desc.setmode", targets.size(), status),
                players -> executeSetMode(source, players, enable));
    }

    private static int executeSetMode(CommandSourceStack source, List<ServerPlayer> targets, boolean enable) {
        String messageKey = enable ? "inrp.status.turned_on" : "inrp.status.turned_off";
        ChatFormatting color = enable ? ChatFormatting.GREEN : ChatFormatting.AQUA;

        for (ServerPlayer player : targets) {
            InRPAttachments.setInRP(player, enable);
            ScoreboardHandler.updatePlayerScoreboard(player);
            player.sendSystemMessage(LocalizationHelper.getPrefixedMessage(messageKey).withStyle(color));
        }

        Component status = LocalizationHelper.getMessage(enable ? "inrp.admin.status_on" : "inrp.admin.status_off");
        int affected = targets.size();
        source.sendSuccess(() -> LocalizationHelper
                .getPrefixedMessage("inrp.admin.set.success", status, affected)
                .withStyle(ChatFormatting.GOLD), true);
        return affected;
    }

    // -------------------------------------------------------------- Gameplay rules

    private static LiteralArgumentBuilder<CommandSourceStack> ruleNode(String name,
                                                                       ModConfigSpec.BooleanValue setting,
                                                                       String messageKey) {
        return Commands.literal(name)
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setRule(context.getSource(), setting, messageKey,
                                BoolArgumentType.getBool(context, "value"))));
    }

    private static int setRule(CommandSourceStack source, ModConfigSpec.BooleanValue setting, String messageKey,
                               boolean value) {
        Component status = LocalizationHelper.getMessage(value ? "inrp.admin.status_on" : "inrp.admin.status_off");

        // Avoid a pointless disk write, and tell the admin nothing changed.
        if (setting.get() == value) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage(messageKey + ".already", status)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        setting.set(value);
        InRPConfig.SPEC.save();

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(messageKey, status)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // --------------------------------------------------------------------- Lives

    private static int setLives(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        return stage(source, targets,
                LocalizationHelper.format("inrp.admin.confirm.desc.setlives", targets.size(), amount),
                players -> executeSetLives(source, players, amount));
    }

    private static int executeSetLives(CommandSourceStack source, List<ServerPlayer> targets, int amount) {
        // Anything at or below zero other than the explicit -1 sentinel also means "no limit".
        int maxLives = amount <= 0 ? InRPAttachments.UNLIMITED_LIVES : amount;

        for (ServerPlayer player : targets) {
            InRPAttachments.setMaxLives(player, maxLives);
            // Granting a finite limit the player is still within brings them back; "unlimited" is left alone so an
            // admin can raise the cap without implicitly reviving an eliminated character.
            if (maxLives > 0 && InRPAttachments.isDead(player) && !InRPAttachments.hasRunOutOfLives(player)) {
                LivesEventHandler.revive(player);
            }
        }

        String amountText = maxLives == InRPAttachments.UNLIMITED_LIVES
                ? LocalizationHelper.getRaw("inrp.lives.unlimited")
                : String.valueOf(maxLives);
        int affected = targets.size();
        source.sendSuccess(() -> LocalizationHelper
                .getPrefixedMessage("inrp.admin.lives.set.success", amountText, affected)
                .withStyle(ChatFormatting.GOLD), true);
        return affected;
    }

    private static int setDeaths(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        return stage(source, targets,
                LocalizationHelper.format("inrp.admin.confirm.desc.setdeaths", targets.size(), amount),
                players -> executeSetDeaths(source, players, amount));
    }

    private static int executeSetDeaths(CommandSourceStack source, List<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            InRPAttachments.setDeathCount(player, amount);

            if (InRPAttachments.hasRunOutOfLives(player)) {
                LivesEventHandler.markEliminated(player);
                LivesEventHandler.enforceEliminationState(player);
            } else if (InRPAttachments.isDead(player)) {
                // Back below the limit: lift the elimination but keep the death count the admin just set.
                LivesEventHandler.clearElimination(player, false);
            } else {
                ScoreboardHandler.refreshPlayerTabList(player);
            }
        }

        int affected = targets.size();
        source.sendSuccess(() -> LocalizationHelper
                .getPrefixedMessage("inrp.admin.lives.setdeaths.success", amount, affected)
                .withStyle(ChatFormatting.GOLD), true);
        return affected;
    }

    /**
     * Revives by game profile rather than entity selector, so offline players can be revived too: their UUID is
     * removed from the world-level store and the login handler restores them on their next connection.
     */
    private static int revive(CommandSourceStack source, Collection<GameProfile> profiles) {
        List<UUID> ids = new ArrayList<>(profiles.size());
        for (GameProfile profile : profiles) {
            if (profile.getId() != null) {
                ids.add(profile.getId());
            }
        }

        // Single disk write for the whole batch.
        InRPLivesManager.unmarkDeadAll(ids);

        PlayerList playerList = source.getServer().getPlayerList();
        for (UUID id : ids) {
            ServerPlayer player = playerList.getPlayer(id);
            if (player != null) {
                LivesEventHandler.revive(player);
            }
        }

        int affected = ids.size();
        source.sendSuccess(() -> LocalizationHelper
                .getPrefixedMessage("inrp.admin.lives.revive.success", affected)
                .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int setLivesAction(CommandSourceStack source, String action) {
        if (InRPConfig.LIVES_ACTION.get().equalsIgnoreCase(action)) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.admin.lives.action.already", action)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.LIVES_ACTION.set(action);
        InRPConfig.SPEC.save();

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage("inrp.admin.lives.action.success", action)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** @param targets explicit targets, or {@code null} to apply to every online player. */
    private static int applyDefaultLives(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int defaultMaxLives = InRPConfig.DEFAULT_MAX_LIVES.get();
        if (defaultMaxLives <= 0) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.admin.lives.applydefault.disabled")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        Collection<ServerPlayer> players = targets != null ? targets : source.getServer().getPlayerList().getPlayers();
        return stage(source, players,
                LocalizationHelper.format("inrp.admin.confirm.desc.applydefault", players.size(), defaultMaxLives),
                resolved -> executeApplyDefaultLives(source, resolved, defaultMaxLives));
    }

    private static int executeApplyDefaultLives(CommandSourceStack source, List<ServerPlayer> targets, int defaultMaxLives) {
        for (ServerPlayer player : targets) {
            InRPAttachments.setMaxLives(player, defaultMaxLives);
            if (InRPAttachments.isDead(player) && !InRPAttachments.hasRunOutOfLives(player)) {
                LivesEventHandler.revive(player);
            }
        }

        String amountText = String.valueOf(defaultMaxLives);
        int affected = targets.size();
        source.sendSuccess(() -> LocalizationHelper
                .getPrefixedMessage("inrp.admin.lives.applydefault.success", amountText, affected)
                .withStyle(ChatFormatting.GOLD), true);
        return affected;
    }

    // -------------------------------------------------------------- Confirmation

    /**
     * Runs {@code action} immediately for small selections and for console sources, or stages it behind
     * {@code /rpadmin confirm} when it would affect {@value #CONFIRMATION_THRESHOLD} players or more.
     */
    private static int stage(CommandSourceStack source, Collection<ServerPlayer> targets, String description,
                             TargetAction action) {
        UUID adminUUID = adminUUID(source);
        if (targets.size() < CONFIRMATION_THRESHOLD || adminUUID == null) {
            return action.run(List.copyOf(targets));
        }

        List<UUID> targetIds = new ArrayList<>(targets.size());
        for (ServerPlayer target : targets) {
            targetIds.add(target.getUUID());
        }

        ConfirmationManager.requestConfirmation(source, adminUUID, description,
                () -> action.run(resolveOnline(source, targetIds)));
        return 0;
    }

    private static int executeConfirm(CommandSourceStack source) {
        if (!ConfirmationManager.confirm(adminUUID(source))) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage("inrp.admin.confirm.expired")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage("inrp.admin.confirm.success")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** Re-resolves staged targets, dropping anyone who disconnected while the confirmation was pending. */
    private static List<ServerPlayer> resolveOnline(CommandSourceStack source, List<UUID> targetIds) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> online = new ArrayList<>(targetIds.size());
        for (UUID id : targetIds) {
            ServerPlayer player = playerList.getPlayer(id);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    /** @return the executing player's UUID, or {@code null} for the console and command blocks. */
    private static UUID adminUUID(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
    }

    private static int showAdminHelp(CommandSourceStack source) {
        Component help = HelpText.build("inrp.admin.help.header", HELP_ENTRIES);
        source.sendSuccess(() -> help, false);
        return 1;
    }
}

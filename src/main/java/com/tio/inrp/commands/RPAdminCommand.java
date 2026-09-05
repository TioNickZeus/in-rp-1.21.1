package com.tio.inrp.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.data.InRPLivesManager;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.ConfirmationManager;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Collection;
import java.util.UUID;

public class RPAdminCommand {

    private static final int CONFIRMATION_THRESHOLD = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("on")
                                        .executes(context -> setMode(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                true
                                        )))
                                .then(Commands.literal("off")
                                        .executes(context -> setMode(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                false
                                        )))
                        )
                )
                .then(Commands.literal("config")
                        .then(Commands.literal("pvp")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigPvp(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("block_break")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigBlockBreak(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("block_place")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigBlockPlace(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("op_bypass")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setConfigOpBypass(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                )
                .then(Commands.literal("lives")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1, 100000))
                                                .executes(context -> setLives(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                )))))
                        .then(Commands.literal("revive")
                                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                        .executes(context -> reviveGameProfiles(
                                                context.getSource(),
                                                GameProfileArgument.getGameProfiles(context, "targets")
                                        ))))
                        .then(Commands.literal("setdeaths")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100000))
                                                .executes(context -> setDeaths(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                )))))
                        .then(Commands.literal("action")
                                .then(Commands.literal("spectator")
                                        .executes(context -> setLivesAction(context.getSource(), "spectator")))
                                .then(Commands.literal("kick")
                                        .executes(context -> setLivesAction(context.getSource(), "kick"))))
                        .then(Commands.literal("applydefault")
                                .executes(context -> applyDefaultLives(context.getSource(), null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> applyDefaultLives(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")
                                        ))))
                )
                .then(Commands.literal("confirm")
                        .executes(context -> executeConfirm(context.getSource())))
                .then(Commands.literal("help")
                        .executes(context -> showAdminHelp(context.getSource())))
        );
    }

    private static int setMode(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enable) {
        if (targets.size() >= CONFIRMATION_THRESHOLD && requiresConfirmation(source)) {
            String desc = LocalizationHelper.format("inrp.admin.confirm.desc.setmode", targets.size(), enable ? "ON" : "OFF");
            ConfirmationManager.requestConfirmation(source, getAdminUUID(source), desc, () -> executeSetMode(source, targets, enable));
            return 0;
        }
        return executeSetMode(source, targets, enable);
    }

    private static int executeSetMode(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enable) {
        int count = 0;
        for (ServerPlayer player : targets) {
            InRPAttachments.setInRP(player, enable);
            ScoreboardHandler.updatePlayerScoreboard(player);

            String playerMsgKey = enable ? "inrp.status.turned_on" : "inrp.status.turned_off";
            ChatFormatting color = enable ? ChatFormatting.GREEN : ChatFormatting.AQUA;
            player.sendSystemMessage(LocalizationHelper.getPrefixedMessage(playerMsgKey).withStyle(color));
            count++;
        }

        String statusKey = enable ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);
        final int finalCount = count;

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.set.success",
                statusComponent,
                finalCount
        ).withStyle(ChatFormatting.GOLD), true);

        return count;
    }

    private static int setConfigPvp(CommandSourceStack source, boolean value) {
        if (InRPConfig.PVP_ALLOWED_IN_RP.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.pvp.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.PVP_ALLOWED_IN_RP.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.pvp",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setConfigBlockBreak(CommandSourceStack source, boolean value) {
        if (InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.block_break.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.BLOCK_BREAK_ALLOWED_IN_RP.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.block_break",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setConfigBlockPlace(CommandSourceStack source, boolean value) {
        if (InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.block_place.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.BLOCK_PLACE_ALLOWED_IN_RP.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.block_place",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setConfigOpBypass(CommandSourceStack source, boolean value) {
        if (InRPConfig.OP_BYPASS_RESTRICTIONS.get() == value) {
            String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
            Component statusComponent = LocalizationHelper.getMessage(statusKey);
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.config.op_bypass.already",
                    statusComponent
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.OP_BYPASS_RESTRICTIONS.set(value);
        InRPConfig.SPEC.save();

        String statusKey = value ? "inrp.admin.status_on" : "inrp.admin.status_off";
        Component statusComponent = LocalizationHelper.getMessage(statusKey);

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.config.op_bypass",
                statusComponent
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setLives(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        if (targets.size() >= CONFIRMATION_THRESHOLD && requiresConfirmation(source)) {
            String desc = LocalizationHelper.format("inrp.admin.confirm.desc.setlives", targets.size(), amount);
            ConfirmationManager.requestConfirmation(source, getAdminUUID(source), desc, () -> executeSetLives(source, targets, amount));
            return 0;
        }
        return executeSetLives(source, targets, amount);
    }

    private static int executeSetLives(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        int finalAmount = (amount <= 0 && amount != -1) ? -1 : amount;
        for (ServerPlayer player : targets) {
            InRPAttachments.setMaxLives(player, finalAmount);
            if (finalAmount > 0 && InRPAttachments.getDeathCount(player) < finalAmount && InRPAttachments.isDead(player)) {
                reviveSinglePlayer(player);
            }
        }

        String amountStr = finalAmount == -1 ? LocalizationHelper.getRaw("inrp.lives.unlimited") : String.valueOf(finalAmount);
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.lives.set.success",
                amountStr,
                targets.size()
        ).withStyle(ChatFormatting.GOLD), true);

        return targets.size();
    }

    private static int reviveGameProfiles(CommandSourceStack source, Collection<GameProfile> profiles) {
        int count = 0;
        for (GameProfile profile : profiles) {
            InRPLivesManager.unmarkDead(profile.getId());
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
            if (player != null) {
                reviveSinglePlayer(player);
            }
            count++;
        }

        final int finalCount = count;
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.lives.revive.success",
                finalCount
        ).withStyle(ChatFormatting.GREEN), true);

        return count;
    }

    private static void reviveSinglePlayer(ServerPlayer player) {
        InRPAttachments.setDead(player, false);
        InRPAttachments.setDeathCount(player, 0);
        InRPLivesManager.unmarkDead(player.getUUID());
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(GameType.SURVIVAL);
        }
        ScoreboardHandler.updatePlayerScoreboard(player);
        ScoreboardHandler.refreshPlayerTabList(player);
        player.sendSystemMessage(LocalizationHelper.getPrefixedMessage("inrp.lives.revived_notification").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
    }

    private static int setDeaths(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        if (targets.size() >= CONFIRMATION_THRESHOLD && requiresConfirmation(source)) {
            String desc = LocalizationHelper.format("inrp.admin.confirm.desc.setdeaths", targets.size(), amount);
            ConfirmationManager.requestConfirmation(source, getAdminUUID(source), desc, () -> executeSetDeaths(source, targets, amount));
            return 0;
        }
        return executeSetDeaths(source, targets, amount);
    }

    private static int executeSetDeaths(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            InRPAttachments.setDeathCount(player, amount);
            if (InRPAttachments.hasLivesLimit(player) && amount >= InRPAttachments.getMaxLives(player)) {
                InRPAttachments.setDead(player, true);
                InRPLivesManager.markDead(player.getUUID());
                if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                    player.setGameMode(GameType.SPECTATOR);
                }
            } else if (InRPAttachments.isDead(player) && InRPAttachments.hasLivesLimit(player) && amount < InRPAttachments.getMaxLives(player)) {
                InRPAttachments.setDead(player, false);
                InRPLivesManager.unmarkDead(player.getUUID());
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    player.setGameMode(GameType.SURVIVAL);
                }
            }
            ScoreboardHandler.updatePlayerScoreboard(player);
            ScoreboardHandler.refreshPlayerTabList(player);
        }

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.lives.setdeaths.success",
                amount,
                targets.size()
        ).withStyle(ChatFormatting.GOLD), true);

        return targets.size();
    }

    private static int setLivesAction(CommandSourceStack source, String action) {
        if (InRPConfig.LIVES_ACTION.get().equalsIgnoreCase(action)) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.lives.action.already",
                    action
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        InRPConfig.LIVES_ACTION.set(action);
        InRPConfig.SPEC.save();

        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.lives.action.success",
                action
        ).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int applyDefaultLives(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int defaultMax = InRPConfig.DEFAULT_MAX_LIVES.get();
        if (defaultMax <= 0) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.lives.applydefault.disabled"
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        Collection<ServerPlayer> players = targets != null ? targets : source.getServer().getPlayerList().getPlayers();

        if (players.size() >= CONFIRMATION_THRESHOLD && requiresConfirmation(source)) {
            String desc = LocalizationHelper.format("inrp.admin.confirm.desc.applydefault", players.size(), defaultMax);
            final Collection<ServerPlayer> finalPlayers = players;
            ConfirmationManager.requestConfirmation(source, getAdminUUID(source), desc, () -> executeApplyDefaultLives(source, finalPlayers, defaultMax));
            return 0;
        }
        return executeApplyDefaultLives(source, players, defaultMax);
    }

    private static int executeApplyDefaultLives(CommandSourceStack source, Collection<ServerPlayer> players, int defaultMax) {
        for (ServerPlayer player : players) {
            InRPAttachments.setMaxLives(player, defaultMax);
            if (InRPAttachments.getDeathCount(player) < defaultMax && InRPAttachments.isDead(player)) {
                reviveSinglePlayer(player);
            }
        }

        String amountStr = String.valueOf(defaultMax);
        final int count = players.size();
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.lives.applydefault.success",
                amountStr,
                count
        ).withStyle(ChatFormatting.GOLD), true);

        return count;
    }

    private static int executeConfirm(CommandSourceStack source) {
        UUID adminUUID = getAdminUUID(source);
        if (adminUUID == null || !ConfirmationManager.confirm(adminUUID)) {
            source.sendFailure(LocalizationHelper.getPrefixedMessage(
                    "inrp.admin.confirm.expired"
            ).withStyle(ChatFormatting.RED));
            return 0;
        }
        source.sendSuccess(() -> LocalizationHelper.getPrefixedMessage(
                "inrp.admin.confirm.success"
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static boolean requiresConfirmation(CommandSourceStack source) {
        UUID uuid = getAdminUUID(source);
        return uuid != null && !ConfirmationManager.hasPending(uuid);
    }

    private static UUID getAdminUUID(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return null;
    }

    private static int showAdminHelp(CommandSourceStack source) {
        Component help = Component.empty()
                .append(LocalizationHelper.getPrefixedMessage("inrp.admin.help.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.set").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.config").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.lives_set").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.lives_revive").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.lives_setdeaths").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.lives_action").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.lives_applydefault").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(LocalizationHelper.getMessage("inrp.admin.help.confirm").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> help, false);
        return 1;
    }
}

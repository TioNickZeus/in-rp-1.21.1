package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class ScoreboardHandler {
    public static final String TEAM_NAME = "inrp_active";
    public static final String TEAM_AFK_NAME = "inrp_afk";

    public static void updatePlayerScoreboard(ServerPlayer player) {
        if (player == null || player.server == null) return;
        ServerScoreboard scoreboard = player.server.getScoreboard();

        // Active RP Team
        PlayerTeam teamRP = scoreboard.getPlayerTeam(TEAM_NAME);
        if (teamRP == null) {
            teamRP = scoreboard.addPlayerTeam(TEAM_NAME);
            teamRP.setDisplayName(Component.literal("In RP"));
        }
        Component suffixRP = Component.literal(" ")
                .append(LocalizationHelper.getMessage("inrp.chat.suffix").withStyle(ChatFormatting.GOLD));
        teamRP.setPlayerSuffix(suffixRP);

        // AFK Team
        PlayerTeam teamAFK = scoreboard.getPlayerTeam(TEAM_AFK_NAME);
        if (teamAFK == null) {
            teamAFK = scoreboard.addPlayerTeam(TEAM_AFK_NAME);
            teamAFK.setDisplayName(Component.literal("AFK"));
        }
        Component suffixAFK = Component.literal(" ")
                .append(LocalizationHelper.getMessage("inrp.afk.nametag.suffix").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        teamAFK.setPlayerSuffix(suffixAFK);

        boolean isAFK = InRPAttachments.isAFK(player);
        boolean inRP = InRPAttachments.isInRP(player);

        if (isAFK) {
            if (player.getTeam() == teamRP) {
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), teamRP);
            }
            if (player.getTeam() != teamAFK) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), teamAFK);
            }
        } else if (inRP) {
            if (player.getTeam() == teamAFK) {
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), teamAFK);
            }
            if (player.getTeam() != teamRP) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), teamRP);
            }
        } else {
            if (player.getTeam() == teamRP) {
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), teamRP);
            }
            if (player.getTeam() == teamAFK) {
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), teamAFK);
            }
        }

        player.refreshDisplayName();
        refreshPlayerTabList(player);
    }

    public static void refreshPlayerTabList(ServerPlayer player) {
        if (player == null || player.server == null) return;
        player.server.getPlayerList().broadcastAll(
                new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                        net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                        player
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updatePlayerScoreboard(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updatePlayerScoreboard(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updatePlayerScoreboard(player);
        }
    }
}

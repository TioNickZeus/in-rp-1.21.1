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

    public static void updatePlayerScoreboard(ServerPlayer player) {
        if (player == null || player.server == null) return;
        ServerScoreboard scoreboard = player.server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            team.setDisplayName(Component.literal("In RP"));
        }
        
        Component suffixComponent = Component.literal(" ")
                .append(LocalizationHelper.getMessage("inrp.nametag.suffix").withStyle(ChatFormatting.GRAY));
        team.setPlayerSuffix(suffixComponent);

        boolean inRP = InRPAttachments.isInRP(player);
        if (inRP) {
            if (player.getTeam() != team) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            }
        } else {
            if (player.getTeam() == team) {
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
            }
        }
        player.refreshDisplayName();
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

package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Renders roleplay and AFK markers through native scoreboard teams.
 *
 * <p>Using teams keeps the mod fully server-side: vanilla clients apply the team suffix themselves above the
 * player's head, in the tab list and in chat, with no custom packets and no risk of the marker being duplicated.
 *
 * <p>A player is on at most one In-RP team at a time, and AFK takes precedence over RP mode. Teams are created
 * lazily, so a server that never uses these features keeps a clean scoreboard.
 */
public final class ScoreboardHandler {

    /** Team carrying the roleplay suffix. */
    public static final String TEAM_NAME = "inrp_active";
    /** Team carrying the AFK suffix. */
    public static final String TEAM_AFK_NAME = "inrp_afk";

    private ScoreboardHandler() {
    }

    /** Moves the player onto the team matching their current state and pushes the new display name to clients. */
    public static void updatePlayerScoreboard(ServerPlayer player) {
        if (player == null || player.server == null) {
            return;
        }
        ServerScoreboard scoreboard = player.server.getScoreboard();

        boolean afk = InRPAttachments.isAFK(player);
        boolean inRP = !afk && InRPAttachments.isInRP(player);
        String targetTeamName = afk ? TEAM_AFK_NAME : (inRP ? TEAM_NAME : null);

        // Pick up config or language changes on teams that already exist.
        refreshSuffix(scoreboard, TEAM_NAME);
        refreshSuffix(scoreboard, TEAM_AFK_NAME);

        // Recorded before the player leaves anything: a player moving from the RP team to the AFK team is briefly
        // on no team at all, and reading it afterwards would look like they never had a foreign team to go back to.
        if (targetTeamName != null) {
            rememberForeignTeam(player);
        }

        boolean leftRPTeam = leaveTeamUnless(scoreboard, player, TEAM_NAME, targetTeamName);
        boolean leftAFKTeam = leaveTeamUnless(scoreboard, player, TEAM_AFK_NAME, targetTeamName);

        if (targetTeamName != null) {
            PlayerTeam team = getOrCreateTeam(scoreboard, targetTeamName);
            if (player.getTeam() != team) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            }
        } else if (leftRPTeam || leftAFKTeam) {
            restoreForeignTeam(scoreboard, player);
        }

        player.refreshDisplayName();
        refreshPlayerTabList(player);
    }

    /**
     * Recomputes the player's tab list entry so tags such as {@code [DEAD]} and {@code [AFK]} appear immediately.
     *
     * <p>Must go through {@link ServerPlayer#refreshTabListName()} rather than broadcasting the packet directly:
     * NeoForge fires {@link PlayerEvent.TabListNameFormat} from that method and caches the result in
     * {@code tabListDisplayName}, and the packet only serialises that cached value. Broadcasting the packet on its
     * own therefore re-sends the <em>previous</em> name, which is why a revived player kept their {@code [DEAD]}
     * tag until they reconnected. {@code refreshTabListName()} broadcasts the update itself, and only when the
     * name actually changed.
     *
     * <p>Callers must update the player's state before calling this, since the tag is derived from it.
     */
    public static void refreshPlayerTabList(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.refreshTabListName();
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

    private static PlayerTeam getOrCreateTeam(ServerScoreboard scoreboard, String teamName) {
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setDisplayName(Component.literal(TEAM_AFK_NAME.equals(teamName) ? "AFK" : "In RP"));
            applySuffix(team, suffixFor(teamName));
        }
        return team;
    }

    private static void refreshSuffix(ServerScoreboard scoreboard, String teamName) {
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team != null) {
            applySuffix(team, suffixFor(teamName));
        }
    }

    /**
     * Writing a suffix broadcasts a team update packet to every connected client, so the value is only written when
     * the rendered text actually changed.
     */
    private static void applySuffix(PlayerTeam team, Component suffix) {
        if (!suffix.equals(team.getPlayerSuffix())) {
            team.setPlayerSuffix(suffix);
        }
    }

    private static Component suffixFor(String teamName) {
        if (TEAM_AFK_NAME.equals(teamName)) {
            return Component.literal(" ").append(LocalizationHelper.getMessage("inrp.afk.nametag.suffix")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        String suffix = InRPConfig.rpSuffix();
        if (suffix.isEmpty()) {
            return Component.empty();
        }
        return Component.literal(" ").append(Component.literal(suffix).withStyle(ChatFormatting.GOLD));
    }

    /** @return whether the player was actually removed from {@code teamName}. */
    private static boolean leaveTeamUnless(ServerScoreboard scoreboard, ServerPlayer player, String teamName, String keptTeamName) {
        if (teamName.equals(keptTeamName)) {
            return false;
        }
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team != null && player.getTeam() == team) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
            return true;
        }
        return false;
    }

    /**
     * Records the team the player is leaving behind, so {@link #restoreForeignTeam} can put them back later.
     *
     * <p>Vanilla's {@code addPlayerToTeam} removes a player from their current team, so a server that uses teams
     * for rank prefixes would otherwise lose that membership permanently the first time the player entered RP mode.
     */
    private static void rememberForeignTeam(ServerPlayer player) {
        Team current = player.getTeam();
        if (current == null) {
            forgetForeignTeam(player);
            return;
        }

        // Moving between our own RP and AFK teams: keep the team we recorded when the player first entered.
        if (isOwnTeam(current.getName())) {
            return;
        }
        InRPAttachments.setPreviousTeam(player, current.getName());
    }

    /** Puts the player back on the team they held before entering RP mode, if it still exists. */
    private static void restoreForeignTeam(ServerScoreboard scoreboard, ServerPlayer player) {
        String rememberedTeam = InRPAttachments.getPreviousTeam(player);
        forgetForeignTeam(player);
        if (rememberedTeam.isEmpty()) {
            return;
        }

        PlayerTeam team = scoreboard.getPlayerTeam(rememberedTeam);
        if (team == null) {
            // Deleted while the player was in RP mode; there is nothing to go back to.
            return;
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
    }

    private static void forgetForeignTeam(ServerPlayer player) {
        if (!InRPAttachments.getPreviousTeam(player).isEmpty()) {
            InRPAttachments.setPreviousTeam(player, "");
        }
    }

    private static boolean isOwnTeam(String teamName) {
        return TEAM_NAME.equals(teamName) || TEAM_AFK_NAME.equals(teamName);
    }
}

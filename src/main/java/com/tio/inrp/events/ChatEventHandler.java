package com.tio.inrp.events;

/**
 * Formerly handled [RP] name formatting via NameFormat.
 * Roleplay and AFK name/chat suffixes are now handled cleanly and uniformly
 * by ScoreboardHandler (via Minecraft Scoreboard teams) to prevent duplicate suffixes in chat.
 */
public class ChatEventHandler {
    // Kept for backward compatibility; event registration moved to ScoreboardHandler.
}

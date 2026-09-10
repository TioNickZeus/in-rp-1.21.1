package com.tio.inrp.config;

import com.tio.inrp.InRP;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * The {@code SERVER} config spec, written to {@code <world>/serverconfig/inrp-server.toml}.
 *
 * <p>Values with a finite domain are declared with {@code defineInRange}/{@code defineInList} so NeoForge rejects
 * bad input before the mod ever reads it. Free-form strings are sanitised on read by the accessors below.
 *
 * <p>Every default preserves the behaviour of the previous release, so upgrading never silently changes a server.
 */
public final class InRPConfig {

    /** Elimination action that moves the player to spectator mode. */
    public static final String LIVES_ACTION_SPECTATOR = "spectator";
    /** Elimination action that disconnects the player until an admin revives them. */
    public static final String LIVES_ACTION_KICK = "kick";

    /** Upper bound on a roleplay suffix, so a stray config value cannot bloat every scoreboard team packet. */
    private static final int MAX_SUFFIX_LENGTH = 64;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // general
    public static final ModConfigSpec.ConfigValue<String> SERVER_LANGUAGE;
    public static final ModConfigSpec.ConfigValue<String> CHAT_SUFFIX;
    public static final ModConfigSpec.ConfigValue<String> NAMETAG_SUFFIX;

    // rules
    public static final ModConfigSpec.BooleanValue PVP_ALLOWED_IN_RP;
    public static final ModConfigSpec.BooleanValue BLOCK_BREAK_ALLOWED_IN_RP;
    public static final ModConfigSpec.BooleanValue BLOCK_PLACE_ALLOWED_IN_RP;
    public static final ModConfigSpec.BooleanValue OP_BYPASS_RESTRICTIONS;

    // roll
    public static final ModConfigSpec.IntValue ROLL_DEFAULT_SIDES;
    public static final ModConfigSpec.DoubleValue ROLL_PROXIMITY_RADIUS;

    // lives
    public static final ModConfigSpec.ConfigValue<String> LIVES_ACTION;
    public static final ModConfigSpec.IntValue DEFAULT_MAX_LIVES;
    public static final ModConfigSpec.BooleanValue COUNT_DEATHS_ONLY_IN_RP;

    // afk
    public static final ModConfigSpec.BooleanValue AFK_ENABLED;
    public static final ModConfigSpec.IntValue AFK_TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue AFK_KICK_SECONDS;
    public static final ModConfigSpec.BooleanValue AUTO_DISABLE_RP_ON_AFK;

    // chat
    public static final ModConfigSpec.BooleanValue LOCAL_CHAT_ENABLED;
    public static final ModConfigSpec.DoubleValue LOCAL_CHAT_RADIUS;
    public static final ModConfigSpec.IntValue GLOBAL_CHAT_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue SPY_LOCAL_CHAT;
    public static final ModConfigSpec.BooleanValue SPY_PRIVATE_MESSAGES;

    static {
        BUILDER.push("general");

        SERVER_LANGUAGE = BUILDER
                .comment("Server fallback language for vanilla clients (e.g. en_us, pt_br)",
                        "Must match a bundled file in assets/inrp/lang/; invalid values fall back to en_us")
                .define("serverLanguage", "en_us");

        NAMETAG_SUFFIX = BUILDER
                .comment("Roleplay suffix shown after the player name above their head, in the tab list and in chat",
                        "In-RP marks players with a native scoreboard team, which carries a single suffix, so the",
                        "same text is used in all three places. Leave empty to disable the marker entirely.")
                .define("nametagSuffix", " [in RP]");

        CHAT_SUFFIX = BUILDER
                .comment("Fallback for nametagSuffix, used only when nametagSuffix is empty",
                        "Kept for compatibility with configs written before the suffixes were unified.")
                .define("chatSuffix", "[RP]");

        BUILDER.pop();

        BUILDER.push("rules");

        PVP_ALLOWED_IN_RP = BUILDER
                .comment("Whether PvP is allowed between or against players in RP mode",
                        "Covers melee attacks as well as indirect damage such as arrows, thrown potions and TNT")
                .define("pvpAllowedInRP", true);

        BLOCK_BREAK_ALLOWED_IN_RP = BUILDER
                .comment("Whether players in RP mode can break blocks")
                .define("blockBreakAllowedInRP", true);

        BLOCK_PLACE_ALLOWED_IN_RP = BUILDER
                .comment("Whether players in RP mode can place blocks")
                .define("blockPlaceAllowedInRP", true);

        OP_BYPASS_RESTRICTIONS = BUILDER
                .comment("Whether operators/staff (OP level 2+) bypass RP restrictions (block break, block place, PvP)")
                .define("opBypassRestrictions", true);

        BUILDER.pop();

        BUILDER.push("roll");

        ROLL_DEFAULT_SIDES = BUILDER
                .comment("Default number of sides for /roll when no arguments are given")
                .defineInRange("rollDefaultSides", 20, 2, 10000);

        ROLL_PROXIMITY_RADIUS = BUILDER
                .comment("Radius in blocks to hear /roll results. Set to -1.0 for global broadcast.")
                .defineInRange("rollProximityRadius", 30.0, -1.0, 1000.0);

        BUILDER.pop();

        BUILDER.push("lives");

        LIVES_ACTION = BUILDER
                .comment("Action taken when a player loses all lives (spectator or kick)")
                .defineInList("livesAction", LIVES_ACTION_SPECTATOR, List.of(LIVES_ACTION_SPECTATOR, LIVES_ACTION_KICK));

        DEFAULT_MAX_LIVES = BUILDER
                .comment("Default max lives for players (-1 for unlimited/disabled)")
                .defineInRange("defaultMaxLives", -1, -1, 100000);

        COUNT_DEATHS_ONLY_IN_RP = BUILDER
                .comment("If true, only deaths while in RP mode count toward the lives system")
                .define("countDeathsOnlyInRP", false);

        BUILDER.pop();

        BUILDER.push("afk");

        AFK_ENABLED = BUILDER
                .comment("Enable or disable the AFK (inactivity) system")
                .define("afkEnabled", true);

        AFK_TIMEOUT_SECONDS = BUILDER
                .comment("Idle time in seconds before a player is automatically marked as AFK (default: 300 = 5 minutes)")
                .defineInRange("afkTimeoutSeconds", 300, 10, 86400);

        AFK_KICK_SECONDS = BUILDER
                .comment("Idle time in seconds before an AFK player is kicked (-1 to disable kick)",
                        "Only players already marked as AFK are kicked, so values below afkTimeoutSeconds",
                        "behave as if they were equal to afkTimeoutSeconds.")
                .defineInRange("afkKickSeconds", -1, -1, 86400);

        AUTO_DISABLE_RP_ON_AFK = BUILDER
                .comment("If true, entering AFK mode automatically disables RP mode")
                .define("autoDisableRPOnAFK", true);

        BUILDER.pop();

        BUILDER.push("chat");

        LOCAL_CHAT_ENABLED = BUILDER
                .comment("Whether standard chat is converted into proximity local chat")
                .define("localChatEnabled", true);

        LOCAL_CHAT_RADIUS = BUILDER
                .comment("Proximity radius in blocks for local chat")
                .defineInRange("localChatRadius", 40.0, 5.0, 500.0);

        GLOBAL_CHAT_COOLDOWN_SECONDS = BUILDER
                .comment("Cooldown in seconds between messages in /g or /global (0 to disable)")
                .defineInRange("globalChatCooldownSeconds", 3, 0, 300);

        SPY_LOCAL_CHAT = BUILDER
                .comment("Whether staff with Chat Spy active receive out-of-range local chat")
                .define("spyLocalChat", true);

        SPY_PRIVATE_MESSAGES = BUILDER
                .comment("Whether staff with Chat Spy active receive copies of private messages (/tell, /msg, /w)")
                .define("spyPrivateMessages", false);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private InRPConfig() {
    }

    /**
     * @return the roleplay suffix to display, or an empty string when the marker is disabled. Surrounding
     *         whitespace is stripped; renderers add their own separator.
     */
    public static String rpSuffix() {
        String suffix = sanitizeSuffix(NAMETAG_SUFFIX.get());
        return suffix.isEmpty() ? sanitizeSuffix(CHAT_SUFFIX.get()) : suffix;
    }

    /** @return the idle time, in milliseconds, after which a player is marked AFK. */
    public static long afkTimeoutMillis() {
        return AFK_TIMEOUT_SECONDS.get() * 1000L;
    }

    /**
     * @return the idle time, in milliseconds, after which an AFK player is disconnected, or {@code -1} when the
     *         idle kick is disabled. Never returns a value below the AFK timeout, since only players who are
     *         already marked AFK can be kicked.
     */
    public static long afkKickMillis() {
        int seconds = AFK_KICK_SECONDS.get();
        if (seconds <= 0) {
            return -1L;
        }
        return Math.max(seconds * 1000L, afkTimeoutMillis());
    }

    /** @return the squared local chat radius, ready for {@code distanceToSqr} comparisons. */
    public static double localChatRadiusSq() {
        double radius = LOCAL_CHAT_RADIUS.get();
        return radius * radius;
    }

    /** @return whether eliminated players are disconnected instead of moved to spectator mode. */
    public static boolean eliminatesByKick() {
        return LIVES_ACTION_KICK.equalsIgnoreCase(LIVES_ACTION.get());
    }

    /** Logs a warning for combinations that are valid but almost certainly not what the operator intended. */
    public static void logSuspiciousValues() {
        int kickSeconds = AFK_KICK_SECONDS.get();
        int timeoutSeconds = AFK_TIMEOUT_SECONDS.get();
        if (kickSeconds > 0 && kickSeconds < timeoutSeconds) {
            InRP.LOGGER.warn("afkKickSeconds ({}) is below afkTimeoutSeconds ({}); AFK players will be kicked as"
                    + " soon as they are marked AFK", kickSeconds, timeoutSeconds);
        }
        if (rpSuffix().isEmpty()) {
            InRP.LOGGER.info("Roleplay suffix is disabled; players in RP mode will not carry a visible marker");
        }
        if (isOversized(NAMETAG_SUFFIX.get()) || isOversized(CHAT_SUFFIX.get())) {
            InRP.LOGGER.warn("A roleplay suffix is longer than {} characters and will be truncated", MAX_SUFFIX_LENGTH);
        }
    }

    /**
     * Truncation is silent here on purpose: this runs on every scoreboard refresh, and
     * {@link #logSuspiciousValues()} already reports an oversized suffix once at startup.
     */
    private static String sanitizeSuffix(String configured) {
        if (configured == null) {
            return "";
        }
        String suffix = configured.strip();
        return isOversized(suffix) ? suffix.substring(0, MAX_SUFFIX_LENGTH) : suffix;
    }

    private static boolean isOversized(String suffix) {
        return suffix != null && suffix.strip().length() > MAX_SUFFIX_LENGTH;
    }
}

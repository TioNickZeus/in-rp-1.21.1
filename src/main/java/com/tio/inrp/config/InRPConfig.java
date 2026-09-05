package com.tio.inrp.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public class InRPConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> SERVER_LANGUAGE;
    public static final ForgeConfigSpec.BooleanValue PVP_ALLOWED_IN_RP;
    public static final ForgeConfigSpec.BooleanValue BLOCK_BREAK_ALLOWED_IN_RP;
    public static final ForgeConfigSpec.BooleanValue BLOCK_PLACE_ALLOWED_IN_RP;
    public static final ForgeConfigSpec.BooleanValue OP_BYPASS_RESTRICTIONS;
    public static final ForgeConfigSpec.IntValue ROLL_DEFAULT_SIDES;
    public static final ForgeConfigSpec.DoubleValue ROLL_PROXIMITY_RADIUS;
    public static final ForgeConfigSpec.ConfigValue<String> CHAT_SUFFIX;
    public static final ForgeConfigSpec.ConfigValue<String> NAMETAG_SUFFIX;
    public static final ForgeConfigSpec.ConfigValue<String> LIVES_ACTION;
    public static final ForgeConfigSpec.IntValue DEFAULT_MAX_LIVES;
    public static final ForgeConfigSpec.BooleanValue COUNT_DEATHS_ONLY_IN_RP;

    static {
        BUILDER.push("general");

        SERVER_LANGUAGE = BUILDER
                .comment("Server fallback language for vanilla clients (e.g. en_us, pt_br)")
                .define("serverLanguage", "en_us");

        CHAT_SUFFIX = BUILDER
                .comment("Suffix displayed after player name in chat when in RP mode")
                .define("chatSuffix", "[RP]");

        NAMETAG_SUFFIX = BUILDER
                .comment("Suffix displayed in player nametag above head and tablist when in RP mode")
                .define("nametagSuffix", " [in RP]");

        BUILDER.pop();

        BUILDER.push("rules");

        PVP_ALLOWED_IN_RP = BUILDER
                .comment("Whether PvP is allowed between or against players in RP mode")
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
                .comment("Action taken when a player loses all lives ('spectator' or 'kick')")
                .defineInList("livesAction", "spectator", List.of("spectator", "kick"));

        DEFAULT_MAX_LIVES = BUILDER
                .comment("Default max lives for players (-1 for unlimited/disabled)")
                .defineInRange("defaultMaxLives", -1, -1, 100000);

        COUNT_DEATHS_ONLY_IN_RP = BUILDER
                .comment("If true, only deaths while in RP mode count toward the lives system")
                .define("countDeathsOnlyInRP", false);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}

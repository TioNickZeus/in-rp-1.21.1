package com.tio.inrp.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class InRPConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> SERVER_LANGUAGE;
    public static final ModConfigSpec.BooleanValue PVP_ALLOWED_IN_RP;
    public static final ModConfigSpec.BooleanValue BLOCK_BREAK_ALLOWED_IN_RP;
    public static final ModConfigSpec.BooleanValue BLOCK_PLACE_ALLOWED_IN_RP;
    public static final ModConfigSpec.BooleanValue OP_BYPASS_RESTRICTIONS;
    public static final ModConfigSpec.IntValue ROLL_DEFAULT_SIDES;
    public static final ModConfigSpec.DoubleValue ROLL_PROXIMITY_RADIUS;
    public static final ModConfigSpec.ConfigValue<String> CHAT_SUFFIX;
    public static final ModConfigSpec.ConfigValue<String> NAMETAG_SUFFIX;

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
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}

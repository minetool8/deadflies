package com.deadflies;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class DeadFliesConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue MIN_FLIES;
    public static final ForgeConfigSpec.IntValue MAX_FLIES;
    public static final ForgeConfigSpec.DoubleValue FLY_SCALE;
    public static final ForgeConfigSpec.IntValue ACTIVE_DISTANCE;
    public static final ForgeConfigSpec.IntValue MERGE_RADIUS;
    public static final ForgeConfigSpec.IntValue BODY_HEIGHT_PIXELS;

    public static final ForgeConfigSpec.BooleanValue SOUND_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.IntValue MAX_SOUNDS;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NAMESPACES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> KEYWORDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_BLOCKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("flies");
        ENABLED = b.comment("Turn the whole mod on/off.").define("enabled", true);
        MIN_FLIES = b.comment("Minimum flies per body.").defineInRange("minFlies", 10, 1, 100);
        MAX_FLIES = b.comment("Maximum flies per body.").defineInRange("maxFlies", 20, 1, 100);
        FLY_SCALE = b.comment("Size multiplier. 1.0 = 2 pixels long, 1 pixel high.")
                .defineInRange("flyScale", 1.0, 0.25, 4.0);
        ACTIVE_DISTANCE = b.comment("Bodies farther than this (blocks) from you get no flies.")
                .defineInRange("activeDistance", 48, 16, 128);
        MERGE_RADIUS = b.comment("Body blocks closer than this (blocks) share one swarm (for multi-block bodies).")
                .defineInRange("mergeRadius", 2, 0, 8);
        BODY_HEIGHT_PIXELS = b.comment("Visual height of a body in pixels (16 = full block). Flies land on and hover above this height,",
                        "not above the block's full-size hitbox.")
                .defineInRange("bodyHeightPixels", 4, 1, 16);
        b.pop();

        b.push("sound");
        SOUND_ENABLED = b.comment("Play the buzzing sound near swarms.").define("enabled", true);
        SOUND_VOLUME = b.comment("Buzz volume (0-1). Also scaled by your Ambient volume slider.")
                .defineInRange("volume", 0.6, 0.0, 1.0);
        MAX_SOUNDS = b.comment("Max swarms buzzing at the same time (nearest ones win).")
                .defineInRange("maxSimultaneous", 3, 1, 16);
        b.pop();

        b.push("bodyDetection");
        NAMESPACES = b.comment("Only blocks from these mods are checked against the keywords.")
                .defineListAllowEmpty("namespaces", List.of("doomsday_decoration"), o -> o instanceof String);
        KEYWORDS = b.comment("A block from those mods counts as a body if its id path contains one of these.",
                        "Use /deadflies matches in game to see what matched, /deadflies find <text> to search.")
                .defineListAllowEmpty("keywords",
                        List.of("remains", "corpse", "cadaver", "deadbody", "dead_body", "human_body", "humanbody"),
                        o -> o instanceof String);
        EXTRA_BLOCKS = b.comment("Exact block ids that always count as bodies, e.g. \"doomsday_decoration:corpse_1\".")
                .defineListAllowEmpty("extraBlocks", List.of(), o -> o instanceof String);
        EXCLUDED_BLOCKS = b.comment("Exact block ids that never count as bodies.")
                .defineListAllowEmpty("excludedBlocks", List.of(), o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    private DeadFliesConfig() {}
}

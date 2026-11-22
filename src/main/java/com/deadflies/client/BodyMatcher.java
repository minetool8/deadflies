package com.deadflies.client;

import com.deadflies.DeadFliesConfig;
import com.deadflies.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Decides whether a block is a "dead body" that should attract flies. */
public final class BodyMatcher {
    private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();

    private BodyMatcher() {}

    public static boolean isBody(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        return CACHE.computeIfAbsent(state.getBlock(), block -> compute(block));
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static boolean compute(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) {
            return false;
        }
        String full = id.toString();

        if (contains(DeadFliesConfig.EXCLUDED_BLOCKS.get(), full)) {
            return false;
        }
        if (contains(DeadFliesConfig.EXTRA_BLOCKS.get(), full)) {
            return true;
        }
        if (block.defaultBlockState().is(ModTags.BODIES)) {
            return true;
        }
        if (!contains(DeadFliesConfig.NAMESPACES.get(), id.getNamespace())) {
            return false;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (String keyword : DeadFliesConfig.KEYWORDS.get()) {
            if (!keyword.isEmpty() && path.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(List<? extends String> list, String value) {
        for (String s : list) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}

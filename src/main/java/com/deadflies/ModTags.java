package com.deadflies;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Add block ids to this tag with a datapack to make them attract flies. */
    public static final TagKey<Block> BODIES = BlockTags.create(new ResourceLocation(DeadFliesMod.MODID, "bodies"));

    private ModTags() {}
}

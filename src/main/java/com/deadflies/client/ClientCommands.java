package com.deadflies.client;

import com.deadflies.DeadFliesConfig;
import com.deadflies.DeadFliesMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client-side helper commands:
 *   /deadflies status        - how many swarms/flies are active
 *   /deadflies matches       - block ids currently treated as bodies
 *   /deadflies find <text>   - search block ids of the configured mods
 *   /deadflies test          - spawn a test swarm at your feet (no body needed)
 *   /deadflies cleartest     - remove test swarms
 */
@Mod.EventBusSubscriber(modid = DeadFliesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientCommands {
    private static final int MAX_LINES = 40;

    private ClientCommands() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("deadflies")
                .then(Commands.literal("status").executes(ClientCommands::status))
                .then(Commands.literal("matches").executes(ClientCommands::matches))
                .then(Commands.literal("find")
                        .then(Commands.argument("text", StringArgumentType.word()).executes(ClientCommands::find)))
                .then(Commands.literal("test").executes(ClientCommands::test))
                .then(Commands.literal("cleartest").executes(ClientCommands::clearTest)));
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        say(ctx, "Dead Flies: " + SwarmManager.swarmCount() + " swarms, " + SwarmManager.flyCount() + " flies active.");
        return 1;
    }

    private static int matches(CommandContext<CommandSourceStack> ctx) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<Block>, Block> e : ForgeRegistries.BLOCKS.getEntries()) {
            if (BodyMatcher.isBody(e.getValue().defaultBlockState())) {
                ids.add(e.getKey().location().toString());
            }
        }
        ids.sort(String::compareTo);
        say(ctx, "Blocks treated as bodies: " + ids.size() + " (full list in latest.log)");
        DeadFliesMod.LOGGER.info("Dead Flies body blocks ({}): {}", ids.size(), ids);
        for (int i = 0; i < Math.min(MAX_LINES, ids.size()); i++) {
            say(ctx, " - " + ids.get(i));
        }
        if (ids.size() > MAX_LINES) {
            say(ctx, " ... and " + (ids.size() - MAX_LINES) + " more");
        }
        return ids.size();
    }

    private static int find(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text").toLowerCase(Locale.ROOT);
        List<String> namespaces = new ArrayList<>(DeadFliesConfig.NAMESPACES.get());
        List<String> hits = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            if (namespaces.contains(id.getNamespace()) && id.getPath().toLowerCase(Locale.ROOT).contains(text)) {
                hits.add(id.toString());
            }
        }
        hits.sort(String::compareTo);
        say(ctx, "Blocks containing \"" + text + "\": " + hits.size());
        DeadFliesMod.LOGGER.info("Dead Flies find '{}': {}", text, hits);
        for (int i = 0; i < Math.min(MAX_LINES, hits.size()); i++) {
            say(ctx, " - " + hits.get(i));
        }
        if (hits.size() > MAX_LINES) {
            say(ctx, " ... and " + (hits.size() - MAX_LINES) + " more (see latest.log)");
        }
        return hits.size();
    }

    private static int test(CommandContext<CommandSourceStack> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return 0;
        }
        SwarmManager.spawnTest(mc.level, mc.player.blockPosition());
        say(ctx, "Spawned a test swarm at your feet. Use /deadflies cleartest to remove it.");
        return 1;
    }

    private static int clearTest(CommandContext<CommandSourceStack> ctx) {
        SwarmManager.clearTests();
        say(ctx, "Test swarms removed.");
        return 1;
    }
}

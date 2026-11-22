package com.deadflies.client;

import com.deadflies.DeadFliesConfig;
import com.deadflies.DeadFliesMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds body blocks near the player (a few chunks per second, round robin), keeps one swarm per body,
 * and handles the buzzing sound for the nearest swarms.
 */
@Mod.EventBusSubscriber(modid = DeadFliesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwarmManager {
    private static final Map<Long, FlySwarm> SWARMS = new LinkedHashMap<>();
    private static final ArrayDeque<ChunkPos> SCAN_QUEUE = new ArrayDeque<>();
    private static final double SOUND_START_RANGE = 16.0;
    private static final double SOUND_STOP_RANGE = 22.0;

    private static ClientLevel lastLevel;
    private static int tickCounter;

    private SwarmManager() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            clear();
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {
            clear();
            lastLevel = level;
        }
        if (mc.isPaused()) {
            return;
        }
        if (!DeadFliesConfig.ENABLED.get()) {
            clear();
            return;
        }

        tickCounter++;
        double active = DeadFliesConfig.ACTIVE_DISTANCE.get();

        scanStep(level, player, active);

        Iterator<FlySwarm> it = SWARMS.values().iterator();
        while (it.hasNext()) {
            FlySwarm swarm = it.next();
            if (!swarm.forced && (swarm.age % 20 == 0 || swarm.age == 0)) {
                double distSq = player.distanceToSqr(swarm.center.x, swarm.center.y, swarm.center.z);
                BlockState state = level.getBlockState(swarm.pos);
                if (!level.hasChunkAt(swarm.pos) || !BodyMatcher.isBody(state) || distSq > (active + 16.0) * (active + 16.0)) {
                    swarm.remove();
                    it.remove();
                    continue;
                }
            }
            swarm.tick();
        }

        if (tickCounter % 10 == 0) {
            updateSounds(mc, player);
        }
    }

    // ---------------------------------------------------------------- scanning

    private static void scanStep(ClientLevel level, Player player, double active) {
        if (SCAN_QUEUE.isEmpty()) {
            int radius = (int) Math.ceil(active / 16.0);
            int pcx = player.blockPosition().getX() >> 4;
            int pcz = player.blockPosition().getZ() >> 4;
            List<ChunkPos> chunks = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius + 1) {
                        chunks.add(new ChunkPos(pcx + dx, pcz + dz));
                    }
                }
            }
            chunks.sort(Comparator.comparingInt(c -> (c.x - pcx) * (c.x - pcx) + (c.z - pcz) * (c.z - pcz)));
            SCAN_QUEUE.addAll(chunks);
        }

        // 2 chunks per tick keeps the cost tiny: sections without bodies are skipped via the palette
        for (int i = 0; i < 2 && !SCAN_QUEUE.isEmpty(); i++) {
            ChunkPos cp = SCAN_QUEUE.poll();
            LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
            if (chunk != null) {
                scanChunk(level, player, chunk, active);
            }
        }
    }

    private static void scanChunk(ClientLevel level, Player player, LevelChunk chunk, double active) {
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = level.getMinSection();
        ChunkPos cp = chunk.getPos();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(BodyMatcher::isBody)) {
                continue;
            }
            int baseY = (minSection + i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (BodyMatcher.isBody(section.getBlockState(x, y, z))) {
                            tryAdd(level, player, new BlockPos(cp.getMinBlockX() + x, baseY + y, cp.getMinBlockZ() + z), active);
                        }
                    }
                }
            }
        }
    }

    private static void tryAdd(ClientLevel level, Player player, BlockPos pos, double active) {
        if (SWARMS.containsKey(pos.asLong())) {
            return;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > active * active) {
            return;
        }
        double merge = DeadFliesConfig.MERGE_RADIUS.get();
        double mergeSq = merge * merge;
        for (FlySwarm other : SWARMS.values()) {
            if (other.pos.distSqr(pos) <= mergeSq) {
                return; // part of an existing (multi-block) body
            }
        }
        SWARMS.put(pos.asLong(), new FlySwarm(level, pos, false));
    }

    // ---------------------------------------------------------------- sound

    private static void updateSounds(Minecraft mc, Player player) {
        boolean enabled = DeadFliesConfig.SOUND_ENABLED.get();
        List<FlySwarm> sorted = new ArrayList<>(SWARMS.values());
        sorted.sort(Comparator.comparingDouble(s -> player.distanceToSqr(s.center.x, s.center.y, s.center.z)));

        int allowed = DeadFliesConfig.MAX_SOUNDS.get();
        int used = 0;
        for (FlySwarm swarm : sorted) {
            double dist = Math.sqrt(player.distanceToSqr(swarm.center.x, swarm.center.y, swarm.center.z));
            boolean playing = swarm.sound != null && !swarm.sound.isStopped();
            boolean want = enabled && used < allowed && dist <= (playing ? SOUND_STOP_RANGE : SOUND_START_RANGE);
            if (want) {
                used++;
            }
            swarm.wantsSound = want;
            if (want && !playing) {
                swarm.sound = new SwarmSound(swarm);
                mc.getSoundManager().play(swarm.sound);
            } else if (!want && swarm.sound != null && swarm.sound.isStopped()) {
                swarm.sound = null;
            }
        }
    }

    // ---------------------------------------------------------------- events / helpers

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        lastLevel = null;
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        BodyMatcher.clearCache();
    }

    public static void clear() {
        for (FlySwarm swarm : SWARMS.values()) {
            swarm.remove();
        }
        SWARMS.clear();
        SCAN_QUEUE.clear();
    }

    /** Debug: spawn a swarm at the player's position that is not tied to any block. */
    public static void spawnTest(ClientLevel level, BlockPos pos) {
        FlySwarm swarm = new FlySwarm(level, pos, true);
        SWARMS.put(pos.asLong() ^ 0x5DEECE66DL, swarm);
    }

    public static void clearTests() {
        Iterator<FlySwarm> it = SWARMS.values().iterator();
        while (it.hasNext()) {
            FlySwarm swarm = it.next();
            if (swarm.forced) {
                swarm.remove();
                it.remove();
            }
        }
    }

    public static int swarmCount() {
        return SWARMS.size();
    }

    public static int flyCount() {
        int n = 0;
        for (FlySwarm swarm : SWARMS.values()) {
            n += swarm.flyCount();
        }
        return n;
    }
}

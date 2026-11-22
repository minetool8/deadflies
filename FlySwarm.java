package com.deadflies.client;

import com.deadflies.DeadFliesConfig;
import com.deadflies.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A group of 10-20 fly particles bound to one dead body. */
public final class FlySwarm {
    final BlockPos pos;
    final boolean forced; // test swarms are not tied to a block
    final Vec3 center;
    final double topY;
    final double bottomY;
    final double halfX;
    final double halfZ;
    final double radiusMin;
    final double radiusMax;
    final int count;

    private final List<FlyParticle> flies = new ArrayList<>();
    private boolean removed;
    private int spawnCooldown;
    int age;

    SwarmSound sound;
    boolean wantsSound;

    FlySwarm(ClientLevel level, BlockPos pos, boolean forced) {
        this.pos = pos.immutable();
        this.forced = forced;

        double minX = 0.0, minY = 0.0, minZ = 0.0, maxX = 1.0, maxY = 0.4, maxZ = 1.0;
        if (!forced) {
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getShape(level, pos);
            if (!shape.isEmpty()) {
                AABB b = shape.bounds();
                minX = b.minX; minY = b.minY; minZ = b.minZ;
                maxX = b.maxX; maxY = b.maxY; maxZ = b.maxZ;
            }
        }
        // the hitbox is a full block but the body is only a few pixels tall: use that height instead
        maxY = minY + DeadFliesConfig.BODY_HEIGHT_PIXELS.get() / 16.0;

        this.center = new Vec3(pos.getX() + (minX + maxX) / 2.0, pos.getY() + (minY + maxY) / 2.0, pos.getZ() + (minZ + maxZ) / 2.0);
        this.topY = pos.getY() + maxY;
        this.bottomY = pos.getY() + minY;
        this.halfX = (maxX - minX) / 2.0;
        this.halfZ = (maxZ - minZ) / 2.0;
        this.radiusMin = 0.35;
        this.radiusMax = Math.max(1.2, Math.max(halfX, halfZ) + 0.7);

        int min = Math.min(DeadFliesConfig.MIN_FLIES.get(), DeadFliesConfig.MAX_FLIES.get());
        int max = Math.max(DeadFliesConfig.MIN_FLIES.get(), DeadFliesConfig.MAX_FLIES.get());
        // seeded by position so a body always gets the same fly count
        this.count = min + new Random(pos.asLong() * 31L + 7L).nextInt(max - min + 1);
        this.spawnCooldown = 0;
    }

    boolean isRemoved() {
        return removed;
    }

    void remove() {
        removed = true;
        wantsSound = false;
        for (FlyParticle fly : flies) {
            fly.remove();
        }
        flies.clear();
    }

    void tick() {
        age++;
        flies.removeIf(f -> !f.isAlive());

        if (flies.size() < count && --spawnCooldown <= 0) {
            spawnCooldown = 2; // trickle in so they do not all pop in at once
            spawnFly();
        }
    }

    private void spawnFly() {
        Minecraft mc = Minecraft.getInstance();
        double angle = Math.random() * Math.PI * 2.0;
        double dist = Math.random() * radiusMax * 0.7;
        double sx = center.x + Math.cos(angle) * dist;
        double sz = center.z + Math.sin(angle) * dist;
        double sy = topY + 0.1 + Math.random() * 0.8;
        Particle particle = mc.particleEngine.createParticle(ModParticles.FLY.get(), sx, sy, sz, 0.0, 0.0, 0.0);
        if (particle instanceof FlyParticle fly) {
            fly.bind(this);
            flies.add(fly);
        }
    }

    /** Average position of this swarm's flies; used as the buzz sound source. */
    Vec3 centroid() {
        if (flies.isEmpty()) {
            return new Vec3(center.x, topY + 0.4, center.z);
        }
        double x = 0, y = 0, z = 0;
        for (FlyParticle f : flies) {
            x += f.posX();
            y += f.posY();
            z += f.posZ();
        }
        int n = flies.size();
        return new Vec3(x / n, y / n, z / n);
    }

    int flyCount() {
        return flies.size();
    }
}

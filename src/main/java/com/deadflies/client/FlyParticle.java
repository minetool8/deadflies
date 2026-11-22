package com.deadflies.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.deadflies.DeadFliesConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One fly. 2 pixels long, 1 pixel high (a block is 16 pixels), ignores collisions.
 * Flies move with a small steering model so they curve and dart instead of moving on rails:
 * LOOP  - chase a point that circles over the body (wobbly orbits)
 * DART  - fast straight-ish burst to a random point near the body
 * HOVER - slow jittery hang in the air
 * LAND  - settle on the body, crawl a little, then take off again
 */
public class FlyParticle extends TextureSheetParticle {
    private static final float HALF_LENGTH = 1.0f / 16.0f;   // 2 px long -> half = 1 px
    private static final float HALF_HEIGHT = 0.5f / 16.0f;   // 1 px high -> half = 0.5 px
    private static final double MAX_SPEED = 0.38;
    private static final double TAU = Math.PI * 2.0;

    private enum Mode { LOOP, DART, HOVER, LAND }

    private final SpriteSet sprites;
    private FlySwarm swarm;

    private Mode mode = Mode.HOVER;
    private int modeTicks;
    private boolean landed;
    private double crawlAngle;

    // personality
    private float baseSpeed;
    private float maxAccel;
    private float wander;

    // loop parameters
    private double phase, omega, orbitRx, orbitRz, tilt, bobAmp, bobFreq, baseHeight, offX, offZ;

    private Vec3 target = Vec3.ZERO;
    private double vx, vy, vz;
    private float renderAngle;

    protected FlyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.lifetime = Integer.MAX_VALUE; // lives until its swarm is removed
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.setSprite(sprites.get(this.random));
    }

    double posX() { return this.x; }
    double posY() { return this.y; }
    double posZ() { return this.z; }

    void bind(FlySwarm swarm) {
        this.swarm = swarm;
        this.baseSpeed = 0.10F + this.random.nextFloat() * 0.10F;
        this.maxAccel = 0.030F + this.random.nextFloat() * 0.030F;
        this.wander = 0.010F + this.random.nextFloat() * 0.014F;
        this.renderAngle = this.random.nextFloat() * Mth.PI;
        this.crawlAngle = this.random.nextDouble() * TAU;
        this.vx = (this.random.nextDouble() - 0.5) * 0.1;
        this.vy = (this.random.nextDouble() - 0.5) * 0.1;
        this.vz = (this.random.nextDouble() - 0.5) * 0.1;
        pickNextMode();
    }

    // ------------------------------------------------------------------ behaviour

    private void pickNextMode() {
        Mode previous = mode;
        landed = false;
        if (previous == Mode.LAND) {
            startDart();
            return;
        }
        float r = random.nextFloat();
        if (r < 0.50F) {
            startLoop();
        } else if (r < 0.78F) {
            startDart();
        } else if (r < 0.90F) {
            startHover();
        } else {
            startLand();
        }
    }

    private void startLoop() {
        mode = Mode.LOOP;
        modeTicks = 40 + random.nextInt(80);
        phase = random.nextDouble() * TAU;
        omega = (0.07 + random.nextDouble() * 0.15) * (random.nextBoolean() ? 1 : -1);
        double span = swarm.radiusMax - swarm.radiusMin;
        orbitRx = swarm.radiusMin + random.nextDouble() * span;
        orbitRz = orbitRx * (0.6 + random.nextDouble() * 0.6);
        tilt = random.nextDouble() * TAU;
        bobAmp = 0.08 + random.nextDouble() * 0.30;
        bobFreq = 1 + random.nextInt(3);
        baseHeight = 0.12 + random.nextDouble() * 0.75;
        offX = (random.nextDouble() - 0.5) * 0.8;
        offZ = (random.nextDouble() - 0.5) * 0.8;
        updateLoopTarget(false);
    }

    private void updateLoopTarget(boolean advance) {
        if (advance) {
            phase += omega;
        }
        double lx = Math.cos(phase) * orbitRx;
        double lz = Math.sin(phase) * orbitRz;
        double rx = lx * Math.cos(tilt) - lz * Math.sin(tilt);
        double rz = lx * Math.sin(tilt) + lz * Math.cos(tilt);
        double ty = swarm.topY + baseHeight + Math.sin(phase * bobFreq) * bobAmp;
        ty = Math.max(ty, swarm.bottomY + 0.05);
        target = new Vec3(swarm.center.x + offX + rx, ty, swarm.center.z + offZ + rz);
    }

    private void startDart() {
        mode = Mode.DART;
        modeTicks = 20 + random.nextInt(20);
        double radius = swarm.radiusMax * (0.5 + random.nextDouble() * 0.9);
        double angle = random.nextDouble() * TAU;
        double dist = Math.sqrt(random.nextDouble()) * radius;
        target = new Vec3(
                swarm.center.x + Math.cos(angle) * dist,
                swarm.topY + 0.08 + random.nextDouble() * 1.1,
                swarm.center.z + Math.sin(angle) * dist);
    }

    private void startHover() {
        mode = Mode.HOVER;
        modeTicks = 10 + random.nextInt(25);
        target = new Vec3(x, y, z);
    }

    private void startLand() {
        mode = Mode.LAND;
        modeTicks = 70; // approach timeout; replaced by a rest time on arrival
        double tx = swarm.center.x + (random.nextDouble() * 2 - 1) * Math.max(0.2, swarm.halfX);
        double tz = swarm.center.z + (random.nextDouble() * 2 - 1) * Math.max(0.2, swarm.halfZ);
        target = new Vec3(tx, swarm.topY + 0.01, tz);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (swarm == null || swarm.isRemoved()) {
            this.remove();
            return;
        }

        // wing flicker: constant while flying, rare twitch while resting
        if (!landed || random.nextInt(12) == 0) {
            this.setSprite(sprites.get(this.random));
        }

        if (--modeTicks <= 0) {
            pickNextMode();
        }

        // stay near the body no matter what
        double dxc = x - swarm.center.x;
        double dzc = z - swarm.center.z;
        double dyc = y - swarm.topY;
        double leash = swarm.radiusMax + 0.8;
        if (!landed && dxc * dxc + dzc * dzc + dyc * dyc > leash * leash) {
            startDart();
            target = new Vec3(swarm.center.x, swarm.topY + 0.3, swarm.center.z);
        }

        if (landed) {
            tickLanded();
            return;
        }

        double speed;
        switch (mode) {
            case LOOP -> {
                updateLoopTarget(true);
                speed = baseSpeed;
            }
            case DART -> speed = baseSpeed * 1.9;
            case HOVER -> speed = 0.025;
            default -> speed = baseSpeed * 0.6; // LAND approach
        }

        double tx = target.x - x;
        double ty = target.y - y;
        double tz = target.z - z;
        double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);

        if (mode == Mode.DART && dist < 0.15) {
            pickNextMode();
        } else if (mode == Mode.LAND && dist < 0.05) {
            landed = true;
            modeTicks = 25 + random.nextInt(70);
            vx = vy = vz = 0.0;
            this.setPos(target.x, target.y, target.z);
            return;
        }

        double desired = speed;
        if (mode != Mode.LOOP && dist < 0.4) {
            desired *= Math.max(0.15, dist / 0.4);
        }

        double dvx = 0, dvy = 0, dvz = 0;
        if (dist > 1.0E-6) {
            dvx = tx / dist * desired - vx;
            dvy = ty / dist * desired - vy;
            dvz = tz / dist * desired - vz;
        }
        double accel = Math.sqrt(dvx * dvx + dvy * dvy + dvz * dvz);
        if (accel > maxAccel) {
            double k = maxAccel / accel;
            dvx *= k;
            dvy *= k;
            dvz *= k;
        }

        vx += dvx + (random.nextFloat() - 0.5F) * wander;
        vy += dvy + (random.nextFloat() - 0.5F) * wander;
        vz += dvz + (random.nextFloat() - 0.5F) * wander;

        double s = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (s > MAX_SPEED) {
            double k = MAX_SPEED / s;
            vx *= k;
            vy *= k;
            vz *= k;
        }

        // no collision on purpose: flies may clip through walls
        this.setPos(x + vx, y + vy, z + vz);
    }

    private void tickLanded() {
        if (random.nextInt(10) == 0) {
            crawlAngle += (random.nextFloat() - 0.5F) * 1.6F;
        }
        double step = random.nextInt(4) == 0 ? 0.006 : 0.0;
        vx = Math.cos(crawlAngle) * step;
        vz = Math.sin(crawlAngle) * step;
        vy = 0.0;
        double nx = x + vx;
        double nz = z + vz;
        if (Math.abs(nx - swarm.center.x) > swarm.halfX + 0.15 || Math.abs(nz - swarm.center.z) > swarm.halfZ + 0.15) {
            crawlAngle += Math.PI;
        } else {
            this.setPos(nx, y, nz);
        }
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());

        // point the long axis along the direction of travel (as seen on screen)
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double mx = this.x - this.xo;
        double my = this.y - this.yo;
        double mz = this.z - this.zo;
        float sx = (float) -(mx * left.x() + my * left.y() + mz * left.z());
        float sy = (float) (mx * up.x() + my * up.y() + mz * up.z());
        if (sx * sx + sy * sy > 1.0E-7F) {
            float wanted = (float) Math.atan2(sy, sx);
            float diff = wanted - renderAngle;
            // a fly is symmetric, so only turn through +-90 degrees
            diff = (float) (diff - Math.PI * Math.round(diff / Math.PI));
            renderAngle += diff * 0.35F;
        }

        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.rotateZ(renderAngle);

        float scale = DeadFliesConfig.FLY_SCALE.get().floatValue();
        float hw = HALF_LENGTH * scale;
        float hh = HALF_HEIGHT * scale;
        Vector3f[] corners = new Vector3f[] {
                new Vector3f(-hw, -hh, 0.0F),
                new Vector3f(-hw, hh, 0.0F),
                new Vector3f(hw, hh, 0.0F),
                new Vector3f(hw, -hh, 0.0F)
        };
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.add(px, py, pz);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    protected int getLightColor(float partialTick) {
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        if (this.level.getBlockState(pos).isSolidRender(this.level, pos)) {
            pos = pos.above();
        }
        return this.level.hasChunkAt(pos) ? LevelRenderer.getLightColor(this.level, pos) : 0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                                     double x, double y, double z,
                                                                     double dx, double dy, double dz) {
            return new FlyParticle(level, x, y, z, sprites);
        }
    }
}

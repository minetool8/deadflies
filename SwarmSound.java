package com.deadflies.client;

import com.deadflies.DeadFliesConfig;
import com.deadflies.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Looping, positional buzz that follows the swarm's flies. */
public class SwarmSound extends AbstractTickableSoundInstance {
    private final FlySwarm swarm;

    public SwarmSound(FlySwarm swarm) {
        super(ModSounds.BUZZ.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.swarm = swarm;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 0.92F + this.random.nextFloat() * 0.16F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
        Vec3 c = swarm.centroid();
        this.x = c.x;
        this.y = c.y;
        this.z = c.z;
    }

    @Override
    public void tick() {
        if (swarm.isRemoved() || !swarm.wantsSound || !DeadFliesConfig.SOUND_ENABLED.get()) {
            this.stop();
            return;
        }
        Vec3 c = swarm.centroid();
        this.x = c.x;
        this.y = c.y;
        this.z = c.z;

        float target = DeadFliesConfig.SOUND_VOLUME.get().floatValue();
        if (this.volume < target) {
            this.volume = Math.min(target, this.volume + 0.03F); // fade in
        } else {
            this.volume = target;
        }
    }
}

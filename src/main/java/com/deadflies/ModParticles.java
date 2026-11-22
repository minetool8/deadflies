package com.deadflies;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DeadFliesMod.MODID);

    public static final RegistryObject<SimpleParticleType> FLY =
            PARTICLES.register("fly", () -> new SimpleParticleType(false));

    private ModParticles() {}
}

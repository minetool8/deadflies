package com.deadflies;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, DeadFliesMod.MODID);

    public static final RegistryObject<SoundEvent> BUZZ = SOUNDS.register("fly_buzz",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DeadFliesMod.MODID, "fly_buzz")));

    private ModSounds() {}
}

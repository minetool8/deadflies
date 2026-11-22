package com.deadflies;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DeadFliesMod.MODID)
public class DeadFliesMod {
    public static final String MODID = "deadflies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeadFliesMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DeadFliesConfig.SPEC);

        // The mod is visual only: players can join servers that do not have it,
        // and servers do not need it.
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> IExtensionPoint.DisplayTest.IGNORESERVERONLY,
                        (remote, isServer) -> true));
    }
}

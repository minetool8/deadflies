package com.deadflies.client;

import com.deadflies.DeadFliesConfig;
import com.deadflies.DeadFliesMod;
import com.deadflies.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = DeadFliesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FLY.get(), FlyParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == DeadFliesConfig.SPEC) {
            BodyMatcher.clearCache();
        }
    }
}

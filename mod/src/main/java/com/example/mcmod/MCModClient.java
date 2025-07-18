package com.example.mcmod;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.entity.client.DoppelgangerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class MCModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, (EntityRendererFactory<DoppelgangerEntity>) (context -> new DoppelgangerRenderer(context)));
        MCMod.LOGGER.info("Initializing MCMod client-side features");
        MCMod.LOGGER.info("Doppelganger will be visible only to the haunted player");
        MCMod.LOGGER.info("Ritual completion now plays demon laugh for haunted player only");
    }
} 
package com.example.mcmod;

import com.example.mcmod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.ZombieEntityRenderer;

public class MCModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the doppelganger renderer (placeholder: zombie)
        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, (context) ->
            new ZombieEntityRenderer(context)
        );
    }
} 
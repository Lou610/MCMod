package com.example.mcmod;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.entity.DoppelgangerEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.MinecraftClient;

public class MCModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the doppelganger renderer with player skin
        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, (context) ->
            new MobEntityRenderer<DoppelgangerEntity, PlayerEntityModel<DoppelgangerEntity>>(
                context,
                new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false),
                0.5f
            ) {
                @Override
                public Identifier getTexture(DoppelgangerEntity entity) {
                    // Get the skin profile name from the doppelganger
                    String skinProfileName = entity.getSkinProfileName();
                    if (skinProfileName != null && !skinProfileName.isEmpty()) {
                        // Try to find the player with this name
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.world != null) {
                            for (var player : client.world.getPlayers()) {
                                if (player.getName().getString().equals(skinProfileName)) {
                                    // Use the player's skin texture
                                    return player.getSkinTextures().texture();
                                }
                            }
                        }
                    }
                    // Fallback to default Steve texture
                    return new Identifier("minecraft", "textures/entity/steve.png");
                }
            }
        );
    }
} 
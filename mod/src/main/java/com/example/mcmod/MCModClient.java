package com.example.mcmod;

import com.example.mcmod.entity.DoppelgangerEntity;
import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.network.HauntPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class MCModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, (EntityRendererFactory.Context ctx) -> {
            // Use the player renderer for the Doppelganger
            return new PlayerEntityRenderer(ctx, false);
        });
        HauntPackets.registerClientHandlers();

        // Client tick: despawn Doppelganger if looked at
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof DoppelgangerEntity doppelganger) {
                    Vec3d playerLook = client.player.getRotationVec(1.0f);
                    Vec3d toEntity = doppelganger.getPos().subtract(client.player.getPos()).normalize();
                    double dot = playerLook.dotProduct(toEntity);
                    double angle = Math.acos(dot) * (180.0 / Math.PI);
                    if (angle < 30) {
                        doppelganger.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        });
    }
} 
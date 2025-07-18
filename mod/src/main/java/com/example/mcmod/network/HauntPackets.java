package com.example.mcmod.network;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.entity.DoppelgangerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import com.example.mcmod.MCMod;
import net.minecraft.server.world.ServerWorld;

public class HauntPackets {
    public static void sendSpawnDoppelganger(ServerPlayerEntity player, Vec3d pos, String skinName) {
        ServerWorld world = (ServerWorld) player.getWorld();
        if (world != null) {
            DoppelgangerEntity entity = new DoppelgangerEntity(ModEntities.DOPPELGANGER, world);
            entity.setPosition(pos);
            entity.setSkinProfileName(skinName);
            world.spawnEntity(entity);
        }
    }

    public static void sendDespawnDoppelganger(ServerPlayerEntity player) {
        // For now, just remove entities directly on the server
        if (player.getWorld() != null) {
            // Use a simple approach to find and remove Doppelganger entities
            // This is a workaround since we don't have the exact API method
            try {
                // Try to access entities through reflection if needed
                java.lang.reflect.Field entitiesField = player.getWorld().getClass().getDeclaredField("entities");
                entitiesField.setAccessible(true);
                java.util.Collection<Entity> entities = (java.util.Collection<Entity>) entitiesField.get(player.getWorld());
                if (entities != null) {
                    entities.removeIf(entity -> entity instanceof DoppelgangerEntity);
                }
            } catch (Exception e) {
                // Fallback: just log that we can't remove entities
                MCMod.LOGGER.warn("Could not remove Doppelganger entities: " + e.getMessage());
            }
        }
    }

    public static void registerClientHandlers() {
    }
} 
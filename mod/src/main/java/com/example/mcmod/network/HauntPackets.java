package com.example.mcmod.network;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.entity.DoppelgangerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;

public class HauntPackets {
    public static final Identifier SPAWN_DOPPELGANGER = new Identifier("mcmod", "spawn_doppelganger");
    public static final Identifier DESPAWN_DOPPELGANGER = new Identifier("mcmod", "despawn_doppelganger");

    public static void sendSpawnDoppelganger(ServerPlayerEntity player, Vec3d pos, String skinName) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeString(skinName);
        ServerPlayNetworking.send(player, SPAWN_DOPPELGANGER, buf);
    }

    public static void sendDespawnDoppelganger(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        ClientPlayNetworking.send(DESPAWN_DOPPELGANGER, buf);
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(SPAWN_DOPPELGANGER, (client, handler, buf, responseSender) -> {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            String skinName = buf.readString();
            client.execute(() -> {
                ClientWorld world = MinecraftClient.getInstance().world;
                if (world != null) {
                    DoppelgangerEntity entity = ModEntities.DOPPELGANGER.create(world);
                    if (entity != null) {
                        entity.setPosition(x, y, z);
                        entity.setSkinProfileName(skinName);
                        world.addEntity(entity.getId(), entity);
                    }
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DESPAWN_DOPPELGANGER, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                ClientWorld world = MinecraftClient.getInstance().world;
                if (world != null) {
                    // Remove all Doppelganger entities
                    world.getEntities().stream()
                        .filter(e -> e instanceof DoppelgangerEntity)
                        .forEach(e -> e.remove(Entity.RemovalReason.DISCARDED));
                }
            });
        });
    }
} 
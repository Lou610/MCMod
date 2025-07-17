package com.example.mcmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DoppelgangerEntity extends MobEntity {
    private String skinProfileName;

    public DoppelgangerEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
    }

    public void setSkinProfileName(String name) {
        this.skinProfileName = name;
    }

    public String getSkinProfileName() {
        return skinProfileName;
    }

    protected void initGoals() {
        // No AI, just stands and stares
    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        if (skinProfileName != null) nbt.putString("SkinProfileName", skinProfileName);
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("SkinProfileName")) {
            skinProfileName = nbt.getString("SkinProfileName").orElse(null);
        }
    }

    // TODO: Custom rendering to use the haunted player's skin
    // TODO: Only visible to the haunted player
} 
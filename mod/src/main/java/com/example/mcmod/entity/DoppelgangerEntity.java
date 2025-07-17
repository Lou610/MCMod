package com.example.mcmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
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

    @Override
    protected void initGoals() {
        // No AI, just stands and stares
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (skinProfileName != null) nbt.putString("SkinProfileName", skinProfileName);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SkinProfileName")) skinProfileName = nbt.getString("SkinProfileName");
    }

    // TODO: Custom rendering to use the haunted player's skin
    // TODO: Only visible to the haunted player
} 
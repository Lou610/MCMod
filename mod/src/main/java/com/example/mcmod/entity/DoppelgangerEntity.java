package com.example.mcmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundPointOfInterestGoal;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;

public class DoppelgangerEntity extends MobEntity {
    private String skinProfileName;
    private PlayerEntity targetPlayer;
    private long nextAppearTick = 0L;

    public DoppelgangerEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0);
    }

    public void setSkinProfileName(String name) {
        this.skinProfileName = name;
    }

    public String getSkinProfileName() {
        return skinProfileName;
    }

    public void setTargetPlayer(PlayerEntity player) {
        this.targetPlayer = player;
    }

    public void setNextAppearTick(long tick) {
        this.nextAppearTick = tick;
    }

    public long getNextAppearTick() {
        return nextAppearTick;
    }

    @Override
    protected void initGoals() {
        // Make the doppelganger look at the target player
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        // No vanilla wander goal, using custom walk in tick()
    }

    @Override
    public void tick() {
        super.tick();
        // Make the doppelganger face the target player when they're close
        if (targetPlayer != null && targetPlayer.isAlive()) {
            Vec3d toPlayer = targetPlayer.getPos().subtract(this.getPos()).normalize();
            this.setYaw((float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z)));
        }
        // Custom random walk: only if not being looked at
        if (this.getWorld() != null && !this.getWorld().isClient && this.age % 20 == 0) { // every second
            if (targetPlayer != null && !isPlayerLookingAtMe(targetPlayer)) {
                double angle = this.random.nextDouble() * 2 * Math.PI;
                double distance = 2.0 + this.random.nextDouble() * 3.0; // 2-5 blocks
                double dx = MathHelper.cos((float) angle) * distance;
                double dz = MathHelper.sin((float) angle) * distance;
                double newX = this.getX() + dx;
                double newZ = this.getZ() + dz;
                double newY = this.getY();
                BlockPos newPos = BlockPos.ofFloored(newX, newY, newZ);
                if (this.getWorld().isAir(newPos) && this.getWorld().isAir(newPos.up())) {
                    this.getNavigation().startMovingTo(newX, newY, newZ, 1.0);
                }
            }
        }
    }

    private boolean isPlayerLookingAtMe(PlayerEntity player) {
        Vec3d playerLook = player.getRotationVec(1.0f);
        Vec3d toEntity = this.getPos().subtract(player.getPos()).normalize();
        double dot = playerLook.dotProduct(toEntity);
        double angle = Math.acos(dot) * (180.0 / Math.PI);
        return angle < 30.0;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (skinProfileName != null) nbt.putString("SkinProfileName", skinProfileName);
        nbt.putLong("NextAppearTick", nextAppearTick);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SkinProfileName")) {
            skinProfileName = nbt.getString("SkinProfileName");
        }
        if (nbt.contains("NextAppearTick")) {
            nextAppearTick = nbt.getLong("NextAppearTick");
        }
    }

    // TODO: Custom rendering to use the haunted player's skin
    // TODO: Only visible to the haunted player
} 
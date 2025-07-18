package com.example.mcmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;

public class DoppelgangerEntity extends MobEntity {
    private String skinProfileName;
    private PlayerEntity targetPlayer;
    private long nextTeleportTick = 0L;
    private long lastTeleportTime = 0L;
    private Vec3d lastKnownPlayerPos;
    private boolean isTeleporting = false;

    public DoppelgangerEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
        this.setCustomNameVisible(true);
        this.setCustomName(Text.literal("§8Doppelganger"));
    }

    public void setSkinProfileName(String name) {
        this.skinProfileName = name;
        this.setCustomName(Text.literal("§8" + name));
    }

    public String getSkinProfileName() {
        return skinProfileName;
    }

    public void setTargetPlayer(PlayerEntity player) {
        this.targetPlayer = player;
        if (player != null) {
            this.lastKnownPlayerPos = player.getPos();
        }
    }

    public void setNextTeleportTick(long tick) {
        this.nextTeleportTick = tick;
    }

    public long getNextTeleportTick() {
        return nextTeleportTick;
    }

    @Override
    protected void initGoals() {
        // No AI goals to avoid compatibility issues
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.getWorld() == null || this.getWorld().isClient) {
            return;
        }

        if (targetPlayer != null && targetPlayer.isAlive()) {
            Vec3d playerPos = targetPlayer.getPos();
            Vec3d myPos = this.getPos();
            double distance = playerPos.distanceTo(myPos);
            
            // Update last known player position
            this.lastKnownPlayerPos = playerPos;
            
            // Check if player is looking at us
            boolean isPlayerLooking = isPlayerLookingAtMe(targetPlayer);
            
            // If player is looking at us and we're close, teleport away
            if (isPlayerLooking && distance < 12.0 && !isTeleporting) {
                teleportToNewLocation();
            }
            
            // If we're too far from player, teleport closer
            if (distance > 25.0 && !isTeleporting) {
                teleportCloserToPlayer();
            }
            
            // Random teleportation when not being looked at
            if (!isPlayerLooking && distance < 8.0 && this.random.nextInt(400) == 0 && !isTeleporting) {
                teleportToNewLocation();
            }
            
            // Make the doppelganger face the target player when they're close
            if (distance < 16.0) {
                Vec3d toPlayer = playerPos.subtract(myPos).normalize();
                this.setYaw((float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z)));
            }
        }
    }

    private void teleportToNewLocation() {
        if (isTeleporting || targetPlayer == null) return;
        
        isTeleporting = true;
        long currentTime = this.getWorld().getTime();
        
        // Don't teleport too frequently
        if (currentTime - lastTeleportTime < 60) { // 3 seconds minimum
            isTeleporting = false;
            return;
        }
        
        Vec3d playerPos = targetPlayer.getPos();
        double angle = this.random.nextDouble() * 2 * Math.PI;
        double distance = 8.0 + this.random.nextDouble() * 12.0; // 8-20 blocks away
        double dx = MathHelper.cos((float) angle) * distance;
        double dz = MathHelper.sin((float) angle) * distance;
        
        double newX = playerPos.x + dx;
        double newZ = playerPos.z + dz;
        double newY = findSafeY(newX, newZ);
        
        if (newY > 0) {
            // Play teleport sound at old position
            this.getWorld().playSound(null, BlockPos.ofFloored(this.getPos()), 
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.5f, 1.0f);
            
            // Teleport using setPosition instead of teleport method
            this.setPosition(newX, newY, newZ);
            
            // Play teleport sound at new position
            this.getWorld().playSound(null, BlockPos.ofFloored(this.getPos()), 
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.5f, 1.0f);
            
            lastTeleportTime = currentTime;
            
            // Send message to target player (only occasionally)
            if (targetPlayer instanceof ServerPlayerEntity && this.random.nextInt(10) == 0) {
                ((ServerPlayerEntity) targetPlayer).sendMessage(
                    Text.literal("§8You hear a whisper in the distance..."), false);
            }
        }
        
        isTeleporting = false;
    }

    private void teleportCloserToPlayer() {
        if (isTeleporting || targetPlayer == null) return;
        
        isTeleporting = true;
        long currentTime = this.getWorld().getTime();
        
        // Don't teleport too frequently
        if (currentTime - lastTeleportTime < 60) {
            isTeleporting = false;
            return;
        }
        
        Vec3d playerPos = targetPlayer.getPos();
        double angle = this.random.nextDouble() * 2 * Math.PI;
        double distance = 6.0 + this.random.nextDouble() * 8.0; // 6-14 blocks away
        double dx = MathHelper.cos((float) angle) * distance;
        double dz = MathHelper.sin((float) angle) * distance;
        
        double newX = playerPos.x + dx;
        double newZ = playerPos.z + dz;
        double newY = findSafeY(newX, newZ);
        
        if (newY > 0) {
            // Play teleport sound at old position
            this.getWorld().playSound(null, BlockPos.ofFloored(this.getPos()), 
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.5f, 1.0f);
            
            // Teleport using setPosition instead of teleport method
            this.setPosition(newX, newY, newZ);
            
            // Play teleport sound at new position
            this.getWorld().playSound(null, BlockPos.ofFloored(this.getPos()), 
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.5f, 1.0f);
            
            lastTeleportTime = currentTime;
        }
        
        isTeleporting = false;
    }

    private double findSafeY(double x, double z) {
        BlockPos pos = BlockPos.ofFloored(x, this.getY(), z);
        World world = this.getWorld();
        
        // Try to find a safe position near the current Y level
        for (int yOffset = -5; yOffset <= 5; yOffset++) {
            BlockPos testPos = pos.add(0, yOffset, 0);
            if (world.isAir(testPos) && world.isAir(testPos.up()) && 
                !world.isAir(testPos.down())) {
                return testPos.getY() + 0.5;
            }
        }
        
        // If no safe position found, return current Y
        return this.getY();
    }

    private boolean isPlayerLookingAtMe(PlayerEntity player) {
        Vec3d playerLook = player.getRotationVec(1.0f);
        Vec3d toEntity = this.getPos().subtract(player.getPos()).normalize();
        double dot = playerLook.dotProduct(toEntity);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot))) * (180.0 / Math.PI);
        return angle < 25.0; // 25 degree cone
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        // Only visible to the target player (haunted player)
        return !player.equals(targetPlayer);
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 20.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.25);
    }
} 
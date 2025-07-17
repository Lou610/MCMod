package com.example.mcmod;

import com.example.mcmod.entity.DoppelgangerEntity;
import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.network.HauntPackets;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.block.entity.SignText;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Random;
import java.util.HashMap;
import java.util.Optional;
import java.lang.reflect.Method;
import net.minecraft.entity.damage.DamageSource;

public class HauntingManager {
    private static UUID hauntedPlayerId = null;
    private static final WeakHashMap<ServerPlayerEntity, DoppelgangerEntity> doppelgangers = new WeakHashMap<>();
    private static final Random random = new Random();
    private static final HashMap<UUID, Long> hauntingStartTime = new HashMap<>();
    private static final HashMap<UUID, Boolean> entityCanEnterHome = new HashMap<>();
    private static final HashMap<UUID, Long> lastKnockTime = new HashMap<>();
    private static final HashMap<UUID, Boolean> basementBuilt = new HashMap<>();
    private static final HashMap<UUID, Long> lastDamageTime = new HashMap<>();
    private static final HashMap<UUID, Long> nextDoppelgangerAppearTime = new HashMap<>();

    // Utility method to get player's bed/respawn position using reflection
    private static BlockPos getPlayerBedPosition(ServerPlayerEntity player) {
        try {
            // Try common field names for bed/respawn position
            String[] possibleFieldNames = {
                "spawnPointPosition", "bedPosition", "respawnPosition", "spawnPoint",
                "bedLocation", "respawnLocation", "spawnLocation"
            };
            
            Class<?> playerClass = player.getClass();
            
            // First try ServerPlayerEntity
            for (String fieldName : possibleFieldNames) {
                try {
                    Field field = playerClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(player);
                    if (value instanceof BlockPos) {
                        return (BlockPos) value;
                    } else if (value instanceof GlobalPos) {
                        // Try to get BlockPos from GlobalPos using reflection
                        try {
                            Field posField = GlobalPos.class.getDeclaredField("pos");
                            posField.setAccessible(true);
                            return (BlockPos) posField.get(value);
                        } catch (Exception e) {
                            // Continue to next field
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Continue to next field
                }
            }
            
            // Try parent class (PlayerEntity)
            Class<?> parentClass = playerClass.getSuperclass();
            if (parentClass != null) {
                for (String fieldName : possibleFieldNames) {
                    try {
                        Field field = parentClass.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object value = field.get(player);
                        if (value instanceof BlockPos) {
                            return (BlockPos) value;
                        } else if (value instanceof GlobalPos) {
                            // Try to get BlockPos from GlobalPos using reflection
                            try {
                                Field posField = GlobalPos.class.getDeclaredField("pos");
                                posField.setAccessible(true);
                                return (BlockPos) posField.get(value);
                            } catch (Exception e) {
                                // Continue to next field
                            }
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        // Continue to next field
                    }
                }
            }
            
        } catch (Exception e) {
            MCMod.LOGGER.warn("Failed to get bed position for player: " + e.getMessage());
        }
        
        // Fallback: return null if no bed position found
        return null;
    }

    public static void startHaunting(ServerPlayerEntity player) {
        hauntedPlayerId = player.getUuid();
        hauntingStartTime.put(hauntedPlayerId, player.getWorld().getTime());
    }

    public static void stopHaunting() {
        hauntedPlayerId = null;
        doppelgangers.keySet().forEach(player -> HauntPackets.sendDespawnDoppelganger(player));
        doppelgangers.clear();
        hauntingStartTime.clear();
    }

    public static boolean isHaunted(ServerPlayerEntity player) {
        return hauntedPlayerId != null && hauntedPlayerId.equals(player.getUuid());
    }

    public static UUID getHauntedPlayerId() {
        return hauntedPlayerId;
    }

    public static long getHauntingDuration(ServerPlayerEntity player) {
        if (!isHaunted(player)) return 0;
        long start = hauntingStartTime.getOrDefault(player.getUuid(), player.getWorld().getTime());
        return player.getWorld().getTime() - start;
    }

    public static boolean isPlayerAtHome(ServerPlayerEntity player) {
        BlockPos bed = getPlayerBedPosition(player);
        if (bed == null) return false;
        World world = player.getWorld();
        double dist = player.getPos().distanceTo(Vec3d.ofCenter(bed));
        return dist < 8.0;
    }

    // Call this from a server tick event
    public static void tick(MinecraftServer server) {
        if (hauntedPlayerId == null) return;
        ServerPlayerEntity player = getHauntedPlayer(server);
        if (player == null || !player.isAlive()) {
            stopHaunting();
            return;
        }
        long duration = getHauntingDuration(player);
        boolean atHome = isPlayerAtHome(player);
        boolean canEnter = entityCanEnterHome.getOrDefault(player.getUuid(), false);
        
        // For testing: immediately allow doppelganger to enter after ritual
        if (!canEnter && duration > 100) { // After 5 seconds instead of 2-4 days
            entityCanEnterHome.put(player.getUuid(), true);
            player.sendMessage(Text.literal("§8The entity can now enter your home..."), false);
        }
        
        // Knocking phase: after 2-3 days (24000 ticks per day) - DISABLED FOR TESTING
        if (!canEnter && duration > 2 * 24000 && duration < 4 * 24000 && atHome) {
            long now = player.getWorld().getTime();
            long lastKnock = lastKnockTime.getOrDefault(player.getUuid(), 0L);
            if (now - lastKnock > 100) { // Knock every 5 seconds (100 ticks)
                BlockPos door = findNearestDoor(player);
                if (door != null) {
                    // Play knocking sound at the door
                    player.getWorld().playSound(null, door, SoundEvent.of(Identifier.of("mcmod", "haunt_knock")), SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                lastKnockTime.put(player.getUuid(), now);
            }
        }
        
        // Detect if player opens the door during knocking phase - DISABLED FOR TESTING
        if (!canEnter && atHome && player.isUsingItem()) {
            BlockPos door = findNearestDoor(player);
            if (door != null && isDoorOpen((ServerWorld) player.getWorld(), door)) {
                entityCanEnterHome.put(player.getUuid(), true);
                // Play demon laugh sound for the haunted player
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_laugh")), SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }
        
        // Doppelganger peripheral vision logic
        if (entityCanEnterHome.getOrDefault(player.getUuid(), false)) {
            DoppelgangerEntity doppelganger = doppelgangers.get(player);
            
            // Check if we need to respawn the doppelganger after cooldown
            if (doppelganger == null) {
                // Check if there's a cooldown timer for this player
                Long nextAppearTime = nextDoppelgangerAppearTime.get(player.getUuid());
                if (nextAppearTime != null && nextAppearTime > 0) {
                    long currentTime = player.getWorld().getTime();
                    if (currentTime >= nextAppearTime) {
                        // Time to respawn!
                        nextDoppelgangerAppearTime.remove(player.getUuid());
                        player.sendMessage(Text.literal("§8You feel a presence watching you again..."), false);
                    } else {
                        // Still in cooldown, don't spawn yet
                        return;
                    }
                }
                
                // Spawn doppelganger if it doesn't exist
                if (doppelganger == null) {
                    BlockPos spawnPos = getPeripheralSpawnPos(player);
                    if (spawnPos != null) {
                        GameProfile profile = player.getGameProfile();
                        doppelganger = new DoppelgangerEntity(ModEntities.DOPPELGANGER, player.getWorld());
                        doppelganger.setPosition(Vec3d.ofCenter(spawnPos));
                        doppelganger.setSkinProfileName(profile.getName());
                        doppelganger.setTargetPlayer(player);
                        player.getWorld().spawnEntity(doppelganger);
                        doppelgangers.put(player, doppelganger);
                        // Play ghostly whispers sound for the haunted player
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_whisper")), SoundCategory.PLAYERS, 1.0f, 1.0f);
                        player.sendMessage(Text.literal("§8A doppelganger has appeared in your peripheral vision..."), false);
                        MCMod.LOGGER.info("Doppelganger spawned for player {} at position {}", player.getName().getString(), spawnPos);
                    } else {
                        player.sendMessage(Text.literal("§8Failed to find spawn position for doppelganger"), false);
                        MCMod.LOGGER.warn("Could not find peripheral spawn position for player {}", player.getName().getString());
                    }
                }
            } else {
                // Handle doppelganger movement and damage logic
                handleDoppelgangerBehavior(player, doppelganger);
            }
            
            // Secret basement construction and sign placement
            if (shouldBuildBasement(duration)) {
                buildSecretBasement(player);
                placeLureSigns(player);
                checkBasementFinale(player);
            }
        } else {
            // Debug message for testing
            if (duration % 200 == 0) { // Every 10 seconds
                player.sendMessage(Text.literal("§7Debug: Haunting duration: " + duration + " ticks, Can enter: " + canEnter), false);
            }
        }
    }

    private static ServerPlayerEntity getHauntedPlayer(MinecraftServer server) {
        if (hauntedPlayerId == null) return null;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getUuid().equals(hauntedPlayerId)) return player;
        }
        return null;
    }

    private static BlockPos getPeripheralSpawnPos(ServerPlayerEntity player) {
        Vec3d playerPos = player.getPos();
        Vec3d look = player.getRotationVec(1.0f);
        // Try up to 10 times to find a valid peripheral position
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(60 + random.nextInt(60)); // 60-120 degrees off center
            if (random.nextBoolean()) angle = -angle;
            double radius = 6 + random.nextInt(5); // 6-10 blocks away
            double dx = look.x * Math.cos(angle) - look.z * Math.sin(angle);
            double dz = look.x * Math.sin(angle) + look.z * Math.cos(angle);
            double x = playerPos.x + dx * radius;
            double z = playerPos.z + dz * radius;
            int y = player.getBlockY();
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            // Check for air block to spawn in
            if (player.getWorld().isAir(pos) && player.getWorld().isAir(pos.up())) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isLookingAt(ServerPlayerEntity player, DoppelgangerEntity entity, double thresholdDegrees) {
        Vec3d playerLook = player.getRotationVec(1.0f);
        Vec3d toEntity = entity.getPos().subtract(player.getPos()).normalize();
        double dot = playerLook.dotProduct(toEntity);
        double angle = Math.acos(dot) * (180.0 / Math.PI);
        return angle < thresholdDegrees;
    }

    private static BlockPos findNearestDoor(ServerPlayerEntity player) {
        BlockPos bed = getPlayerBedPosition(player);
        if (bed == null) return null;
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(bed, 8, 4, 8)) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof DoorBlock) {
                double dist = player.getPos().distanceTo(Vec3d.ofCenter(pos));
                if (dist < minDist) {
                    minDist = dist;
                    nearest = pos;
                }
            }
        }
        return nearest;
    }

    private static boolean isDoorOpen(ServerWorld world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof DoorBlock) {
            return world.getBlockState(pos).get(DoorBlock.OPEN);
        }
        return false;
    }

    private static BlockPos getHomeSpawnPos(ServerPlayerEntity player) {
        BlockPos bed = getPlayerBedPosition(player);
        if (bed == null) return null;
        ServerWorld world = (ServerWorld) player.getWorld();
        // Try up to 10 times to find a valid spawn position in the home (within 6 blocks of bed)
        for (int i = 0; i < 10; i++) {
            int dx = random.nextInt(13) - 6;
            int dz = random.nextInt(13) - 6;
            BlockPos pos = bed.add(dx, 0, dz);
            if (world.isAir(pos) && world.isAir(pos.up())) {
                return pos;
            }
        }
        return null;
    }

    private static boolean shouldBuildBasement(long duration) {
        // Start building basement after 4 days of haunting
        return duration > 4 * 24000;
    }

    private static void buildSecretBasement(ServerPlayerEntity player) {
        if (basementBuilt.getOrDefault(player.getUuid(), false)) return;
        BlockPos bed = getPlayerBedPosition(player);
        if (bed == null) return;
        ServerWorld world = (ServerWorld) player.getWorld();
        // Dig stairs down from under the bed
        BlockPos stairStart = bed.down();
        for (int i = 0; i < 4; i++) {
            BlockPos step = stairStart.down(i).offset(player.getHorizontalFacing(), 0);
            world.setBlockState(step, Blocks.AIR.getDefaultState());
        }
        // Dig out 5x5x3 room
        BlockPos roomOrigin = stairStart.down(4).add(-2, 0, -2);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos pos = roomOrigin.add(x, y, z);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
        }
        // Place floor and walls (stone bricks)
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                world.setBlockState(roomOrigin.add(x, 0, z), Blocks.STONE_BRICKS.getDefaultState()); // floor
                world.setBlockState(roomOrigin.add(x, 2, z), Blocks.STONE_BRICKS.getDefaultState()); // ceiling
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int i = 0; i < 5; i++) {
                world.setBlockState(roomOrigin.add(0, y, i), Blocks.STONE_BRICKS.getDefaultState());
                world.setBlockState(roomOrigin.add(4, y, i), Blocks.STONE_BRICKS.getDefaultState());
                world.setBlockState(roomOrigin.add(i, y, 0), Blocks.STONE_BRICKS.getDefaultState());
                world.setBlockState(roomOrigin.add(i, y, 4), Blocks.STONE_BRICKS.getDefaultState());
            }
        }
        // Place a sign in the basement
        BlockPos signPos = roomOrigin.add(2, 1, 2);
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState());
        SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(signPos);
        if (sign != null) {
            // Use reflection to set sign text since the methods are not accessible
            try {
                // Try to find and call the setText method using reflection
                Method setTextMethod = SignBlockEntity.class.getDeclaredMethod("setText", int.class, Text.class);
                setTextMethod.setAccessible(true);
                setTextMethod.invoke(sign, 0, Text.literal("Welcome home."));
                setTextMethod.invoke(sign, 1, Text.literal("You found me."));
                setTextMethod.invoke(sign, 2, Text.empty());
                setTextMethod.invoke(sign, 3, Text.empty());
            } catch (Exception e) {
                MCMod.LOGGER.warn("Could not set sign text: " + e.getMessage());
            }
        }
        basementBuilt.put(player.getUuid(), true);
    }

    private static void placeLureSigns(ServerPlayerEntity player) {
        BlockPos bed = getPlayerBedPosition(player);
        if (bed == null) return;
        ServerWorld world = (ServerWorld) player.getWorld();
        // Place a sign near the bed
        BlockPos signPos = bed.add(1, 0, 0);
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState());
        SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(signPos);
        if (sign != null) {
            // Use reflection to set sign text since the methods are not accessible
            try {
                // Try to find and call the setText method using reflection
                Method setTextMethod = SignBlockEntity.class.getDeclaredMethod("setText", int.class, Text.class);
                setTextMethod.setAccessible(true);
                setTextMethod.invoke(sign, 0, Text.literal("Come downstairs..."));
                setTextMethod.invoke(sign, 1, Text.literal("I have something for you..."));
                setTextMethod.invoke(sign, 2, Text.empty());
                setTextMethod.invoke(sign, 3, Text.empty());
            } catch (Exception e) {
                MCMod.LOGGER.warn("Could not set sign text: " + e.getMessage());
            }
        }
    }

    private static void checkBasementFinale(ServerPlayerEntity player) {
        BlockPos bed = getPlayerBedPosition(player);
        if (bed == null) return;
        BlockPos stairStart = bed.down();
        BlockPos roomOrigin = stairStart.down(4).add(-2, 0, -2);
        BlockPos playerPos = player.getBlockPos();
        // If player is inside the basement room
        if (playerPos.getY() >= roomOrigin.getY() && playerPos.getY() < roomOrigin.getY() + 3 &&
            playerPos.getX() >= roomOrigin.getX() && playerPos.getX() < roomOrigin.getX() + 5 &&
            playerPos.getZ() >= roomOrigin.getZ() && playerPos.getZ() < roomOrigin.getZ() + 5) {
            // Kill the player - use a simpler approach for 1.20.4
            try {
                player.damage(player.getWorld().getDamageSources().generic(), Float.MAX_VALUE);
            } catch (Exception e) {
                MCMod.LOGGER.warn("Could not damage player: " + e.getMessage());
            }
            // End haunting for this player
            UUID oldId = player.getUuid();
            stopHaunting();
            // Move haunting to next player if multiplayer
            ServerPlayerEntity next = pickNextHauntedPlayer(player);
            if (next != null) {
                startHaunting(next);
                next.sendMessage(Text.literal("§4You feel a presence watching you..."), false);
            }
        }
    }

    private static ServerPlayerEntity pickNextHauntedPlayer(ServerPlayerEntity previous) {
        MinecraftServer server = previous.getServer();
        if (server == null) return null;
        var players = server.getPlayerManager().getPlayerList();
        if (players.size() <= 1) return null;
        // Pick a random player who is not the previous victim
        var eligible = players.stream().filter(p -> !p.getUuid().equals(previous.getUuid())).toList();
        if (eligible.isEmpty()) return null;
        return eligible.get(random.nextInt(eligible.size()));
    }

    private static void handleDoppelgangerBehavior(ServerPlayerEntity player, DoppelgangerEntity doppelganger) {
        if (!doppelganger.isAlive()) {
            doppelgangers.remove(player);
            return;
        }
        
        Vec3d playerPos = player.getPos();
        Vec3d doppelPos = doppelganger.getPos();
        double distance = playerPos.distanceTo(doppelPos);
        
        // Check if player is looking at the doppelganger
        boolean isLookingAt = isLookingAt(player, doppelganger, 30.0); // 30 degree cone
        
        // If player is looking directly at doppelganger, make it disappear for 60-512 seconds
        if (isLookingAt && distance < 15.0) {
            // Set the next appear time (60-512 seconds = 1200-10240 ticks)
            long disappearTime = 1200 + random.nextInt(10240 - 1200 + 1); // 60-512 seconds
            long nextAppearTime = player.getWorld().getTime() + disappearTime;
            nextDoppelgangerAppearTime.put(player.getUuid(), nextAppearTime);
            
            // Remove the doppelganger from the map and kill it
            doppelgangers.remove(player);
            doppelganger.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            
            // Play disappearing sound
            player.getWorld().playSound(null, BlockPos.ofFloored(doppelPos), SoundEvent.of(Identifier.of("mcmod", "haunt_disappear")), SoundCategory.PLAYERS, 0.5f, 1.0f);
            
            // Send message to player
            player.sendMessage(Text.literal("§8The doppelganger has vanished..."), false);
            
            MCMod.LOGGER.info("Doppelganger disappeared for player {} for {} ticks ({} seconds)", 
                player.getName().getString(), disappearTime, disappearTime / 20);
        }
        
        // If player looks away and doppelganger is close, damage them
        if (!isLookingAt && distance < 3.0) {
            long now = player.getWorld().getTime();
            long lastDamage = lastDamageTime.getOrDefault(player.getUuid(), 0L);
            if (now - lastDamage > 60) { // Damage every 3 seconds
                player.damage(player.getWorld().getDamageSources().generic(), 2.0f);
                lastDamageTime.put(player.getUuid(), now);
                // Play damage sound
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_damage")), SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }
        
        // Occasionally move to a new peripheral position
        if (random.nextInt(200) == 0) { // 1 in 200 chance per tick
            BlockPos newPos = getPeripheralSpawnPos(player);
            if (newPos != null) {
                doppelganger.teleport(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);
            }
        }
    }
} 
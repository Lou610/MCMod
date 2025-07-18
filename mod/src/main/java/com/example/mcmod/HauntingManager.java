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
        
        // Early sightings phase: Days 1-2 - occasional doppelganger sightings to create "being watched" feeling
        if (!canEnter && duration > 1200 && duration < 2 * 24000) { // After 1 minute for testing (was 24000)
            long now = player.getWorld().getTime();
            long lastSighting = lastKnockTime.getOrDefault(player.getUuid(), 0L); // Reusing this for sighting cooldown
            
            // Debug: Check if we're in the right phase
            if (duration % 1200 == 0) { // Every minute
                player.sendMessage(Text.literal("§7Debug: Haunting duration: " + duration + " ticks, canEnter: " + canEnter), false);
            }
            
            // Debug: Check if we should spawn
            if (now - lastSighting > 200 + random.nextInt(400)) { // 10-30 seconds for testing
                player.sendMessage(Text.literal("§7Debug: Attempting to spawn doppelganger..."), false);
            }
            
            // Random sightings every 10-30 seconds for testing
            if (now - lastSighting > 200 + random.nextInt(400)) { // 10-30 seconds for testing
                BlockPos spawnPos = getPeripheralSpawnPos(player);
                if (spawnPos != null) {
                    player.sendMessage(Text.literal("§7Debug: Found spawn position at " + spawnPos), false);
                    try {
                        // Create a temporary doppelganger for early sightings
                        DoppelgangerEntity doppelganger = new DoppelgangerEntity(ModEntities.DOPPELGANGER, player.getWorld());
                        doppelganger.setPosition(Vec3d.ofCenter(spawnPos));
                        doppelganger.setCustomName(Text.literal("§8" + player.getName().getString()));
                        doppelganger.setCustomNameVisible(true);
                        doppelganger.setTargetPlayer(player);
                        doppelganger.setSkinProfileName(player.getName().getString());
                        player.getWorld().spawnEntity(doppelganger);
                        
                        // Play ghostly whispers sound and send whisper message
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_whisper")), SoundCategory.PLAYERS, 0.7f, 1.0f);
                        player.sendMessage(Text.literal("§8You hear a whisper in the distance..."), false);
                        
                        // Store the doppelganger for removal later
                        final DoppelgangerEntity finalDoppelganger = doppelganger;
                        final int removeDelay = 60 + random.nextInt(40); // 3-5 seconds
                        
                        // Schedule removal
                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                player.getWorld().getServer().execute(() -> {
                                    if (finalDoppelganger.isAlive()) {
                                        finalDoppelganger.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
                                    }
                                });
                            }
                        }, removeDelay * 50); // Convert ticks to milliseconds
                        
                        lastKnockTime.put(player.getUuid(), now);
                        MCMod.LOGGER.info("Early doppelganger sighting for player {} at position {}", player.getName().getString(), spawnPos);
                        player.sendMessage(Text.literal("§7Debug: Doppelganger spawned successfully!"), false);
                    } catch (Exception e) {
                        MCMod.LOGGER.warn("Could not spawn early doppelganger sighting: " + e.getMessage());
                        player.sendMessage(Text.literal("§7Debug: Failed to spawn doppelganger: " + e.getMessage()), false);
                    }
                } else {
                    player.sendMessage(Text.literal("§7Debug: Could not find spawn position"), false);
                }
            }
        }
        
        // Knocking phase: after 2-3 days (24000 ticks per day)
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
        
        // Detect if player opens the door during knocking phase
        if (!canEnter && atHome && player.isUsingItem()) {
            BlockPos door = findNearestDoor(player);
            if (door != null && isDoorOpen((ServerWorld) player.getWorld(), door)) {
                entityCanEnterHome.put(player.getUuid(), true);
                player.sendMessage(Text.literal("§8The entity can now enter your home..."), false);
                // Play demon laugh sound for the haunted player
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_laugh")), SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }
        
        // Doppelganger peripheral vision logic
        if (entityCanEnterHome.getOrDefault(player.getUuid(), false)) {
            DoppelgangerEntity doppelganger = doppelgangers.get(player);
            
            // Check if we need to respawn the doppelganger after cooldown
            if (doppelganger == null || !doppelganger.isAlive()) {
                // Debug: Print current state
                player.sendMessage(Text.literal("§7Debug: Starting doppelganger spawn process"), false);
                MCMod.LOGGER.info("Debug: Starting doppelganger spawn process for player {}", player.getName().getString());
                
                // Check if there's a cooldown timer for this player
                Long nextAppearTime = nextDoppelgangerAppearTime.get(player.getUuid());
                long currentTime = player.getWorld().getTime();
                
                if (nextAppearTime != null && nextAppearTime > 0) {
                    if (currentTime >= nextAppearTime) {
                        // Time to respawn!
                        nextDoppelgangerAppearTime.remove(player.getUuid());
                        player.sendMessage(Text.literal("§8You feel a presence watching you again..."), false);
                    } else {
                        // Still in cooldown, don't spawn yet
                        return;
                    }
                } else {
                    // No cooldown set, set one to prevent spam
                    nextDoppelgangerAppearTime.put(player.getUuid(), currentTime + 600); // 30 second cooldown
                    return;
                }
                
                // Debug: About to get spawn position
                player.sendMessage(Text.literal("§7Debug: Getting spawn position..."), false);
                
                // Spawn doppelganger if it doesn't exist
                BlockPos spawnPos = getPeripheralSpawnPos(player);
                if (spawnPos != null) {
                    player.sendMessage(Text.literal("§7Debug: Found spawn position: " + spawnPos), false);
                    try {
                        // Debug: Print entity type and world
                        player.sendMessage(Text.literal("§7Debug: ModEntities.DOPPELGANGER=" + ModEntities.DOPPELGANGER), false);
                        player.sendMessage(Text.literal("§7Debug: player.getWorld()=" + player.getWorld()), false);
                        MCMod.LOGGER.info("Debug: ModEntities.DOPPELGANGER={}", ModEntities.DOPPELGANGER);
                        MCMod.LOGGER.info("Debug: player.getWorld()={}", player.getWorld());
                        // Debug: Check if ModEntities.DOPPELGANGER is null
                        if (ModEntities.DOPPELGANGER == null) {
                            MCMod.LOGGER.error("ModEntities.DOPPELGANGER is null! Entity not registered properly.");
                            player.sendMessage(Text.literal("§cDebug: Doppelganger entity not registered"), false);
                            return;
                        }
                        
                        // Create a doppelganger entity
                        doppelganger = new DoppelgangerEntity(ModEntities.DOPPELGANGER, player.getWorld());
                        doppelganger.setPosition(Vec3d.ofCenter(spawnPos));
                        doppelganger.setCustomName(Text.literal("§8" + player.getName().getString()));
                        doppelganger.setCustomNameVisible(true);
                        doppelganger.setTargetPlayer(player);
                        doppelganger.setSkinProfileName(player.getName().getString());
                        player.getWorld().spawnEntity(doppelganger);
                        
                        // Store the doppelganger
                        doppelgangers.put(player, doppelganger);
                        
                        // Play ghostly whispers sound for the haunted player
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_whisper")), SoundCategory.PLAYERS, 1.0f, 1.0f);
                        player.sendMessage(Text.literal("§8A doppelganger has appeared in your peripheral vision..."), false);
                        MCMod.LOGGER.info("Doppelganger spawned for player {} at position {}", player.getName().getString(), spawnPos);
                    } catch (Exception e) {
                        MCMod.LOGGER.error("Could not spawn doppelganger", e);
                        java.io.StringWriter sw = new java.io.StringWriter();
                        e.printStackTrace(new java.io.PrintWriter(sw));
                        String stackTrace = sw.toString();
                        player.sendMessage(Text.literal("§cDebug: Failed to spawn doppelganger: " + e), false);
                        player.sendMessage(Text.literal(stackTrace.substring(0, Math.min(stackTrace.length(), 500))), false); // Only show first 500 chars
                        // Set a shorter cooldown on failure
                        nextDoppelgangerAppearTime.put(player.getUuid(), currentTime + 300); // 15 second cooldown
                    }
                } else {
                    // Set a shorter cooldown if no spawn position found
                    nextDoppelgangerAppearTime.put(player.getUuid(), currentTime + 300); // 15 second cooldown
                    MCMod.LOGGER.warn("Could not find peripheral spawn position for player {}", player.getName().getString());
                    player.sendMessage(Text.literal("§cDebug: Could not find spawn position"), false);
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

    private static boolean isLookingAt(ServerPlayerEntity player, net.minecraft.entity.Entity entity, double thresholdDegrees) {
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
            // Kill the player - use a simpler approach for 1.21.7
            try {
                // Try to use reflection to find the correct damage method
                java.lang.reflect.Method damageMethod = ServerPlayerEntity.class.getDeclaredMethod("damage", ServerWorld.class, DamageSource.class, float.class);
                damageMethod.setAccessible(true);
                damageMethod.invoke(player, (ServerWorld) player.getWorld(), player.getWorld().getDamageSources().generic(), Float.MAX_VALUE);
                MCMod.LOGGER.info("Player killed in basement finale");
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
            // Set a cooldown before the next spawn
            long currentTime = player.getWorld().getTime();
            nextDoppelgangerAppearTime.put(player.getUuid(), currentTime + 1200); // 60 second cooldown
            return;
        }
        
        Vec3d playerPos = player.getPos();
        Vec3d doppelPos = doppelganger.getPos();
        double distance = playerPos.distanceTo(doppelPos);
        
        // Check if player is looking at the doppelganger
        boolean isLookingAt = isLookingAt(player, doppelganger, 25.0); // 25 degree cone
        
        // If player is looking directly at doppelganger and close, the doppelganger will teleport away
        // (This is now handled in the DoppelgangerEntity.tick() method)
        
        // If player looks away and doppelganger is close, damage them
        if (!isLookingAt && distance < 3.0) {
            long now = player.getWorld().getTime();
            long lastDamage = lastDamageTime.getOrDefault(player.getUuid(), 0L);
            if (now - lastDamage > 60) { // Damage every 3 seconds
                // Try to use reflection to find the correct damage method
                try {
                    java.lang.reflect.Method damageMethod = ServerPlayerEntity.class.getDeclaredMethod("damage", ServerWorld.class, DamageSource.class, float.class);
                    damageMethod.setAccessible(true);
                    damageMethod.invoke(player, (ServerWorld) player.getWorld(), player.getWorld().getDamageSources().generic(), 2.0f);
                    MCMod.LOGGER.info("Player damaged by doppelganger");
                } catch (Exception e) {
                    MCMod.LOGGER.warn("Could not damage player: " + e.getMessage());
                }
                lastDamageTime.put(player.getUuid(), now);
                // Play damage sound
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_damage")), SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }
        
        // The doppelganger now handles its own teleportation in its tick() method
        // No need for additional teleport logic here
    }
} 
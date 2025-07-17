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

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Random;
import java.util.HashMap;
import java.util.Optional;

public class HauntingManager {
    private static UUID hauntedPlayerId = null;
    private static final WeakHashMap<ServerPlayerEntity, DoppelgangerEntity> doppelgangers = new WeakHashMap<>();
    private static final Random random = new Random();
    private static final HashMap<UUID, Long> hauntingStartTime = new HashMap<>();
    private static final HashMap<UUID, Boolean> entityCanEnterHome = new HashMap<>();
    private static final HashMap<UUID, Long> lastKnockTime = new HashMap<>();
    private static final HashMap<UUID, Boolean> basementBuilt = new HashMap<>();

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
        BlockPos bed = player.getSpawnPointPosition();
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
            if (door != null && isDoorOpen(player.getWorld(), door)) {
                entityCanEnterHome.put(player.getUuid(), true);
                // Play demon laugh sound for the haunted player
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_laugh")), SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }
        // Doppelganger spawn logic
        if (entityCanEnterHome.getOrDefault(player.getUuid(), false)) {
            DoppelgangerEntity doppelganger = doppelgangers.get(player);
            if (doppelganger == null) {
                BlockPos spawnPos = getHomeSpawnPos(player);
                if (spawnPos != null) {
                    GameProfile profile = player.getGameProfile();
                    HauntPackets.sendSpawnDoppelganger(player, new Vec3d(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5), profile.getName());
                    doppelgangers.put(player, null);
                    // Play ghostly whispers sound for the haunted player
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_whisper")), SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
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

    private static boolean isLookingAt(ServerPlayerEntity player, DoppelgangerEntity entity, double thresholdDegrees) {
        Vec3d playerLook = player.getRotationVec(1.0f);
        Vec3d toEntity = entity.getPos().subtract(player.getPos()).normalize();
        double dot = playerLook.dotProduct(toEntity);
        double angle = Math.acos(dot) * (180.0 / Math.PI);
        return angle < thresholdDegrees;
    }

    private static BlockPos findNearestDoor(ServerPlayerEntity player) {
        BlockPos bed = player.getSpawnPointPosition();
        if (bed == null) return null;
        ServerWorld world = player.getWorld();
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
        BlockPos bed = player.getSpawnPointPosition();
        if (bed == null) return null;
        ServerWorld world = player.getWorld();
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
        BlockPos bed = player.getSpawnPointPosition();
        if (bed == null) return;
        ServerWorld world = player.getWorld();
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
            // New sign API: setText(List<Text>)
            sign.setTextOnRow(Direction.NORTH, 0, Text.literal("Welcome home."));
            sign.setTextOnRow(Direction.NORTH, 1, Text.literal("You found me."));
            sign.setTextOnRow(Direction.NORTH, 2, Text.empty());
            sign.setTextOnRow(Direction.NORTH, 3, Text.empty());
        }
        basementBuilt.put(player.getUuid(), true);
    }

    private static void placeLureSigns(ServerPlayerEntity player) {
        BlockPos bed = player.getSpawnPointPosition();
        if (bed == null) return;
        ServerWorld world = player.getWorld();
        // Place a sign near the bed
        BlockPos signPos = bed.add(1, 0, 0);
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState());
        SignBlockEntity sign = (SignBlockEntity) world.getBlockEntity(signPos);
        if (sign != null) {
            sign.setTextOnRow(Direction.NORTH, 0, Text.literal("Come downstairs..."));
            sign.setTextOnRow(Direction.NORTH, 1, Text.literal("I have something for you..."));
            sign.setTextOnRow(Direction.NORTH, 2, Text.empty());
            sign.setTextOnRow(Direction.NORTH, 3, Text.empty());
        }
    }

    private static void checkBasementFinale(ServerPlayerEntity player) {
        BlockPos bed = player.getSpawnPointPosition();
        if (bed == null) return;
        BlockPos stairStart = bed.down();
        BlockPos roomOrigin = stairStart.down(4).add(-2, 0, -2);
        BlockPos playerPos = player.getBlockPos();
        // If player is inside the basement room
        if (playerPos.getY() >= roomOrigin.getY() && playerPos.getY() < roomOrigin.getY() + 3 &&
            playerPos.getX() >= roomOrigin.getX() && playerPos.getX() < roomOrigin.getX() + 5 &&
            playerPos.getZ() >= roomOrigin.getZ() && playerPos.getZ() < roomOrigin.getZ() + 5) {
            // Kill the player
            player.damage(player.getWorld().getDamageSources().magic(), Float.MAX_VALUE);
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
} 
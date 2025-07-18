package com.example.mcmod;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.inventory.Inventory;

import java.util.List;

public class RitualHandler {
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            Direction facing = player.getHorizontalFacing();

            // Check if player is placing a candle on a netherrack block
            if (block == Blocks.NETHERRACK && isCandle(player.getMainHandStack().getItem())) {
                // Chest must be to the right of the netherrack block (relative to player facing)
                Direction right = facing.rotateYClockwise();
                BlockPos chestPos = pos.offset(right);
                Block chestBlock = world.getBlockState(chestPos).getBlock();
                if (chestBlock != Blocks.CHEST) return ActionResult.PASS;

                // Check chest for paper with a player's name
                BlockEntity be = world.getBlockEntity(chestPos);
                if (!(be instanceof ChestBlockEntity chest)) return ActionResult.PASS;
                
                // Use the inventory interface
                Inventory inv = chest;
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (stack.getItem() == Items.PAPER && !stack.getName().equals(stack.getItem().getName(stack))) {
                        String targetName = stack.getName().getString();
                        // Try to find the player by name
                        MinecraftServer server = player.getServer();
                        if (server != null) {
                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
                            if (target != null) {
                                // Ritual complete! Consume the paper and trigger haunting
                                stack.decrement(1);
                                MCMod.LOGGER.info("Ritual complete! Haunting triggered for {} by {}", targetName, player.getName().getString());
                                player.sendMessage(Text.literal("§4The ritual is complete. The haunting begins..."), false);
                                target.sendMessage(Text.literal("§8You feel a chill run down your spine..."), false);
                                HauntingManager.startHaunting(target);
                                // Play demon laugh sound only for the haunted player
                                target.getWorld().playSound(null, target.getBlockPos(), SoundEvent.of(Identifier.of("mcmod", "haunt_laugh")), SoundCategory.PLAYERS, 1.0f, 1.0f);
                                return ActionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static boolean isCandle(net.minecraft.item.Item item) {
        // Check for all vanilla candle types
        return item == net.minecraft.item.Items.CANDLE
            || item == net.minecraft.item.Items.WHITE_CANDLE
            || item == net.minecraft.item.Items.ORANGE_CANDLE
            || item == net.minecraft.item.Items.MAGENTA_CANDLE
            || item == net.minecraft.item.Items.LIGHT_BLUE_CANDLE
            || item == net.minecraft.item.Items.YELLOW_CANDLE
            || item == net.minecraft.item.Items.LIME_CANDLE
            || item == net.minecraft.item.Items.PINK_CANDLE
            || item == net.minecraft.item.Items.GRAY_CANDLE
            || item == net.minecraft.item.Items.LIGHT_GRAY_CANDLE
            || item == net.minecraft.item.Items.CYAN_CANDLE
            || item == net.minecraft.item.Items.PURPLE_CANDLE
            || item == net.minecraft.item.Items.BLUE_CANDLE
            || item == net.minecraft.item.Items.BROWN_CANDLE
            || item == net.minecraft.item.Items.GREEN_CANDLE
            || item == net.minecraft.item.Items.RED_CANDLE
            || item == net.minecraft.item.Items.BLACK_CANDLE;
    }
} 
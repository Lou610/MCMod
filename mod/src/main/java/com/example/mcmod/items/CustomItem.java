package com.example.mcmod.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CustomItem extends Item {
    public CustomItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        if (!world.isClient) {
            // Teleport the player 10 blocks in the direction they're looking
            BlockPos pos = user.getBlockPos();
            BlockPos targetPos = pos.add(user.getHorizontalFacing().getVector().multiply(10));
            
            // Make sure the target position is safe (not inside blocks)
            while (world.getBlockState(targetPos).isSolidBlock(world, targetPos) && 
                   targetPos.getY() < world.getTopY()) {
                targetPos = targetPos.up();
            }
            
            if (user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.teleport(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
                user.sendMessage(Text.literal("§bYou have been teleported!"), false);
            }
        }
        
        return TypedActionResult.success(itemStack, world.isClient());
    }
} 
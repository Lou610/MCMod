package com.example.mcmod.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class CustomItem extends Item {
    public CustomItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity user = context.getPlayer();
        
        if (!world.isClient && user != null) {
            // Teleport the player 10 blocks in the direction they're looking
            BlockPos pos = user.getBlockPos();
            BlockPos targetPos = pos.add(user.getHorizontalFacing().getVector().multiply(10));
            
            // Make sure the target position is safe (not inside blocks)
            while (world.getBlockState(targetPos).isSolidBlock(world, targetPos) && 
                   targetPos.getY() < world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, targetPos.getX(), targetPos.getZ())) {
                targetPos = targetPos.up();
            }
            
            if (user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.teleport(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, true);
                user.sendMessage(Text.literal("§bYou have been teleported!"), false);
            }
        }
        
        return ActionResult.SUCCESS;
    }
} 
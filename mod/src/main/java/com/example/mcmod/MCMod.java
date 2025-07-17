package com.example.mcmod;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.items.CustomItem;
import com.example.mcmod.RitualHandler;
import com.example.mcmod.HauntingManager;
import com.example.mcmod.command.AskCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class MCMod implements ModInitializer {
    public static final String MOD_ID = "mcmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Custom item
    public static final Item CUSTOM_ITEM = new CustomItem(new Item.Settings());

    // Custom block
    public static final Block CUSTOM_BLOCK = new Block(Block.Settings.create().mapColor(MapColor.IRON_GRAY).strength(4.0f).requiresTool());

    // Block item
    public static final BlockItem CUSTOM_BLOCK_ITEM = new BlockItem(CUSTOM_BLOCK, new Item.Settings());

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MC Mod!");

        // Register /ask command
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AskCommand.register(dispatcher);
        });

        // Register custom entities
        ModEntities.register();

        // Register the custom item
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "custom_item"), CUSTOM_ITEM);
        
        // Register the custom block
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "custom_block"), CUSTOM_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "custom_block"), CUSTOM_BLOCK_ITEM);
        
        // Add items to the vanilla Building Blocks creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(CUSTOM_ITEM);
            entries.add(CUSTOM_BLOCK_ITEM);
        });

        // Register ritual handler
        RitualHandler.register();

        // Register server tick event for haunting
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HauntingManager.tick(server);
        });

        LOGGER.info("MC Mod initialized successfully!");
    }
} 
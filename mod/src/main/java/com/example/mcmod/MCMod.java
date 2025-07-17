package com.example.mcmod;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.items.CustomItem;
import com.example.mcmod.rituals.RitualHandler;
import com.example.mcmod.haunting.HauntingManager;
import com.example.mcmod.command.AskCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCMod implements ModInitializer {
    public static final String MOD_ID = "mcmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Custom item
    public static final Item CUSTOM_ITEM = new CustomItem(new FabricItemSettings());

    // Custom block
    public static final Block CUSTOM_BLOCK = new Block(FabricBlockSettings.of(Material.METAL)
            .strength(4.0f)
            .requiresTool());

    // Block item
    public static final BlockItem CUSTOM_BLOCK_ITEM = new BlockItem(CUSTOM_BLOCK, new FabricItemSettings());

    // Item group
    public static final ItemGroup CUSTOM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(CUSTOM_ITEM))
            .displayName(Text.translatable("itemGroup.mcmod.custom_group"))
            .entries((context, entries) -> {
                entries.add(CUSTOM_ITEM);
                entries.add(CUSTOM_BLOCK_ITEM);
            })
            .build();

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
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "custom_item"), CUSTOM_ITEM);
        
        // Register the custom block
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "custom_block"), CUSTOM_BLOCK);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "custom_block"), CUSTOM_BLOCK_ITEM);
        
        // Register the item group
        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "custom_group"), CUSTOM_GROUP);

        // Register ritual handler
        RitualHandler.register();

        // Register server tick event for haunting
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HauntingManager.tick(server);
        });

        LOGGER.info("MC Mod initialized successfully!");
    }
} 
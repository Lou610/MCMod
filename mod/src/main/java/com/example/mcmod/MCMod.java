package com.example.mcmod;

import com.example.mcmod.entity.ModEntities;
import com.example.mcmod.RitualHandler;
import com.example.mcmod.HauntingManager;
import com.example.mcmod.command.AskCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCMod implements ModInitializer {
    public static final String MOD_ID = "mcmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MCMod!");

        // Register /ask command
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AskCommand.register(dispatcher);
        });

        // Register custom entities
        ModEntities.register();

        // Register ritual handler
        RitualHandler.register();

        // Register server tick event for haunting
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HauntingManager.tick(server);
        });

        LOGGER.info("MCMod initialized successfully!");
    }
} 
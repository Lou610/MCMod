package com.example.mcmod.entity;

import com.example.mcmod.MCMod;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class ModEntities {
    public static EntityType<DoppelgangerEntity> DOPPELGANGER;

    public static final Identifier DOPPELGANGER_ID = Identifier.of(MCMod.MOD_ID, "doppelganger");
    public static final RegistryKey<EntityType<?>> DOPPELGANGER_KEY = 
        RegistryKey.of(RegistryKeys.ENTITY_TYPE, DOPPELGANGER_ID);

    public static void register() {
        // Register the doppelganger entity using the correct method for 1.21.7
        DOPPELGANGER = EntityType.Builder
            .create(DoppelgangerEntity::new, net.minecraft.entity.SpawnGroup.MISC)
            .dimensions(0.6f, 1.8f)
            .build(DOPPELGANGER_KEY);
        
        // Register using the new method for 1.21.7
        net.minecraft.registry.Registry.register(
            Registries.ENTITY_TYPE,
            DOPPELGANGER_ID,
            DOPPELGANGER
        );

        // Register default attributes for the doppelganger entity
        FabricDefaultAttributeRegistry.register(DOPPELGANGER, DoppelgangerEntity.createMobAttributes());
        
        MCMod.LOGGER.info("Doppelganger entity registered successfully with ID: {}", DOPPELGANGER_ID);
    }
} 
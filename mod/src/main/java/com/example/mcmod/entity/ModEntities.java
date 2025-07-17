package com.example.mcmod.entity;

import com.example.mcmod.MCMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class ModEntities {
    public static EntityType<DoppelgangerEntity> DOPPELGANGER;

    public static final RegistryKey<EntityType<?>> DOPPELGANGER_KEY =
        RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MCMod.MOD_ID, "doppelganger"));

    public static void register() {
        DOPPELGANGER = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MCMod.MOD_ID, "doppelganger"),
            EntityType.Builder.create(DoppelgangerEntity::new, SpawnGroup.MONSTER)
                .build("doppelganger")
        );
        
        // Register default attributes for the doppelganger
        FabricDefaultAttributeRegistry.register(DOPPELGANGER, DoppelgangerEntity.createMobAttributes());
    }
} 
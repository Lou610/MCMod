package com.example.mcmod.entity;

import com.example.mcmod.MCMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModEntities {
    public static EntityType<DoppelgangerEntity> DOPPELGANGER;

    public static void register() {
        DOPPELGANGER = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(MCMod.MOD_ID, "doppelganger"),
            EntityType.Builder.create(DoppelgangerEntity::new, SpawnGroup.MONSTER)
                .setDimensions(0.6F, 1.8F)
                .build()
        );
    }
} 
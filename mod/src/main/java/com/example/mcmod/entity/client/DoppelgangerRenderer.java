package com.example.mcmod.entity.client;

import com.example.mcmod.entity.DoppelgangerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class DoppelgangerRenderer extends EntityRenderer<DoppelgangerEntity> {
    public DoppelgangerRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(DoppelgangerEntity entity) {
        return Identifier.of("minecraft", "textures/entity/steve.png");
    }
} 
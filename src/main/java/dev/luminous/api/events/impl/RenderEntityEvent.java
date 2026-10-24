/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 */
package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

public class RenderEntityEvent
extends Event {
    private static final RenderEntityEvent INSTANCE = new RenderEntityEvent();
    private Entity entity;
    private EntityType<?> entityType;

    private RenderEntityEvent() {
    }

    public static RenderEntityEvent get(Entity entity) {
        RenderEntityEvent.INSTANCE.entity = entity;
        RenderEntityEvent.INSTANCE.entityType = null;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public static RenderEntityEvent get(EntityType<?> entityType) {
        RenderEntityEvent.INSTANCE.entity = null;
        RenderEntityEvent.INSTANCE.entityType = entityType;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public EntityType<?> getEntityType() {
        return this.entityType;
    }
}


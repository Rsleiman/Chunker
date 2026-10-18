package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.entity.EntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for mobs, dropped items, experience orbs, vehicles and projectiles stored as a GenericEntity.
 */
public class JavaGenericEntityHandler extends EntityHandler<JavaResolvers, CompoundTag, GenericEntity> {
    public JavaGenericEntityHandler() {
        super(null, GenericEntity.class, GenericEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull GenericEntity value) {
        value.setEntityType(resolvers.entityResolver().getKey(input).orElseThrow());
        if (GenericEntityTypes.isLiving(value.getEntityType())) {
            JavaLivingEntityData.read(resolvers, input, value);
        }
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull GenericEntity value) {
        if (GenericEntityTypes.isLiving(value.getEntityType())) {
            JavaLivingEntityData.write(resolvers, output, value);
        }
    }
}

package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.entity.DoNotWriteEntityHandler;
import com.hivemc.chunker.conversion.encoding.base.resolver.entity.EntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Handler which reads mobs, dropped items, experience orbs, vehicles and projectiles into a GenericEntity.
 * Writing these to Bedrock isn't supported, they are only written when the original NBT is copied (Bedrock to Bedrock).
 */
public class BedrockGenericEntityHandler extends EntityHandler<BedrockResolvers, CompoundTag, GenericEntity> implements DoNotWriteEntityHandler<GenericEntity> {
    public BedrockGenericEntityHandler() {
        super(null, GenericEntity.class, GenericEntity::new);
    }

    @Override
    public void read(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag input, @NotNull GenericEntity value) {
        value.setEntityType(resolvers.entityResolver().getKey(input).orElseThrow());
        if (GenericEntityTypes.isLiving(value.getEntityType())) {
            BedrockLivingEntityReader.read(resolvers, input, value);
        }
    }

    @Override
    public void write(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag output, @NotNull GenericEntity value) {
        // Only the original NBT is written (by the base handler)
    }

    @Override
    public boolean shouldRemoveBeforeWrite(ChunkerColumn column, GenericEntity entity) {
        return entity.getOriginalNBT() == null;
    }
}

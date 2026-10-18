package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.entity.EntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.entity.CushionEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for writing/reading Cushions.
 */
public class JavaCushionEntityHandler extends EntityHandler<JavaResolvers, CompoundTag, CushionEntity> {
    public JavaCushionEntityHandler() {
        super(ChunkerVanillaEntityType.CUSHION, CushionEntity.class, CushionEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull CushionEntity value) {
        value.setColor(input.getOptionalValue("color", String.class).flatMap(ChunkerDyeColor::getColorByName).orElse(ChunkerDyeColor.WHITE));
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull CushionEntity value) {
        output.put("color", value.getColor().getName());

        // A cushion is attached to the block it was placed in, which is the block it occupies
        output.put("block_pos", new int[]{
                (int) Math.floor(value.getPositionX()),
                (int) Math.floor(value.getPositionY()),
                (int) Math.floor(value.getPositionZ())
        });
    }
}

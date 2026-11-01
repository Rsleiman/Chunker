package com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.legacy.handlers.JavaLegacyFurnaceBlockEntityHandler;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.BlastFurnaceBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.FurnaceBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.SmokerBlockEntity;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for Furnace Block Entities.
 */
public class JavaFurnaceBlockEntityHandler extends BlockEntityHandler<JavaResolvers, CompoundTag, FurnaceBlockEntity> {
    public JavaFurnaceBlockEntityHandler() {
        super("minecraft:furnace", FurnaceBlockEntity.class, FurnaceBlockEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull FurnaceBlockEntity value) {
        if (input.contains("lit_time_remaining")) {
            value.setBurnTime(JavaLegacyFurnaceBlockEntityHandler.getShortOrInt(input, "lit_time_remaining", (short) 0));
        } else {
            value.setBurnTime(JavaLegacyFurnaceBlockEntityHandler.getShortOrInt(input, "BurnTime", (short) 0));
        }
        if (input.contains("cooking_time_spent")) {
            value.setCookTime(JavaLegacyFurnaceBlockEntityHandler.getShortOrInt(input, "cooking_time_spent", (short) 0));
        } else {
            value.setCookTime(JavaLegacyFurnaceBlockEntityHandler.getShortOrInt(input, "CookTime", (short) 0));
        }
        if (input.contains("lit_total_time")) {
            value.setCookTimeTotal(JavaLegacyFurnaceBlockEntityHandler.getShortOrInt(input, "lit_total_time", (short) 0));
        } else {
            value.setCookTimeTotal(JavaLegacyFurnaceBlockEntityHandler.getShortOrInt(input, "CookTimeTotal", (short) 0));
        }
    }

    /**
     * Get the time an item takes to cook, which Bedrock doesn't save. Without it Java never finishes the item being
     * cooked, as it finishes an item when the time spent equals the total. Every vanilla recipe takes 200 ticks in a furnace and 100 in a smoker or blast furnace.
     *
     * @param value the furnace.
     * @return the total cooking time in ticks.
     */
    private static short getCookingTotalTime(FurnaceBlockEntity value) {
        return (short) (value instanceof SmokerBlockEntity || value instanceof BlastFurnaceBlockEntity ? 100 : 200);
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull FurnaceBlockEntity value) {
        // In 1.21.4 the fields for the furnace burn times got renamed
        if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 21, 4)) {

            // 26.3.0 changed these to integers
            if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(26, 3, 0)) {
                output.put("lit_time_remaining", (int) value.getBurnTime());
                output.put("cooking_time_spent", (int) value.getCookTime());
                output.put("lit_total_time", (int) value.getCookTimeTotal());
                output.put("cooking_total_time", (int) getCookingTotalTime(value));
            } else {
                output.put("lit_time_remaining", value.getBurnTime());
                output.put("cooking_time_spent", value.getCookTime());
                output.put("lit_total_time", value.getCookTimeTotal());
                output.put("cooking_total_time", getCookingTotalTime(value));
            }
        } else {
            output.put("BurnTime", value.getBurnTime());
            output.put("CookTime", value.getCookTime());
            output.put("CookTimeTotal", value.getCookTimeTotal());
        }
    }
}

package com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.TrialSpawnerBlockEntity;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for trial spawners. The configurations are written as IDs of the trial_spawner registry, which Java has
 * from 1.21.2; before that they could only be inline and are left out. A spawner without a configuration has no mobs
 * to spawn and stays inactive.
 */
public class JavaTrialSpawnerBlockEntityHandler extends BlockEntityHandler<JavaResolvers, CompoundTag, TrialSpawnerBlockEntity> {
    public JavaTrialSpawnerBlockEntityHandler() {
        super("minecraft:trial_spawner", TrialSpawnerBlockEntity.class, TrialSpawnerBlockEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull TrialSpawnerBlockEntity value) {
        // Inline configurations aren't kept
        getString(input, "normal_config").ifPresent(value::setNormalConfig);
        getString(input, "ominous_config").ifPresent(value::setOminousConfig);
        getNumber(input, "required_player_range").ifPresent(range -> value.setRequiredPlayerRange(range.intValue()));
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull TrialSpawnerBlockEntity value) {
        if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 21, 2)) {
            if (value.getNormalConfig() != null) {
                output.put("normal_config", value.getNormalConfig());
            }
            if (value.getOminousConfig() != null) {
                output.put("ominous_config", value.getOminousConfig());
            }
        }
        if (value.getRequiredPlayerRange() != null) {
            output.put("required_player_range", Math.max(1, Math.min(128, value.getRequiredPlayerRange())));
        }
    }

    private static Optional<String> getString(CompoundTag tag, String name) {
        Tag<?> value = tag.get(name);
        return value != null && value.getBoxedValue() instanceof String string ? Optional.of(string) : Optional.empty();
    }

    private static Optional<Number> getNumber(CompoundTag tag, String name) {
        Tag<?> value = tag.get(name);
        return value != null && value.getBoxedValue() instanceof Number number ? Optional.of(number) : Optional.empty();
    }
}

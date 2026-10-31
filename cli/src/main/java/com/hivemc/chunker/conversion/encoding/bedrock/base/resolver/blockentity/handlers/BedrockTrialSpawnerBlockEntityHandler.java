package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.TrialSpawnerBlockEntity;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for trial spawners. Bedrock names the configurations with the same IDs as Java's trial_spawner registry
 * (e.g. "minecraft:trial_chamber/breeze/normal"). Only reading is supported, as the Bedrock versions which store
 * these fields are unverified.
 */
public class BedrockTrialSpawnerBlockEntityHandler extends BlockEntityHandler<BedrockResolvers, CompoundTag, TrialSpawnerBlockEntity> {
    public BedrockTrialSpawnerBlockEntityHandler() {
        super("TrialSpawner", TrialSpawnerBlockEntity.class, TrialSpawnerBlockEntity::new);
    }

    @Override
    public void read(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag input, @NotNull TrialSpawnerBlockEntity value) {
        getString(input, "normal_config").filter(config -> !config.isEmpty()).ifPresent(value::setNormalConfig);
        getString(input, "ominous_config").filter(config -> !config.isEmpty()).ifPresent(value::setOminousConfig);
        getNumber(input, "required_player_range").ifPresent(range -> value.setRequiredPlayerRange(range.intValue()));
    }

    @Override
    public void write(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag output, @NotNull TrialSpawnerBlockEntity value) {
        // Not written (see the class description)
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

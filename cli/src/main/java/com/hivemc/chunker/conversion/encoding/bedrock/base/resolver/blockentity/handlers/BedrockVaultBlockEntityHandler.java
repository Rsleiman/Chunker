package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.VaultBlockEntity;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for vaults, which reads the configuration (key item, loot table and ranges). Only reading is supported, as
 * the Bedrock versions which store these fields are unverified.
 */
public class BedrockVaultBlockEntityHandler extends BlockEntityHandler<BedrockResolvers, CompoundTag, VaultBlockEntity> {
    public BedrockVaultBlockEntityHandler() {
        super("Vault", VaultBlockEntity.class, VaultBlockEntity::new);
    }

    @Override
    public void read(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag input, @NotNull VaultBlockEntity value) {
        CompoundTag config = input.getCompound("config");
        if (config == null) return;

        CompoundTag keyItem = config.getCompound("key_item");
        if (keyItem != null) {
            value.setKeyItem(resolvers.readItem(keyItem));
        }
        getString(config, "loot_table")
                .filter(lootTable -> !lootTable.isEmpty())
                .ifPresent(lootTable -> value.setLootTable(BedrockRandomizableContainerBlockEntityHandler.bedrockLootTableToChunker(lootTable)));
        getString(config, "override_loot_table_to_display")
                .filter(lootTable -> !lootTable.isEmpty())
                .ifPresent(lootTable -> value.setOverrideLootTableToDisplay(BedrockRandomizableContainerBlockEntityHandler.bedrockLootTableToChunker(lootTable)));
        getNumber(config, "activation_range").ifPresent(range -> value.setActivationRange(range.doubleValue()));
        getNumber(config, "deactivation_range").ifPresent(range -> value.setDeactivationRange(range.doubleValue()));
    }

    @Override
    public void write(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag output, @NotNull VaultBlockEntity value) {
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

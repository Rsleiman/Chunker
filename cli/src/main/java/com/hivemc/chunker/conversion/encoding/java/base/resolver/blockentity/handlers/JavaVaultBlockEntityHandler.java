package com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.VaultBlockEntity;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for vaults, which keeps the configuration (key item, loot table and ranges). Without one, Java uses the
 * normal vault's (a trial key and the normal reward loot table).
 */
public class JavaVaultBlockEntityHandler extends BlockEntityHandler<JavaResolvers, CompoundTag, VaultBlockEntity> {
    public JavaVaultBlockEntityHandler() {
        super("minecraft:vault", VaultBlockEntity.class, VaultBlockEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull VaultBlockEntity value) {
        CompoundTag config = input.getCompound("config");
        if (config == null) return;

        CompoundTag keyItem = config.getCompound("key_item");
        if (keyItem != null) {
            value.setKeyItem(resolvers.readItem(keyItem));
        }
        getString(config, "loot_table").ifPresent(value::setLootTable);
        getString(config, "override_loot_table_to_display").ifPresent(value::setOverrideLootTableToDisplay);
        getNumber(config, "activation_range").ifPresent(range -> value.setActivationRange(range.doubleValue()));
        getNumber(config, "deactivation_range").ifPresent(range -> value.setDeactivationRange(range.doubleValue()));
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull VaultBlockEntity value) {
        CompoundTag config = new CompoundTag();
        if (value.getKeyItem() != null && !value.getKeyItem().getIdentifier().isAir()) {
            resolvers.writeItem(value.getKeyItem()).ifPresent(keyItem -> config.put("key_item", keyItem));
        }
        if (value.getLootTable() != null) {
            config.put("loot_table", value.getLootTable());
        }
        if (value.getOverrideLootTableToDisplay() != null) {
            config.put("override_loot_table_to_display", value.getOverrideLootTableToDisplay());
        }
        if (value.getActivationRange() != null) {
            config.put("activation_range", value.getActivationRange().doubleValue());
        }
        if (value.getDeactivationRange() != null) {
            config.put("deactivation_range", value.getDeactivationRange().doubleValue());
        }
        if (config.size() > 0) {
            output.put("config", config);
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

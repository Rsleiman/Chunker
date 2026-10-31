package com.hivemc.chunker.conversion.intermediate.column.blockentity;

import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a Vault Block Entity.
 */
public class VaultBlockEntity extends BlockEntity {
    @Nullable
    private ChunkerItemStack keyItem;
    @Nullable
    private String lootTable;
    @Nullable
    private Double activationRange;
    @Nullable
    private Double deactivationRange;
    @Nullable
    private String overrideLootTableToDisplay;

    /**
     * Get the item which unlocks the vault.
     *
     * @return the key item, null if the default (a trial key) should be used.
     */
    @Nullable
    public ChunkerItemStack getKeyItem() {
        return keyItem;
    }

    /**
     * Set the item which unlocks the vault.
     *
     * @param keyItem the key item, null if the default should be used.
     */
    public void setKeyItem(@Nullable ChunkerItemStack keyItem) {
        this.keyItem = keyItem;
    }

    /**
     * Get the loot table the vault ejects.
     *
     * @return the loot table (Java format, e.g. "minecraft:chests/trial_chambers/reward"), null if the default
     * should be used.
     */
    @Nullable
    public String getLootTable() {
        return lootTable;
    }

    /**
     * Set the loot table the vault ejects.
     *
     * @param lootTable the loot table (Java format), null if the default should be used.
     */
    public void setLootTable(@Nullable String lootTable) {
        this.lootTable = lootTable;
    }

    /**
     * Get the distance at which a player activates the vault.
     *
     * @return the distance in blocks, null if the default should be used.
     */
    @Nullable
    public Double getActivationRange() {
        return activationRange;
    }

    /**
     * Set the distance at which a player activates the vault.
     *
     * @param activationRange the distance in blocks, null if the default should be used.
     */
    public void setActivationRange(@Nullable Double activationRange) {
        this.activationRange = activationRange;
    }

    /**
     * Get the distance at which the vault deactivates when no player is closer.
     *
     * @return the distance in blocks, null if the default should be used.
     */
    @Nullable
    public Double getDeactivationRange() {
        return deactivationRange;
    }

    /**
     * Set the distance at which the vault deactivates when no player is closer.
     *
     * @param deactivationRange the distance in blocks, null if the default should be used.
     */
    public void setDeactivationRange(@Nullable Double deactivationRange) {
        this.deactivationRange = deactivationRange;
    }

    /**
     * Get the loot table whose items the vault displays instead of its own.
     *
     * @return the loot table (Java format), null if the vault displays its own loot table.
     */
    @Nullable
    public String getOverrideLootTableToDisplay() {
        return overrideLootTableToDisplay;
    }

    /**
     * Set the loot table whose items the vault displays instead of its own.
     *
     * @param overrideLootTableToDisplay the loot table (Java format), null if the vault displays its own.
     */
    public void setOverrideLootTableToDisplay(@Nullable String overrideLootTableToDisplay) {
        this.overrideLootTableToDisplay = overrideLootTableToDisplay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VaultBlockEntity that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getKeyItem(), that.getKeyItem()) && Objects.equals(getLootTable(), that.getLootTable()) && Objects.equals(getActivationRange(), that.getActivationRange()) && Objects.equals(getDeactivationRange(), that.getDeactivationRange()) && Objects.equals(getOverrideLootTableToDisplay(), that.getOverrideLootTableToDisplay());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getKeyItem(), getLootTable(), getActivationRange(), getDeactivationRange(), getOverrideLootTableToDisplay());
    }
}

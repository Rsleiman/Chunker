package com.hivemc.chunker.conversion.intermediate.column.blockentity;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a Trial Spawner Block Entity.
 */
public class TrialSpawnerBlockEntity extends BlockEntity {
    @Nullable
    private String normalConfig;
    @Nullable
    private String ominousConfig;
    @Nullable
    private Integer requiredPlayerRange;

    /**
     * Get the configuration used when the spawner isn't ominous.
     *
     * @return the ID of the trial spawner configuration (e.g. "minecraft:trial_chamber/breeze/normal"), null if unknown.
     */
    @Nullable
    public String getNormalConfig() {
        return normalConfig;
    }

    /**
     * Set the configuration used when the spawner isn't ominous.
     *
     * @param normalConfig the ID of the trial spawner configuration, null if unknown.
     */
    public void setNormalConfig(@Nullable String normalConfig) {
        this.normalConfig = normalConfig;
    }

    /**
     * Get the configuration used when the spawner is ominous.
     *
     * @return the ID of the trial spawner configuration (e.g. "minecraft:trial_chamber/breeze/ominous"), null if
     * unknown.
     */
    @Nullable
    public String getOminousConfig() {
        return ominousConfig;
    }

    /**
     * Set the configuration used when the spawner is ominous.
     *
     * @param ominousConfig the ID of the trial spawner configuration, null if unknown.
     */
    public void setOminousConfig(@Nullable String ominousConfig) {
        this.ominousConfig = ominousConfig;
    }

    /**
     * Get the range in blocks within which players activate the spawner.
     *
     * @return the range, null if the default should be used.
     */
    @Nullable
    public Integer getRequiredPlayerRange() {
        return requiredPlayerRange;
    }

    /**
     * Set the range in blocks within which players activate the spawner.
     *
     * @param requiredPlayerRange the range, null if the default should be used.
     */
    public void setRequiredPlayerRange(@Nullable Integer requiredPlayerRange) {
        this.requiredPlayerRange = requiredPlayerRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrialSpawnerBlockEntity that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getNormalConfig(), that.getNormalConfig()) && Objects.equals(getOminousConfig(), that.getOminousConfig()) && Objects.equals(getRequiredPlayerRange(), that.getRequiredPlayerRange());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNormalConfig(), getOminousConfig(), getRequiredPlayerRange());
    }
}

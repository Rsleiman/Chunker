package com.hivemc.chunker.conversion.intermediate.column.entity.data;

/**
 * The biome type of a villager or zombie villager.
 */
public enum ChunkerVillagerType {
    DESERT,
    JUNGLE,
    PLAINS,
    SAVANNA,
    SNOW,
    SWAMP,
    TAIGA;

    /**
     * Get the lowercase name of the type (as used by Java).
     *
     * @return the name of the type.
     */
    public String getName() {
        return name().toLowerCase();
    }
}

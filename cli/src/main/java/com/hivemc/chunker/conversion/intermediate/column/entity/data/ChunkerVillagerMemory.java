package com.hivemc.chunker.conversion.intermediate.column.entity.data;

/**
 * The points of interest a villager remembers.
 */
public enum ChunkerVillagerMemory {
    HOME,
    MEETING_POINT,
    JOB_SITE;

    /**
     * Get the lowercase name of the memory (as used by Java).
     *
     * @return the name of the memory.
     */
    public String getName() {
        return name().toLowerCase();
    }
}

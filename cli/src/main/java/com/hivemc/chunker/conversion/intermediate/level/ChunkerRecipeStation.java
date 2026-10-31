package com.hivemc.chunker.conversion.intermediate.level;

import org.jetbrains.annotations.Nullable;

/**
 * The block a recipe is made at. A Bedrock recipe and a Java recipe are the same recipe when they are made at the
 * same station and make the same item.
 */
public enum ChunkerRecipeStation {
    CRAFTING_TABLE("crafting_table"),
    STONECUTTER("stonecutter"),
    FURNACE("furnace"),
    BLAST_FURNACE("blast_furnace"),
    SMOKER("smoker"),
    CAMPFIRE("campfire"),
    SMITHING_TABLE("smithing_table");

    private final String name;

    ChunkerRecipeStation(String name) {
        this.name = name;
    }

    /**
     * Get a station by the name the recipe tables use.
     *
     * @param name the name.
     * @return the station or null if the name isn't known.
     */
    @Nullable
    public static ChunkerRecipeStation fromName(String name) {
        for (ChunkerRecipeStation station : values()) {
            if (station.name.equals(name)) return station;
        }
        return null;
    }

    /**
     * Get the name the recipe tables use.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }
}

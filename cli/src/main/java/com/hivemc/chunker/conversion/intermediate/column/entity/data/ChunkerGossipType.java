package com.hivemc.chunker.conversion.intermediate.column.entity.data;

/**
 * The kinds of gossip a villager keeps about a player (Java), which set the player's prices.
 */
public enum ChunkerGossipType {
    MAJOR_NEGATIVE(100),
    MINOR_NEGATIVE(200),
    MINOR_POSITIVE(25),
    MAJOR_POSITIVE(20),
    TRADING(25);

    private final int max;

    ChunkerGossipType(int max) {
        this.max = max;
    }

    /**
     * Get the lowercase name of the gossip type (as used by Java).
     *
     * @return the name of the gossip type.
     */
    public String getName() {
        return name().toLowerCase();
    }

    /**
     * Get the highest value Java keeps for this gossip type.
     *
     * @return the maximum value.
     */
    public int getMax() {
        return max;
    }
}

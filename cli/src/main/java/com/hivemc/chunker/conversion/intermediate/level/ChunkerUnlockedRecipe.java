package com.hivemc.chunker.conversion.intermediate.level;

import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;

/**
 * A recipe a player has unlocked, as the station it is made at and the item it makes. Recipe IDs differ between
 * Bedrock and Java, so this is what both sides have in common.
 *
 * @param station the station.
 * @param result  the item the recipe makes.
 */
public record ChunkerUnlockedRecipe(ChunkerRecipeStation station, ChunkerItemStack result) {
}

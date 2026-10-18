package com.hivemc.chunker.conversion.intermediate.column.entity.data;

import com.hivemc.chunker.conversion.intermediate.world.Dimension;

/**
 * A block position in a specific dimension (e.g. a villager's workstation).
 *
 * @param dimension the dimension of the position.
 * @param x         the X co-ordinate.
 * @param y         the Y co-ordinate.
 * @param z         the Z co-ordinate.
 */
public record ChunkerGlobalPosition(Dimension dimension, int x, int y, int z) {
}

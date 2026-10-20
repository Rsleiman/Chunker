package com.hivemc.chunker.conversion.intermediate.level;

import com.hivemc.chunker.conversion.intermediate.world.Dimension;

/**
 * The point where a player respawns.
 *
 * @param dimension the dimension of the respawn point.
 * @param x         the block X.
 * @param y         the block Y.
 * @param z         the block Z.
 * @param yaw       the yaw the player faces after respawning.
 * @param forced    true for a position set by a command, false for a bed or respawn anchor (which must still be
 *                  there and usable for the player to respawn at it).
 */
public record ChunkerRespawnPoint(Dimension dimension, int x, int y, int z, float yaw, boolean forced) {
}

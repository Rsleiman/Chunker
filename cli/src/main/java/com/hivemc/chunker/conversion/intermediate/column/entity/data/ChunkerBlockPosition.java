package com.hivemc.chunker.conversion.intermediate.column.entity.data;

/**
 * A block position referenced by an entity (e.g. a bee's hive).
 *
 * @param x the X co-ordinate.
 * @param y the Y co-ordinate.
 * @param z the Z co-ordinate.
 */
public record ChunkerBlockPosition(int x, int y, int z) {
}

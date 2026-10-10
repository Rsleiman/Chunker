package com.hivemc.chunker.conversion.intermediate.level;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * An axis aligned bounding box in block co-ordinates, both corners are inclusive.
 *
 * @param minX the minimum X.
 * @param minY the minimum Y.
 * @param minZ the minimum Z.
 * @param maxX the maximum X.
 * @param maxY the maximum Y.
 * @param maxZ the maximum Z.
 */
public record ChunkerBoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    /**
     * A deterministic ordering for boxes (by minY, minX, minZ and then the maximums).
     */
    public static final Comparator<ChunkerBoundingBox> ORDER = Comparator
            .comparingInt(ChunkerBoundingBox::minY)
            .thenComparingInt(ChunkerBoundingBox::minX)
            .thenComparingInt(ChunkerBoundingBox::minZ)
            .thenComparingInt(ChunkerBoundingBox::maxY)
            .thenComparingInt(ChunkerBoundingBox::maxX)
            .thenComparingInt(ChunkerBoundingBox::maxZ);

    /**
     * Create a bounding box from an array of [minX, minY, minZ, maxX, maxY, maxZ].
     *
     * @param values the array of six values.
     * @return the bounding box.
     */
    public static ChunkerBoundingBox fromArray(int[] values) {
        return new ChunkerBoundingBox(values[0], values[1], values[2], values[3], values[4], values[5]);
    }

    /**
     * Convert the box to an array of [minX, minY, minZ, maxX, maxY, maxZ].
     *
     * @return a new array of six values.
     */
    public int[] toArray() {
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    /**
     * Get the size of the box on the X axis.
     *
     * @return the number of blocks.
     */
    public int sizeX() {
        return maxX - minX + 1;
    }

    /**
     * Get the size of the box on the Y axis.
     *
     * @return the number of blocks.
     */
    public int sizeY() {
        return maxY - minY + 1;
    }

    /**
     * Get the size of the box on the Z axis.
     *
     * @return the number of blocks.
     */
    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    /**
     * Check whether this box intersects or is directly adjacent to another box.
     *
     * @param other the other box.
     * @return true if they intersect or touch.
     */
    public boolean intersectsOrTouches(ChunkerBoundingBox other) {
        return minX <= other.maxX + 1 && other.minX <= maxX + 1
                && minY <= other.maxY + 1 && other.minY <= maxY + 1
                && minZ <= other.maxZ + 1 && other.minZ <= maxZ + 1;
    }

    /**
     * Get the smallest box containing both boxes.
     *
     * @param other the other box.
     * @return the union of the two boxes.
     */
    public ChunkerBoundingBox union(ChunkerBoundingBox other) {
        return new ChunkerBoundingBox(
                Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ)
        );
    }

    /**
     * Expand the box in every direction.
     *
     * @param amount the number of blocks to expand by.
     * @return the inflated box.
     */
    public ChunkerBoundingBox inflate(int amount) {
        return new ChunkerBoundingBox(minX - amount, minY - amount, minZ - amount, maxX + amount, maxY + amount, maxZ + amount);
    }

    /**
     * Clip the box to the X/Z column of a chunk (Y is unchanged).
     *
     * @param chunkX the X of the chunk.
     * @param chunkZ the Z of the chunk.
     * @return the clipped box or null if the box does not overlap the chunk.
     */
    @Nullable
    public ChunkerBoundingBox clipToChunk(int chunkX, int chunkZ) {
        int clippedMinX = Math.max(minX, chunkX << 4);
        int clippedMaxX = Math.min(maxX, (chunkX << 4) + 15);
        int clippedMinZ = Math.max(minZ, chunkZ << 4);
        int clippedMaxZ = Math.min(maxZ, (chunkZ << 4) + 15);
        if (clippedMinX > clippedMaxX || clippedMinZ > clippedMaxZ) return null;
        return new ChunkerBoundingBox(clippedMinX, minY, clippedMinZ, clippedMaxX, maxY, clippedMaxZ);
    }
}

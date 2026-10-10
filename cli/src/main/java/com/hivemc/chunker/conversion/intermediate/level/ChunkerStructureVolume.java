package com.hivemc.chunker.conversion.intermediate.level;

import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The part of a generated structure that is stored for a single chunk. Every box is already clipped to the chunk which
 * stores the volume.
 */
public class ChunkerStructureVolume {
    private Dimension dimension;
    private final String structureId;
    private final ChunkCoordPair sourceChunk;
    private final @Nullable ChunkerBoundingBox root;
    private final List<ChunkerBoundingBox> pieces;

    /**
     * Create a new structure volume.
     *
     * @param dimension   the dimension the structure is inside.
     * @param structureId the Java-style identifier of the structure (e.g. minecraft:fortress).
     * @param sourceChunk the chunk which stored this volume.
     * @param root        the overall box of the structure clipped to the chunk (null if not known).
     * @param pieces      the boxes of the individual pieces clipped to the chunk.
     */
    public ChunkerStructureVolume(Dimension dimension, String structureId, ChunkCoordPair sourceChunk, @Nullable ChunkerBoundingBox root, List<ChunkerBoundingBox> pieces) {
        this.dimension = dimension;
        this.structureId = structureId;
        this.sourceChunk = sourceChunk;
        this.root = root;
        this.pieces = pieces;
    }

    /**
     * Pack a chunk position into a long, using the same packing as Java (ChunkPos.pack).
     *
     * @param chunkX the X of the chunk.
     * @param chunkZ the Z of the chunk.
     * @return the packed position.
     */
    public static long packChunk(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    /**
     * Get the X of a packed chunk position.
     *
     * @param packed the packed position.
     * @return the chunk X.
     */
    public static int unpackChunkX(long packed) {
        return (int) packed;
    }

    /**
     * Get the Z of a packed chunk position.
     *
     * @param packed the packed position.
     * @return the chunk Z.
     */
    public static int unpackChunkZ(long packed) {
        return (int) (packed >> 32);
    }

    /**
     * Get the dimension the structure is inside.
     *
     * @return the dimension.
     */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Set the dimension the structure is inside.
     *
     * @param dimension the dimension.
     */
    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    /**
     * Get the Java-style identifier of the structure.
     *
     * @return the identifier.
     */
    public String getStructureId() {
        return structureId;
    }

    /**
     * Get the chunk which stored this volume.
     *
     * @return the chunk co-ordinates.
     */
    public ChunkCoordPair getSourceChunk() {
        return sourceChunk;
    }

    /**
     * Get the overall box of the structure clipped to the source chunk.
     *
     * @return the root box or null if it is not known.
     */
    @Nullable
    public ChunkerBoundingBox getRoot() {
        return root;
    }

    /**
     * Get the piece boxes clipped to the source chunk.
     *
     * @return the list of pieces.
     */
    public List<ChunkerBoundingBox> getPieces() {
        return pieces;
    }

    @Override
    public String toString() {
        return "ChunkerStructureVolume{" +
                "dimension=" + dimension +
                ", structureId='" + structureId + '\'' +
                ", sourceChunk=" + sourceChunk +
                ", root=" + root +
                ", pieces=" + pieces +
                '}';
    }
}

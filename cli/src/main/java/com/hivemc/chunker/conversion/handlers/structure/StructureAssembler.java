package com.hivemc.chunker.conversion.handlers.structure;

import com.hivemc.chunker.conversion.intermediate.level.ChunkerBoundingBox;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerStructureVolume;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.*;

/**
 * Assembles the per-chunk structure volumes that Bedrock stores into structure starts as Java stores them (a start
 * is stored in a single chunk, the chunks it covers only hold references to it).
 */
public class StructureAssembler {
    /**
     * Java drops structure references that are further than this many chunks (chessboard distance) from the chunk.
     */
    public static final int MAX_REFERENCE_DISTANCE = 8;

    /**
     * The identifier of the structure which Java regenerates when it loads.
     */
    public static final String MONUMENT = "minecraft:monument";

    /**
     * The structures which adapt to the terrain, Java inflates their bounding box by 12 (Structure#adjustBoundingBox).
     */
    public static final Set<String> TERRAIN_ADAPTING = Set.of(
            "minecraft:pillager_outpost", "minecraft:village_desert", "minecraft:village_plains",
            "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga",
            "minecraft:ancient_city", "minecraft:trial_chambers", "minecraft:trail_ruins", "minecraft:bastion_remnant"
    );

    private static final int TERRAIN_ADAPTATION_INFLATE = 12;
    private static final int MONUMENT_SIZE = 58;
    private static final int MONUMENT_MIN_Y = 39;
    private static final int MONUMENT_MAX_Y = 61;

    /**
     * A piece of an assembled start.
     *
     * @param id    the identifier of the piece.
     * @param box   the bounding box of the piece.
     * @param extra the fields of the piece besides id and BB.
     */
    public record AssembledPiece(String id, ChunkerBoundingBox box, CompoundTag extra) {
    }

    /**
     * A structure start which is stored in a single chunk.
     *
     * @param id          the identifier of the structure.
     * @param chunkX      the X of the chunk the start is stored in.
     * @param chunkZ      the Z of the chunk the start is stored in.
     * @param pieces      the pieces of the start.
     * @param effectiveBox the box used to decide which chunks reference the start.
     */
    public record AssembledStart(String id, int chunkX, int chunkZ, List<AssembledPiece> pieces,
                                 ChunkerBoundingBox effectiveBox) {
    }

    /**
     * The result of assembling all the structures in a dimension, both maps are keyed by the packed chunk position.
     *
     * @param startsByChunk     the starts stored in a chunk by structure identifier.
     * @param referencesByChunk the packed positions of the starts a chunk references by structure identifier.
     */
    public record DimensionStructures(Map<Long, Map<String, AssembledStart>> startsByChunk,
                                      Map<Long, Map<String, LongSet>> referencesByChunk) {
        /**
         * Check whether there is nothing to write.
         *
         * @return true if there are no starts.
         */
        public boolean isEmpty() {
            return startsByChunk.isEmpty();
        }
    }

    /**
     * The part of a structure inside a single chunk.
     */
    private static class Unit {
        final int chunkX;
        final int chunkZ;
        final ChunkerBoundingBox box;
        final ChunkerBoundingBox root;
        List<ChunkerBoundingBox> pieces;

        Unit(ChunkerStructureVolume volume, ChunkerBoundingBox box) {
            this.chunkX = volume.getSourceChunk().chunkX();
            this.chunkZ = volume.getSourceChunk().chunkZ();
            this.box = box;
            this.root = volume.getRoot();
            this.pieces = new ArrayList<>(volume.getPieces());
        }

        long packed() {
            return ChunkerStructureVolume.packChunk(chunkX, chunkZ);
        }
    }

    /**
     * Assemble all the structures of a dimension.
     *
     * @param volumes        the volumes in the dimension (can be of any structure).
     * @param writtenColumns the packed chunk columns which are written.
     * @return the starts and references to write.
     */
    public static DimensionStructures assembleDimension(List<ChunkerStructureVolume> volumes, LongSet writtenColumns) {
        // Group by identifier (sorted so the output is deterministic)
        Map<String, List<ChunkerStructureVolume>> byId = new TreeMap<>();
        LongSet written = new LongOpenHashSet(writtenColumns);
        for (ChunkerStructureVolume volume : volumes) {
            byId.computeIfAbsent(volume.getStructureId(), (ignored) -> new ArrayList<>()).add(volume);
            written.add(ChunkerStructureVolume.packChunk(volume.getSourceChunk().chunkX(), volume.getSourceChunk().chunkZ()));
        }

        Long2ObjectOpenHashMap<Map<String, AssembledStart>> startsByChunk = new Long2ObjectOpenHashMap<>();
        Long2ObjectOpenHashMap<Map<String, LongSet>> referencesByChunk = new Long2ObjectOpenHashMap<>();
        for (Map.Entry<String, List<ChunkerStructureVolume>> entry : byId.entrySet()) {
            for (AssembledStart start : assemble(entry.getKey(), entry.getValue(), written)) {
                long startPacked = ChunkerStructureVolume.packChunk(start.chunkX(), start.chunkZ());
                startsByChunk.computeIfAbsent(startPacked, (ignored) -> new TreeMap<>()).put(start.id(), start);

                // Add the references from every written chunk the start covers
                int minChunkX = Math.max(start.effectiveBox().minX() >> 4, start.chunkX() - MAX_REFERENCE_DISTANCE);
                int maxChunkX = Math.min(start.effectiveBox().maxX() >> 4, start.chunkX() + MAX_REFERENCE_DISTANCE);
                int minChunkZ = Math.max(start.effectiveBox().minZ() >> 4, start.chunkZ() - MAX_REFERENCE_DISTANCE);
                int maxChunkZ = Math.min(start.effectiveBox().maxZ() >> 4, start.chunkZ() + MAX_REFERENCE_DISTANCE);
                for (int x = minChunkX; x <= maxChunkX; x++) {
                    for (int z = minChunkZ; z <= maxChunkZ; z++) {
                        long packed = ChunkerStructureVolume.packChunk(x, z);
                        if (packed != startPacked && !written.contains(packed)) continue;
                        referencesByChunk.computeIfAbsent(packed, (ignored) -> new TreeMap<>())
                                .computeIfAbsent(start.id(), (ignored) -> new LongOpenHashSet())
                                .add(startPacked);
                    }
                }
                // The start itself is always referenced (also when the box doesn't cover the chunk)
                referencesByChunk.computeIfAbsent(startPacked, (ignored) -> new TreeMap<>())
                        .computeIfAbsent(start.id(), (ignored) -> new LongOpenHashSet())
                        .add(startPacked);
            }
        }
        return new DimensionStructures(startsByChunk, referencesByChunk);
    }

    /**
     * Assemble the starts for a single structure.
     *
     * @param javaStructureId the Java identifier of the structure.
     * @param volumes         the volumes of that structure in a single dimension.
     * @param writtenColumns  the packed chunk columns which are written.
     * @return the assembled starts.
     */
    public static List<AssembledStart> assemble(String javaStructureId, List<ChunkerStructureVolume> volumes, LongSet writtenColumns) {
        // Create the units in a deterministic order
        List<ChunkerStructureVolume> sorted = new ArrayList<>(volumes);
        sorted.sort(Comparator.comparingInt((ChunkerStructureVolume volume) -> volume.getSourceChunk().chunkX())
                .thenComparingInt(volume -> volume.getSourceChunk().chunkZ()));
        List<Unit> units = new ArrayList<>(sorted.size());
        LongSet written = new LongOpenHashSet(writtenColumns);
        for (ChunkerStructureVolume volume : sorted) {
            ChunkerBoundingBox box = volume.getRoot();
            if (box == null) {
                for (ChunkerBoundingBox piece : volume.getPieces()) {
                    box = box == null ? piece : box.union(piece);
                }
            }
            if (box == null) continue; // Nothing known about it
            units.add(new Unit(volume, box));
            written.add(ChunkerStructureVolume.packChunk(volume.getSourceChunk().chunkX(), volume.getSourceChunk().chunkZ()));
        }

        // Split the units into instances
        List<List<Unit>> instances = groupInstances(units);

        // Split the instances into starts
        List<AssembledStart> output = new ArrayList<>();
        LongSet usedStartChunks = new LongOpenHashSet();
        for (List<Unit> instance : instances) {
            if (javaStructureId.equals(MONUMENT)) {
                AssembledStart start = assembleMonument(javaStructureId, instance, written, usedStartChunks);
                if (start != null) output.add(start);
                continue;
            }

            // An instance without any pieces only has empty volumes (the root is just the overall box), so it has no start
            boolean hasPieces = false;
            for (Unit unit : instance) {
                if (!unit.pieces.isEmpty()) {
                    hasPieces = true;
                    break;
                }
            }
            if (!hasPieces) continue;
            splitInstance(javaStructureId, instance, usedStartChunks, output);
        }
        return output;
    }

    /**
     * Group units into instances by joining units which touch each other.
     *
     * @param units the units to group.
     * @return the instances in a deterministic order.
     */
    private static List<List<Unit>> groupInstances(List<Unit> units) {
        int[] parent = new int[units.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }

        // Register the boxes in a grid of 16x16 cells, so only nearby boxes are compared
        Long2ObjectOpenHashMap<List<Integer>> grid = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < units.size(); i++) {
            ChunkerBoundingBox box = units.get(i).box;
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
                for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                    grid.computeIfAbsent(ChunkerStructureVolume.packChunk(x, z), (ignored) -> new ArrayList<>()).add(i);
                }
            }
        }

        for (int i = 0; i < units.size(); i++) {
            ChunkerBoundingBox box = units.get(i).box;
            for (int x = (box.minX() - 1) >> 4; x <= (box.maxX() + 1) >> 4; x++) {
                for (int z = (box.minZ() - 1) >> 4; z <= (box.maxZ() + 1) >> 4; z++) {
                    List<Integer> candidates = grid.get(ChunkerStructureVolume.packChunk(x, z));
                    if (candidates == null) continue;
                    for (int j : candidates) {
                        if (j <= i) continue;
                        if (box.intersectsOrTouches(units.get(j).box)) {
                            union(parent, i, j);
                        }
                    }
                }
            }
        }

        // Collect the instances ordered by their first unit
        Map<Integer, List<Unit>> groups = new LinkedHashMap<>();
        for (int i = 0; i < units.size(); i++) {
            groups.computeIfAbsent(find(parent, i), (ignored) -> new ArrayList<>()).add(units.get(i));
        }
        return new ArrayList<>(groups.values());
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA == rootB) return;
        // Keep the lowest index as the root so the grouping is deterministic
        if (rootA < rootB) {
            parent[rootB] = rootA;
        } else {
            parent[rootA] = rootB;
        }
    }

    /**
     * Split an instance into starts where each start only covers chunks Java will reference.
     */
    private static void splitInstance(String javaStructureId, List<Unit> instance, LongSet usedStartChunks, List<AssembledStart> output) {
        // The chunks which have not been assigned to a start yet
        Set<Long> remaining = new TreeSet<>();
        for (Unit unit : instance) {
            remaining.add(unit.packed());
        }

        while (!remaining.isEmpty()) {
            // Find the centre of the remaining chunks
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (long packed : remaining) {
                int x = ChunkerStructureVolume.unpackChunkX(packed);
                int z = ChunkerStructureVolume.unpackChunkZ(packed);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
            double centreX = (minX + maxX) / 2.0;
            double centreZ = (minZ + maxZ) / 2.0;

            // Pick the start chunk (the closest unused chunk to the centre)
            long best = 0;
            boolean found = false;
            double bestChessboard = 0, bestEuclidean = 0;
            for (long packed : remaining) {
                if (usedStartChunks.contains(packed)) continue;
                double dx = Math.abs(ChunkerStructureVolume.unpackChunkX(packed) - centreX);
                double dz = Math.abs(ChunkerStructureVolume.unpackChunkZ(packed) - centreZ);
                double chessboard = Math.max(dx, dz);
                double euclidean = dx * dx + dz * dz;
                boolean better = !found || chessboard < bestChessboard
                        || (chessboard == bestChessboard && euclidean < bestEuclidean);
                if (!better && chessboard == bestChessboard && euclidean == bestEuclidean) {
                    // Prefer the lowest X, then the lowest Z
                    int x = ChunkerStructureVolume.unpackChunkX(packed);
                    int bestX = ChunkerStructureVolume.unpackChunkX(best);
                    better = x < bestX || (x == bestX && ChunkerStructureVolume.unpackChunkZ(packed) < ChunkerStructureVolume.unpackChunkZ(best));
                }
                if (better) {
                    best = packed;
                    found = true;
                    bestChessboard = chessboard;
                    bestEuclidean = euclidean;
                }
            }
            if (!found) {
                // Every chunk is already the start of another instance of this structure, nothing can store this start
                return;
            }
            usedStartChunks.add(best);
            int startX = ChunkerStructureVolume.unpackChunkX(best);
            int startZ = ChunkerStructureVolume.unpackChunkZ(best);

            // Gather the units Java will be able to reference from the start
            List<AssembledPiece> pieces = new ArrayList<>();
            ChunkerBoundingBox effective = null;
            for (Unit unit : instance) {
                if (!remaining.contains(unit.packed())) continue;
                if (Math.max(Math.abs(unit.chunkX - startX), Math.abs(unit.chunkZ - startZ)) > MAX_REFERENCE_DISTANCE) continue;

                for (ChunkerBoundingBox piece : unit.pieces) {
                    // Pieces are always clipped to the chunk they are stored in
                    ChunkerBoundingBox clipped = piece.clipToChunk(unit.chunkX, unit.chunkZ);
                    if (clipped == null) continue;
                    pieces.add(createPiece(javaStructureId, clipped));
                    effective = effective == null ? clipped : effective.union(clipped);
                }
            }

            // Remove the chunks which are now handled
            remaining.removeIf(packed -> Math.max(
                    Math.abs(ChunkerStructureVolume.unpackChunkX(packed) - startX),
                    Math.abs(ChunkerStructureVolume.unpackChunkZ(packed) - startZ)
            ) <= MAX_REFERENCE_DISTANCE);

            if (effective == null) continue; // Nothing to store
            pieces.sort(Comparator.comparing(AssembledPiece::box, ChunkerBoundingBox.ORDER));
            if (TERRAIN_ADAPTING.contains(javaStructureId)) {
                effective = effective.inflate(TERRAIN_ADAPTATION_INFLATE);
            }
            output.add(new AssembledStart(javaStructureId, startX, startZ, pieces, effective));
        }
    }

    /**
     * Create a piece for a structure.
     * <p>
     * Java only post-processes (places) a piece inside a chunk which is newly generated and every piece here is clipped
     * to a chunk that exists in the converted world, so these placeholder pieces are never placed. The bounding boxes
     * are what the spawning and structure predicate logic of Java reads.
     */
    private static AssembledPiece createPiece(String javaStructureId, ChunkerBoundingBox box) {
        CompoundTag extra = new CompoundTag();
        extra.put("O", 0);
        extra.put("GD", 0);
        if (javaStructureId.equals("minecraft:swamp_hut")) {
            extra.put("Width", box.sizeX());
            extra.put("Height", box.sizeY());
            extra.put("Depth", box.sizeZ());
            extra.put("HPos", box.minY());
            extra.put("Witch", (byte) 1);
            extra.put("Cat", (byte) 1);
            return new AssembledPiece("minecraft:tesh", box, extra);
        }
        return new AssembledPiece("minecraft:nebs", box, extra);
    }

    /**
     * Assemble a monument, Java discards the saved pieces and regenerates the whole building from the first piece.
     */
    private static AssembledStart assembleMonument(String javaStructureId, List<Unit> instance, LongSet written, LongSet usedStartChunks) {
        // The union of everything known about the monument
        ChunkerBoundingBox union = null;
        for (Unit unit : instance) {
            union = union == null ? unit.box : union.union(unit.box);
            if (unit.root != null) union = union.union(unit.root);
            for (ChunkerBoundingBox piece : unit.pieces) {
                union = union.union(piece);
            }
        }
        if (union == null) return null;

        // If the monument was cut off by chunks that don't exist then the corner has to be derived from the other side
        int west = union.minX();
        if (union.sizeX() != MONUMENT_SIZE && (union.minX() & 15) == 0
                && !anyWritten(written, (union.minX() >> 4) - 1, (union.minX() >> 4) - 1, union.minZ() >> 4, union.maxZ() >> 4)) {
            west = union.maxX() - (MONUMENT_SIZE - 1);
        }
        int north = union.minZ();
        if (union.sizeZ() != MONUMENT_SIZE && (union.minZ() & 15) == 0
                && !anyWritten(written, union.minX() >> 4, union.maxX() >> 4, (union.minZ() >> 4) - 1, (union.minZ() >> 4) - 1)) {
            north = union.maxZ() - (MONUMENT_SIZE - 1);
        }

        // Use the centre chunk if it exists, otherwise the closest chunk of the monument
        int centreChunkX = (west + MONUMENT_SIZE / 2) >> 4;
        int centreChunkZ = (north + MONUMENT_SIZE / 2) >> 4;
        long start = ChunkerStructureVolume.packChunk(centreChunkX, centreChunkZ);
        if (!written.contains(start)) {
            long best = 0;
            boolean found = false;
            int bestChessboard = 0;
            long bestEuclidean = 0;
            for (Unit unit : instance) {
                long dx = unit.chunkX - centreChunkX;
                long dz = unit.chunkZ - centreChunkZ;
                int chessboard = (int) Math.max(Math.abs(dx), Math.abs(dz));
                long euclidean = dx * dx + dz * dz;
                if (!found || chessboard < bestChessboard || (chessboard == bestChessboard && euclidean < bestEuclidean)) {
                    best = unit.packed();
                    found = true;
                    bestChessboard = chessboard;
                    bestEuclidean = euclidean;
                }
            }
            start = best;
        }
        if (!usedStartChunks.add(start)) return null; // Already the start of a different monument

        ChunkerBoundingBox box = new ChunkerBoundingBox(west, MONUMENT_MIN_Y, north, west + MONUMENT_SIZE - 1, MONUMENT_MAX_Y, north + MONUMENT_SIZE - 1);
        CompoundTag extra = new CompoundTag();
        extra.put("O", -1);
        extra.put("GD", 0);
        return new AssembledStart(
                javaStructureId,
                ChunkerStructureVolume.unpackChunkX(start),
                ChunkerStructureVolume.unpackChunkZ(start),
                List.of(new AssembledPiece("minecraft:omb", box, extra)),
                box
        );
    }

    private static boolean anyWritten(LongSet written, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (written.contains(ChunkerStructureVolume.packChunk(x, z))) return true;
            }
        }
        return false;
    }
}

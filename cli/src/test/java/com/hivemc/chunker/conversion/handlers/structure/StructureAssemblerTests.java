package com.hivemc.chunker.conversion.handlers.structure;

import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerBoundingBox;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerStructureVolume;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StructureAssemblerTests {
    private static long pack(int x, int z) {
        return ChunkerStructureVolume.packChunk(x, z);
    }

    private static int distance(int x1, int z1, int x2, int z2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }

    /**
     * Check that every reference of every chunk is within the distance Java accepts and points at an existing start.
     */
    private static void assertReferencesValid(StructureAssembler.DimensionStructures result) {
        for (Map.Entry<Long, Map<String, LongSet>> entry : result.referencesByChunk().entrySet()) {
            int x = ChunkerStructureVolume.unpackChunkX(entry.getKey());
            int z = ChunkerStructureVolume.unpackChunkZ(entry.getKey());
            for (Map.Entry<String, LongSet> references : entry.getValue().entrySet()) {
                for (long reference : references.getValue()) {
                    int startX = ChunkerStructureVolume.unpackChunkX(reference);
                    int startZ = ChunkerStructureVolume.unpackChunkZ(reference);
                    assertTrue(distance(x, z, startX, startZ) <= 8, "reference too far from " + x + "," + z);
                    Map<String, StructureAssembler.AssembledStart> starts = result.startsByChunk().get(reference);
                    assertNotNull(starts);
                    assertTrue(starts.containsKey(references.getKey()));
                }
            }
        }
    }

    @Test
    public void testFortressFromRealChunks() throws IOException {
        List<ChunkerStructureVolume> volumes = StructureTestData.loadVolumes("nether-fortress.txt");
        assertEquals(177, volumes.size());
        LongSet written = StructureTestData.chunks(volumes);

        // The fortress covers 15x16 chunks, which Java can only reference from one start if it is in the right place
        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:fortress", volumes, written);
        assertFalse(starts.isEmpty());
        assertTrue(starts.size() <= 4, "starts: " + starts.size());
        for (StructureAssembler.AssembledStart start : starts) {
            assertEquals("minecraft:fortress", start.id());
            assertTrue(written.contains(pack(start.chunkX(), start.chunkZ())));
        }

        // One piece for each box which isn't a root, plus the roots of the chunks (50,16) and (51,15) which are a
        // separate instance without any pieces
        int expectedPieces = volumes.stream().mapToInt(volume -> volume.getPieces().size()).sum();
        assertEquals(237, expectedPieces);
        assertEquals(237 + 2, starts.stream().mapToInt(start -> start.pieces().size()).sum());

        // Every piece is inside the chunk of the volume which stored it
        Set<ChunkerBoundingBox> pieceBoxes = new HashSet<>();
        for (StructureAssembler.AssembledStart start : starts) {
            for (StructureAssembler.AssembledPiece piece : start.pieces()) {
                assertEquals("minecraft:nebs", piece.id());
                ChunkerBoundingBox box = piece.box();
                assertEquals(box.minX() >> 4, box.maxX() >> 4);
                assertEquals(box.minZ() >> 4, box.maxZ() >> 4);
                assertEquals(0, piece.extra().getInt("O", -1));
                assertEquals(0, piece.extra().getInt("GD", -1));
                pieceBoxes.add(box);
            }
        }
        for (ChunkerStructureVolume volume : volumes) {
            for (ChunkerBoundingBox box : volume.getPieces()) {
                assertTrue(pieceBoxes.contains(box));
            }
        }

        // Every unit chunk references a start within 8 chunks
        StructureAssembler.DimensionStructures result = StructureAssembler.assembleDimension(volumes, written);
        assertEquals(starts.size(), result.startsByChunk().size());
        assertReferencesValid(result);
        for (ChunkerStructureVolume volume : volumes) {
            ChunkCoordPair chunk = volume.getSourceChunk();
            LongSet references = result.referencesByChunk().get(pack(chunk.chunkX(), chunk.chunkZ())).get("minecraft:fortress");
            assertNotNull(references, chunk.toString());
            assertFalse(references.isEmpty());
        }
    }

    @Test
    public void testLongStructureIsSplitIntoStarts() {
        List<ChunkerStructureVolume> volumes = new ArrayList<>();
        for (int x = 0; x < 40; x++) {
            ChunkerBoundingBox box = new ChunkerBoundingBox(x * 16, 10, 0, x * 16 + 15, 20, 15);
            volumes.add(new ChunkerStructureVolume(Dimension.NETHER, "minecraft:fortress", new ChunkCoordPair(x, 0), box, List.of(box)));
        }
        LongSet written = StructureTestData.chunks(volumes);

        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:fortress", volumes, written);
        assertTrue(starts.size() >= 3, "starts: " + starts.size());
        Set<Long> startChunks = new HashSet<>();
        int pieces = 0;
        for (StructureAssembler.AssembledStart start : starts) {
            assertTrue(startChunks.add(pack(start.chunkX(), start.chunkZ())), "start chunks must be unique");
            for (StructureAssembler.AssembledPiece piece : start.pieces()) {
                assertTrue(distance(piece.box().minX() >> 4, piece.box().minZ() >> 4, start.chunkX(), start.chunkZ()) <= 8);
            }
            pieces += start.pieces().size();
        }
        assertEquals(40, pieces);

        // Running it twice gives the same output
        assertEquals(starts, StructureAssembler.assemble("minecraft:fortress", volumes, written));

        StructureAssembler.DimensionStructures result = StructureAssembler.assembleDimension(volumes, written);
        assertEquals(starts.size(), result.startsByChunk().size());
        assertReferencesValid(result);
        for (int x = 0; x < 40; x++) {
            LongSet references = result.referencesByChunk().get(pack(x, 0)).get("minecraft:fortress");
            assertNotNull(references);
            assertFalse(references.isEmpty());
        }
    }

    @Test
    public void testSeparateInstancesAreNotMerged() {
        List<ChunkerStructureVolume> volumes = new ArrayList<>();
        for (int x : new int[]{0, 1, 10, 11}) {
            ChunkerBoundingBox box = new ChunkerBoundingBox(x * 16, 10, 0, x * 16 + 15, 20, 15);
            volumes.add(new ChunkerStructureVolume(Dimension.OVERWORLD, "minecraft:igloo", new ChunkCoordPair(x, 0), box, List.of(box)));
        }
        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:igloo", volumes, StructureTestData.chunks(volumes));
        assertEquals(2, starts.size());
    }

    @Test
    public void testFullMonument() throws IOException {
        List<ChunkerStructureVolume> volumes = StructureTestData.loadVolumes("monument-full.txt");
        assertEquals(20, volumes.size());
        LongSet written = StructureTestData.chunks(volumes);

        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:monument", volumes, written);
        assertEquals(1, starts.size());
        assertMonument(starts.get(0), written);
    }

    @Test
    public void testTruncatedMonument() throws IOException {
        List<ChunkerStructureVolume> volumes = StructureTestData.loadVolumes("monument-truncated.txt");
        assertEquals(5, volumes.size());
        LongSet written = StructureTestData.chunks(volumes);

        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:monument", volumes, written);
        assertEquals(1, starts.size());
        assertMonument(starts.get(0), written);
    }

    private static void assertMonument(StructureAssembler.AssembledStart start, LongSet written) {
        assertEquals(1, start.pieces().size());
        StructureAssembler.AssembledPiece piece = start.pieces().get(0);
        assertEquals("minecraft:omb", piece.id());
        assertEquals(new ChunkerBoundingBox(2203, 39, -5557, 2260, 61, -5500), piece.box());
        assertEquals(piece.box(), start.effectiveBox());
        assertEquals(-1, piece.extra().getInt("O", 0));
        assertEquals(0, piece.extra().getInt("GD", -1));

        // The centre of the monument is at (2232, -5528), the start has to be a chunk which is written
        assertEquals(139, start.chunkX());
        assertEquals(-346, start.chunkZ());
        assertTrue(written.contains(pack(start.chunkX(), start.chunkZ())));
    }

    @Test
    public void testPillagerOutpostReferences() throws IOException {
        List<ChunkerStructureVolume> volumes = new ArrayList<>();
        for (StructureTestData.Sample sample : StructureTestData.load("aabb-samples.txt")) {
            for (ChunkerStructureVolume volume : StructureTestData.loadVolumes(sample)) {
                if (volume.getStructureId().equals("minecraft:pillager_outpost")) volumes.add(volume);
            }
        }
        assertEquals(9, volumes.size());

        // All the columns around the outpost are written, except for one
        LongSet written = new LongOpenHashSet();
        for (int x = 97; x <= 103; x++) {
            for (int z = -57; z <= -51; z++) {
                written.add(pack(x, z));
            }
        }
        written.remove(pack(98, -56));

        StructureAssembler.DimensionStructures result = StructureAssembler.assembleDimension(volumes, written);
        assertEquals(1, result.startsByChunk().size());
        StructureAssembler.AssembledStart start = result.startsByChunk().values().iterator().next().get("minecraft:pillager_outpost");
        assertEquals(100, start.chunkX());
        assertEquals(-54, start.chunkZ());

        // The pieces cover 1584..1629 x -879..-836, inflated by 12 for the terrain adaptation
        ChunkerBoundingBox pieces = null;
        for (StructureAssembler.AssembledPiece piece : start.pieces()) {
            pieces = pieces == null ? piece.box() : pieces.union(piece.box());
        }
        assertEquals(pieces.inflate(12), start.effectiveBox());
        assertEquals(1572, start.effectiveBox().minX());
        assertEquals(1641, start.effectiveBox().maxX());

        // The inflated box covers chunks 98..102 x -56..-52
        int references = 0;
        for (int x = 97; x <= 103; x++) {
            for (int z = -57; z <= -51; z++) {
                Map<String, LongSet> chunk = result.referencesByChunk().get(pack(x, z));
                boolean expected = x >= 98 && x <= 102 && z >= -56 && z <= -52 && !(x == 98 && z == -56);
                if (expected) {
                    assertNotNull(chunk, x + "," + z);
                    assertEquals(LongSet.of(pack(100, -54)), chunk.get("minecraft:pillager_outpost"));
                    references++;
                } else {
                    assertNull(chunk, x + "," + z);
                }
            }
        }
        assertEquals(24, references);
        assertReferencesValid(result);
    }

    @Test
    public void testSwampHutPieces() {
        ChunkerBoundingBox box = new ChunkerBoundingBox(5, 64, 6, 11, 68, 12);
        ChunkerStructureVolume volume = new ChunkerStructureVolume(Dimension.OVERWORLD, "minecraft:swamp_hut", new ChunkCoordPair(0, 0), box, List.of(box));
        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:swamp_hut", List.of(volume), StructureTestData.chunks(List.of(volume)));

        assertEquals(1, starts.size());
        StructureAssembler.AssembledPiece piece = starts.get(0).pieces().get(0);
        assertEquals("minecraft:tesh", piece.id());
        assertEquals(box, piece.box());
        assertEquals(7, piece.extra().getInt("Width", 0));
        assertEquals(5, piece.extra().getInt("Height", 0));
        assertEquals(7, piece.extra().getInt("Depth", 0));
        assertEquals(64, piece.extra().getInt("HPos", 0));
        assertEquals(1, piece.extra().getByte("Witch", (byte) 0));
        assertEquals(1, piece.extra().getByte("Cat", (byte) 0));
        assertEquals(box, starts.get(0).effectiveBox());
    }

    @Test
    public void testRootIsUsedWhenThereAreNoPieces() {
        ChunkerBoundingBox box = new ChunkerBoundingBox(0, 40, 0, 15, 80, 15);
        ChunkerStructureVolume volume = new ChunkerStructureVolume(Dimension.NETHER, "minecraft:fortress", new ChunkCoordPair(0, 0), box, List.of());
        List<StructureAssembler.AssembledStart> starts = StructureAssembler.assemble("minecraft:fortress", List.of(volume), StructureTestData.chunks(List.of(volume)));

        assertEquals(1, starts.size());
        assertEquals(1, starts.get(0).pieces().size());
        assertEquals(box, starts.get(0).pieces().get(0).box());
    }
}

package com.hivemc.chunker.conversion.encoding.bedrock.util;

import com.hivemc.chunker.conversion.handlers.structure.StructureTestData;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerBoundingBox;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerStructureVolume;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BedrockStructureParserTests {
    private static StructureTestData.Sample find(List<StructureTestData.Sample> samples, int dimension, int x, int z) {
        return samples.stream()
                .filter(sample -> sample.bedrockDimension() == dimension && sample.chunk().equals(new ChunkCoordPair(x, z)))
                .findFirst()
                .orElseThrow();
    }

    private static List<ChunkerStructureVolume> parse(StructureTestData.Sample sample, List<Exception> errors) {
        return BedrockStructureParser.parseAABBVolumes(sample.value(), sample.dimension(), sample.chunk(), errors::add);
    }

    @Test
    public void testChunkType() {
        assertEquals((byte) 119, LevelDBChunkType.AABB_VOLUMES.getId());
    }

    @Test
    public void testAllRealSamplesParse() throws IOException {
        List<Exception> errors = new ArrayList<>();
        int volumes = 0;
        for (String name : List.of("aabb-samples.txt", "nether-fortress.txt", "monument-full.txt", "monument-truncated.txt")) {
            for (StructureTestData.Sample sample : StructureTestData.load(name)) {
                List<ChunkerStructureVolume> parsed = parse(sample, errors);
                assertFalse(parsed.isEmpty(), name + " " + sample.chunk());
                for (ChunkerStructureVolume volume : parsed) {
                    assertNotNull(volume.getRoot(), name + " " + sample.chunk());
                }
                volumes += parsed.size();
            }
        }
        assertEquals(List.of(), errors);
        assertTrue(volumes > 177);
    }

    @Test
    public void testTrialChamberWithSixBoxes() throws IOException {
        List<StructureTestData.Sample> samples = StructureTestData.load("aabb-samples.txt");
        List<ChunkerStructureVolume> volumes = parse(find(samples, 0, 1, -14), new ArrayList<>());

        assertEquals(1, volumes.size());
        ChunkerStructureVolume volume = volumes.get(0);
        assertEquals("minecraft:trial_chambers", volume.getStructureId());
        assertEquals(Dimension.OVERWORLD, volume.getDimension());
        assertEquals(new ChunkCoordPair(1, -14), volume.getSourceChunk());
        assertEquals(new ChunkerBoundingBox(16, -36, -224, 31, 6, -220), volume.getRoot());
        assertEquals(5, volume.getPieces().size());
        assertTrue(volume.getPieces().contains(new ChunkerBoundingBox(17, -25, -224, 19, -24, -224)));
        assertTrue(volume.getPieces().contains(new ChunkerBoundingBox(26, -30, -220, 30, -26, -220)));
    }

    @Test
    public void testOutpostWithThreeBoxes() throws IOException {
        List<StructureTestData.Sample> samples = StructureTestData.load("aabb-samples.txt");
        ChunkerStructureVolume volume = parse(find(samples, 0, 100, -54), new ArrayList<>()).get(0);

        assertEquals("minecraft:pillager_outpost", volume.getStructureId());
        assertEquals(new ChunkerBoundingBox(1600, 63, -864, 1615, 319, -849), volume.getRoot());
        assertEquals(2, volume.getPieces().size());
        assertEquals(new ChunkerBoundingBox(1600, 72, -864, 1615, 93, -849), volume.getPieces().get(0));
    }

    @Test
    public void testMonumentWithSecondMap() throws IOException {
        List<StructureTestData.Sample> samples = StructureTestData.load("monument-full.txt");
        ChunkerStructureVolume volume = parse(find(samples, 0, 137, -348), new ArrayList<>()).get(0);

        assertEquals("minecraft:monument", volume.getStructureId());
        assertEquals(new ChunkerBoundingBox(2203, 39, -5557, 2207, 61, -5553), volume.getRoot());
        assertEquals(List.of(new ChunkerBoundingBox(2203, 39, -5557, 2207, 61, -5553)), volume.getPieces());
    }

    @Test
    public void testFortressChunks() throws IOException {
        List<StructureTestData.Sample> samples = StructureTestData.load("nether-fortress.txt");

        ChunkerStructureVolume rootOnly = parse(find(samples, 1, 44, 4), new ArrayList<>()).get(0);
        assertEquals("minecraft:fortress", rootOnly.getStructureId());
        assertEquals(Dimension.NETHER, rootOnly.getDimension());
        assertEquals(new ChunkerBoundingBox(707, 48, 64, 719, 85, 79), rootOnly.getRoot());
        assertTrue(rootOnly.getPieces().isEmpty());

        ChunkerStructureVolume sixBoxes = parse(find(samples, 1, 44, 8), new ArrayList<>()).get(0);
        assertEquals(new ChunkerBoundingBox(707, 48, 128, 719, 85, 143), sixBoxes.getRoot());
        assertEquals(5, sixBoxes.getPieces().size());
        assertTrue(sixBoxes.getPieces().contains(new ChunkerBoundingBox(707, 62, 128, 719, 75, 135)));
        assertTrue(sixBoxes.getPieces().contains(new ChunkerBoundingBox(716, 65, 143, 719, 71, 143)));
    }

    @Test
    public void testTruncatedValuesAreRejected() throws IOException {
        List<StructureTestData.Sample> samples = StructureTestData.load("aabb-samples.txt");
        byte[] value = find(samples, 0, 1, -14).value();

        for (int length : new int[]{0, 3, 8, 20, 40, value.length / 2, value.length - 1}) {
            List<Exception> errors = new ArrayList<>();
            List<ChunkerStructureVolume> parsed = BedrockStructureParser.parseAABBVolumes(
                    Arrays.copyOf(value, length), Dimension.OVERWORLD, new ChunkCoordPair(1, -14), errors::add);
            assertTrue(parsed.isEmpty(), "length " + length);
            assertEquals(1, errors.size(), "length " + length);
        }

        // Trailing garbage and impossible counts are rejected as well
        List<Exception> errors = new ArrayList<>();
        assertTrue(BedrockStructureParser.parseAABBVolumes(Arrays.copyOf(value, value.length + 3), Dimension.OVERWORLD, new ChunkCoordPair(1, -14), errors::add).isEmpty());
        byte[] impossible = value.clone();
        ByteBuffer.wrap(impossible).order(ByteOrder.LITTLE_ENDIAN).putInt(4, Integer.MAX_VALUE);
        assertTrue(BedrockStructureParser.parseAABBVolumes(impossible, Dimension.OVERWORLD, new ChunkCoordPair(1, -14), errors::add).isEmpty());
        assertEquals(2, errors.size());
    }

    @Test
    public void testHardcodedSpawnersAreClippedAndMerged() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 3 * 25).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(3);
        // Fortress boxes which overlap chunk (1, -1) and a swamp hut that is entirely inside it
        putSpawner(buffer, 10, 40, -20, 40, 60, 5, (byte) 1);
        putSpawner(buffer, 18, 50, -30, 30, 70, -2, (byte) 1);
        putSpawner(buffer, 20, 64, -10, 25, 70, -5, (byte) 2);

        List<Exception> errors = new ArrayList<>();
        List<ChunkerStructureVolume> volumes = BedrockStructureParser.parseHardcodedSpawners(
                buffer.array(), Dimension.OVERWORLD, new ChunkCoordPair(1, -1), errors::add);
        assertEquals(List.of(), errors);
        assertEquals(2, volumes.size());

        ChunkerStructureVolume fortress = volumes.get(0);
        assertEquals("minecraft:fortress", fortress.getStructureId());
        assertEquals(List.of(
                new ChunkerBoundingBox(16, 40, -16, 31, 60, -1),
                new ChunkerBoundingBox(18, 50, -16, 30, 70, -2)
        ), fortress.getPieces());
        assertEquals(new ChunkerBoundingBox(16, 40, -16, 31, 70, -1), fortress.getRoot());

        ChunkerStructureVolume hut = volumes.get(1);
        assertEquals("minecraft:swamp_hut", hut.getStructureId());
        assertEquals(new ChunkerBoundingBox(20, 64, -10, 25, 70, -5), hut.getRoot());
    }

    @Test
    public void testHardcodedSpawnerTypes() {
        assertEquals("minecraft:fortress", BedrockStructureParser.hardcodedSpawnerTypeToId(1));
        assertEquals("minecraft:swamp_hut", BedrockStructureParser.hardcodedSpawnerTypeToId(2));
        assertEquals("minecraft:monument", BedrockStructureParser.hardcodedSpawnerTypeToId(3));
        assertEquals("minecraft:pillager_outpost", BedrockStructureParser.hardcodedSpawnerTypeToId(5));
        assertNull(BedrockStructureParser.toJavaStructureId(BedrockStructureParser.hardcodedSpawnerTypeToId(4)));

        // Truncated values are rejected
        List<Exception> errors = new ArrayList<>();
        assertTrue(BedrockStructureParser.parseHardcodedSpawners(new byte[]{1, 0, 0, 0, 5}, Dimension.OVERWORLD, new ChunkCoordPair(0, 0), errors::add).isEmpty());
        assertEquals(1, errors.size());
    }

    @Test
    public void testJavaIdMapping() {
        assertEquals("minecraft:fortress", BedrockStructureParser.toJavaStructureId("minecraft:fortress"));
        assertEquals("minecraft:trial_chambers", BedrockStructureParser.toJavaStructureId("minecraft:trial_chambers"));
        assertNull(BedrockStructureParser.toJavaStructureId("minecraft:abandoned_camp_taiga"));
        assertNull(BedrockStructureParser.toJavaStructureId("fortress"));
        assertEquals(34, BedrockStructureParser.SUPPORTED_STRUCTURES.size());
    }

    private static void putSpawner(ByteBuffer buffer, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, byte type) {
        buffer.putInt(minX).putInt(minY).putInt(minZ).putInt(maxX).putInt(maxY).putInt(maxZ).put(type);
    }
}

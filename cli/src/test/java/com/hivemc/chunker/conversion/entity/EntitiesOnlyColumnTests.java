package com.hivemc.chunker.conversion.entity;

import com.google.common.io.Resources;
import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.encoding.EncodingType;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType;
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey;
import com.hivemc.chunker.conversion.encoding.java.JavaDataVersion;
import com.hivemc.chunker.conversion.integration.WorldConversionIntegrationTests;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.iq80.leveldb.table.BloomFilterPolicy;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.hivemc.chunker.conversion.entity.BedrockActors.actor;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that entities in Bedrock columns without terrain are written to Java without writing terrain for them, and that
 * block entity records in such columns don't make Chunker write terrain.
 */
public class EntitiesOnlyColumnTests {
    private static final ChunkCoordPair OVERWORLD_COLUMN = new ChunkCoordPair(1000, 1000);
    private static final ChunkCoordPair NETHER_COLUMN = new ChunkCoordPair(-1000, 1000);
    private static final ChunkCoordPair BLOCK_ENTITY_COLUMN = new ChunkCoordPair(2000, 2000);

    @Test
    public void testEntitiesWithoutTerrainAreKept() throws Exception {
        Path input = WorldConversionIntegrationTests.unzip(Resources.getResource("integration/worlds/BEDROCK_R20_80.zip"));
        Path output = WorldConversionIntegrationTests.tempFolder();
        try {
            // Add a zombie to a column with no terrain in each of two dimensions
            try (DB database = openDatabase(input)) {
                for (ChunkCoordPair column : List.of(OVERWORLD_COLUMN, NETHER_COLUMN)) {
                    Dimension dimension = column == OVERWORLD_COLUMN ? Dimension.OVERWORLD : Dimension.NETHER;
                    assertNull(database.get(LevelDBKey.key(dimension, column, LevelDBChunkType.VERSION)), "The column has no terrain");
                    long uniqueId = column == OVERWORLD_COLUMN ? 900 : 901;
                    byte[] actorKey = ByteBuffer.allocate(8).putLong(uniqueId).array();
                    CompoundTag zombie = actor("minecraft:zombie", uniqueId, column.chunkX() * 16 + 8.5f, 64f, column.chunkZ() * 16 + 8.5f);
                    database.put(LevelDBKey.key(LevelDBKey.ACTOR_PREFIX, actorKey), Tag.writeBedrockNBT(zombie));
                    database.put(LevelDBKey.key(LevelDBKey.DIGP_PREFIX, dimension, column), actorKey);
                }
            }

            WorldConverter converter = new WorldConverter(UUID.randomUUID()) {
                @Override
                public void logMissingMapping(MissingMappingType type, String identifier) {
                    // Don't log missing mappings for our tests
                }
            };
            new WorldConversionIntegrationTests().convertWorld(converter, input, output, EncodingType.JAVA, JavaDataVersion.V26_3.getVersion());
            assertFalse(converter.isExceptions());

            for (ChunkCoordPair column : List.of(OVERWORLD_COLUMN, NETHER_COLUMN)) {
                String regionName = "r." + column.regionX() + "." + column.regionZ() + ".mca";
                List<Path> entityRegions = find(output, "entities", regionName);
                assertEquals(1, entityRegions.size(), "Only one dimension has entities at " + column);
                CompoundTag entities = readChunk(entityRegions.get(0), column);
                assertNotNull(entities);
                assertArrayEquals(new int[]{column.chunkX(), column.chunkZ()}, entities.getIntArray("Position"));
                ListTag<CompoundTag, Map<String, Tag<?>>> list = entities.getList("Entities", CompoundTag.class);
                assertEquals(1, list.size());
                assertEquals("minecraft:zombie", list.get(0).getString("id"));

                // No terrain is written, so Java generates it
                assertTrue(find(output, "region", regionName).isEmpty(), "No terrain is written for " + column);
            }
        } finally {
            WorldConversionIntegrationTests.remove(input);
            WorldConversionIntegrationTests.remove(output);
        }
    }

    @Test
    public void testBlockEntitiesWithoutTerrainAreNotWrittenAsTerrain() throws Exception {
        Path input = WorldConversionIntegrationTests.unzip(Resources.getResource("integration/worlds/BEDROCK_R20_80.zip"));
        Path output = WorldConversionIntegrationTests.tempFolder();
        try {
            // A flower pot record in a column with no terrain, as Bedrock keeps around trial chambers
            try (DB database = openDatabase(input)) {
                assertNull(database.get(LevelDBKey.key(Dimension.OVERWORLD, BLOCK_ENTITY_COLUMN, LevelDBChunkType.VERSION)), "The column has no terrain");
                CompoundTag plant = new CompoundTag();
                plant.put("name", "minecraft:deadbush");
                plant.put("states", new CompoundTag());
                CompoundTag flowerPot = new CompoundTag();
                flowerPot.put("id", "FlowerPot");
                flowerPot.put("x", BLOCK_ENTITY_COLUMN.chunkX() * 16 + 5);
                flowerPot.put("y", -27);
                flowerPot.put("z", BLOCK_ENTITY_COLUMN.chunkZ() * 16 + 4);
                flowerPot.put("PlantBlock", plant);
                database.put(LevelDBKey.key(Dimension.OVERWORLD, BLOCK_ENTITY_COLUMN, LevelDBChunkType.BLOCK_ENTITY), Tag.writeBedrockNBT(flowerPot));
            }

            WorldConverter converter = new WorldConverter(UUID.randomUUID()) {
                @Override
                public void logMissingMapping(MissingMappingType type, String identifier) {
                    // Don't log missing mappings for our tests
                }
            };
            new WorldConversionIntegrationTests().convertWorld(converter, input, output, EncodingType.JAVA, JavaDataVersion.V26_3.getVersion());
            assertFalse(converter.isExceptions());

            // An empty column would be a hole in Java, so no terrain is written and Java generates it
            String regionName = "r." + BLOCK_ENTITY_COLUMN.regionX() + "." + BLOCK_ENTITY_COLUMN.regionZ() + ".mca";
            assertTrue(find(output, "region", regionName).isEmpty(), "No terrain is written for " + BLOCK_ENTITY_COLUMN);
        } finally {
            WorldConversionIntegrationTests.remove(input);
            WorldConversionIntegrationTests.remove(output);
        }
    }

    private static DB openDatabase(Path world) throws IOException {
        Files.deleteIfExists(world.resolve("db").resolve("LOCK"));
        Options options = new Options();
        options.compressionType(CompressionType.ZLIB_RAW);
        options.blockSize(160 * 1024);
        options.filterPolicy(new BloomFilterPolicy(10));
        options.createIfMissing(false);
        return new Iq80DBFactory().open(world.resolve("db").toFile(), options);
    }

    private static List<Path> find(Path world, String directory, String fileName) throws IOException {
        try (Stream<Path> files = Files.walk(world)) {
            return files.filter(path -> path.getFileName().toString().equals(fileName)
                    && path.getParent().getFileName().toString().equals(directory)).toList();
        }
    }

    @Nullable
    private static CompoundTag readChunk(Path regionFile, ChunkCoordPair column) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(regionFile.toFile(), "r")) {
            file.seek(((column.chunkX() & 31) + ((column.chunkZ() & 31) << 5)) << 2);
            int location = file.readInt();
            if (location == 0) return null;
            file.seek((long) (location >>> 8) << 12);
            int length = file.readInt();
            assertEquals(2, file.readByte(), "ZLib compression");
            byte[] data = new byte[length - 1];
            file.readFully(data);
            return Tag.readZLibJavaNBT(data);
        }
    }
}

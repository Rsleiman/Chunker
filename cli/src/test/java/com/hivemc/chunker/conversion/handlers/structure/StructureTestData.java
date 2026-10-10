package com.hivemc.chunker.conversion.handlers.structure;

import com.hivemc.chunker.conversion.encoding.bedrock.util.BedrockStructureParser;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerStructureVolume;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Helper for loading the real AABBVolumes values dumped from Bedrock worlds.
 * Each line is "dimension chunkX chunkZ valueLength hex".
 */
public class StructureTestData {
    /**
     * A single chunk value from a dump.
     *
     * @param bedrockDimension the Bedrock dimension ID.
     * @param chunk            the chunk which stored the value.
     * @param value            the raw value.
     */
    public record Sample(int bedrockDimension, ChunkCoordPair chunk, byte[] value) {
        /**
         * Get the dimension of the sample.
         *
         * @return the dimension.
         */
        public Dimension dimension() {
            return switch (bedrockDimension) {
                case 0 -> Dimension.OVERWORLD;
                case 1 -> Dimension.NETHER;
                default -> Dimension.THE_END;
            };
        }
    }

    /**
     * Load the samples from a test resource.
     *
     * @param name the name of the resource inside the structures folder.
     * @return the samples.
     * @throws IOException if the resource couldn't be read.
     */
    public static List<Sample> load(String name) throws IOException {
        List<Sample> output = new ArrayList<>();
        try (InputStream stream = StructureTestData.class.getResourceAsStream("/structures/" + name)) {
            if (stream == null) throw new IOException("Missing resource " + name);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.trim().split("\\s+");
                byte[] value = HexFormat.of().parseHex(parts[4]);
                if (value.length != Integer.parseInt(parts[3])) throw new IOException("Bad length in " + name);
                output.add(new Sample(Integer.parseInt(parts[0]), new ChunkCoordPair(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), value));
            }
        }
        return output;
    }

    /**
     * Load the samples and parse them into volumes (failing on any parse error).
     *
     * @param name the name of the resource inside the structures folder.
     * @return the volumes of all the samples.
     * @throws IOException if the resource couldn't be read.
     */
    public static List<ChunkerStructureVolume> loadVolumes(String name) throws IOException {
        List<ChunkerStructureVolume> output = new ArrayList<>();
        for (Sample sample : load(name)) {
            output.addAll(BedrockStructureParser.parseAABBVolumes(sample.value(), sample.dimension(), sample.chunk(), (e) -> {
                throw new AssertionError(e);
            }));
        }
        return output;
    }

    /**
     * Parse a single sample into volumes (failing on any parse error).
     *
     * @param sample the sample to parse.
     * @return the volumes of the sample.
     */
    public static List<ChunkerStructureVolume> loadVolumes(Sample sample) {
        return BedrockStructureParser.parseAABBVolumes(sample.value(), sample.dimension(), sample.chunk(), (e) -> {
            throw new AssertionError(e);
        });
    }

    /**
     * Get the packed chunks of the volumes.
     *
     * @param volumes the volumes.
     * @return a set of the source chunks.
     */
    public static LongSet chunks(List<ChunkerStructureVolume> volumes) {
        LongSet output = new LongOpenHashSet();
        for (ChunkerStructureVolume volume : volumes) {
            output.add(ChunkerStructureVolume.packChunk(volume.getSourceChunk().chunkX(), volume.getSourceChunk().chunkZ()));
        }
        return output;
    }
}

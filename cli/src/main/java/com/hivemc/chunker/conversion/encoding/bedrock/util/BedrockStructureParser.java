package com.hivemc.chunker.conversion.encoding.bedrock.util;

import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerBoundingBox;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerStructureVolume;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Parser for the per-chunk structure data that Bedrock stores (AABBVolumes and HardcodedSpawners).
 */
public class BedrockStructureParser {
    /**
     * The identifier used for the root box of a structure inside AABBVolumes.
     */
    public static final int ROOT_KEY = 1;

    /**
     * The Bedrock structure identifiers which have an equivalent Java structure with the same identifier.
     */
    public static final Set<String> SUPPORTED_STRUCTURES = Set.of(
            "minecraft:ancient_city", "minecraft:bastion_remnant", "minecraft:buried_treasure",
            "minecraft:desert_pyramid", "minecraft:end_city", "minecraft:fortress", "minecraft:igloo",
            "minecraft:jungle_pyramid", "minecraft:mansion", "minecraft:mineshaft", "minecraft:mineshaft_mesa",
            "minecraft:monument", "minecraft:nether_fossil", "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm",
            "minecraft:pillager_outpost", "minecraft:ruined_portal", "minecraft:ruined_portal_desert",
            "minecraft:ruined_portal_jungle", "minecraft:ruined_portal_mountain", "minecraft:ruined_portal_nether",
            "minecraft:ruined_portal_ocean", "minecraft:ruined_portal_swamp", "minecraft:shipwreck",
            "minecraft:shipwreck_beached", "minecraft:stronghold", "minecraft:swamp_hut", "minecraft:trail_ruins",
            "minecraft:trial_chambers", "minecraft:village_desert", "minecraft:village_plains",
            "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"
    );

    /**
     * Get the Java structure identifier for a structure read from Bedrock.
     *
     * @param bedrockId the identifier read from Bedrock.
     * @return the Java identifier or null if the structure is not supported.
     */
    @Nullable
    public static String toJavaStructureId(String bedrockId) {
        return SUPPORTED_STRUCTURES.contains(bedrockId) ? bedrockId : null;
    }

    /**
     * Parse the AABBVolumes value (chunk key tag 119) of a chunk. Every box is already clipped to the chunk.
     *
     * @param value        the value from the database.
     * @param dimension    the dimension the chunk is inside.
     * @param chunk        the chunk which stores the value.
     * @param errorHandler the handler called when the value is not consistent (nothing is returned in that case).
     * @return the volumes inside the value, empty if the value could not be parsed.
     */
    public static List<ChunkerStructureVolume> parseAABBVolumes(byte[] value, Dimension dimension, ChunkCoordPair chunk, Consumer<Exception> errorHandler) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
            long version = buffer.getInt() & 0xFFFFFFFFL;
            if (version == 0 || version > 1) {
                throw new IllegalArgumentException("Unsupported AABBVolumes version " + version);
            }

            int volumeCount = readCount(buffer, 4 + 2 + 4 + 4 + 4);
            List<ChunkerStructureVolume> output = new ArrayList<>(volumeCount);
            for (int i = 0; i < volumeCount; i++) {
                buffer.getInt(); // Unknown (always 0)
                int idLength = buffer.getShort() & 0xFFFF;
                if (idLength > buffer.remaining()) throw new IllegalArgumentException("Structure id exceeds the value");
                byte[] idBytes = new byte[idLength];
                buffer.get(idBytes);
                String id = new String(idBytes, StandardCharsets.UTF_8);

                // Read the boxes
                int boxCount = readCount(buffer, 28);
                ChunkerBoundingBox root = null;
                List<ChunkerBoundingBox> pieces = new ArrayList<>(boxCount);
                for (int j = 0; j < boxCount; j++) {
                    int key = buffer.getInt();
                    ChunkerBoundingBox box = new ChunkerBoundingBox(
                            buffer.getInt(), buffer.getInt(), buffer.getInt(),
                            buffer.getInt(), buffer.getInt(), buffer.getInt()
                    );
                    if (box.minX() > box.maxX() || box.minY() > box.maxY() || box.minZ() > box.maxZ()) {
                        throw new IllegalArgumentException("Inverted bounding box " + box);
                    }
                    if (key == ROOT_KEY) {
                        root = box;
                    } else {
                        pieces.add(box);
                    }
                }

                // Skip the remaining maps (key, a, isRoot) and (key, a, b, isRoot)
                skip(buffer, readCount(buffer, 12), 12);
                skip(buffer, readCount(buffer, 16), 16);

                if (root != null || !pieces.isEmpty()) {
                    output.add(new ChunkerStructureVolume(dimension, id, chunk, root, pieces));
                }
            }

            if (buffer.hasRemaining()) {
                throw new IllegalArgumentException(buffer.remaining() + " unexpected trailing bytes");
            }
            return output;
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            errorHandler.accept(new Exception("Failed to parse AABBVolumes for chunk " + chunk + " in " + dimension.getIdentifier(), e));
            return Collections.emptyList();
        }
    }

    /**
     * Parse the HardcodedSpawners value (chunk key tag 57) of a chunk. The boxes are not clipped in the value, so they
     * are clipped to the chunk and each type is returned as a single volume with its root being the union of the boxes.
     *
     * @param value        the value from the database.
     * @param dimension    the dimension the chunk is inside.
     * @param chunk        the chunk which stores the value.
     * @param errorHandler the handler called when the value is not consistent (nothing is returned in that case).
     * @return the volumes inside the value, empty if the value could not be parsed. Unknown types have an id that is
     * not supported.
     */
    public static List<ChunkerStructureVolume> parseHardcodedSpawners(byte[] value, Dimension dimension, ChunkCoordPair chunk, Consumer<Exception> errorHandler) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
            int count = readCount(buffer, 25);

            // Group by type in a deterministic order
            Map<Integer, List<ChunkerBoundingBox>> byType = new TreeMap<>();
            for (int i = 0; i < count; i++) {
                ChunkerBoundingBox box = new ChunkerBoundingBox(
                        buffer.getInt(), buffer.getInt(), buffer.getInt(),
                        buffer.getInt(), buffer.getInt(), buffer.getInt()
                );
                int type = buffer.get() & 0xFF;
                if (box.minX() > box.maxX() || box.minY() > box.maxY() || box.minZ() > box.maxZ()) {
                    throw new IllegalArgumentException("Inverted bounding box " + box);
                }

                ChunkerBoundingBox clipped = box.clipToChunk(chunk.chunkX(), chunk.chunkZ());
                if (clipped == null) continue; // Outside of the chunk, nothing to store
                byType.computeIfAbsent(type, (ignored) -> new ArrayList<>()).add(clipped);
            }

            List<ChunkerStructureVolume> output = new ArrayList<>(byType.size());
            for (Map.Entry<Integer, List<ChunkerBoundingBox>> entry : byType.entrySet()) {
                List<ChunkerBoundingBox> boxes = entry.getValue();
                ChunkerBoundingBox root = boxes.get(0);
                for (ChunkerBoundingBox box : boxes) {
                    root = root.union(box);
                }
                output.add(new ChunkerStructureVolume(dimension, hardcodedSpawnerTypeToId(entry.getKey()), chunk, root, boxes));
            }
            return output;
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            errorHandler.accept(new Exception("Failed to parse HardcodedSpawners for chunk " + chunk + " in " + dimension.getIdentifier(), e));
            return Collections.emptyList();
        }
    }

    /**
     * Convert a hardcoded spawner type to the identifier of the structure.
     *
     * @param type the type from the value.
     * @return the identifier of the structure, for unknown types an identifier which is not supported.
     */
    public static String hardcodedSpawnerTypeToId(int type) {
        return switch (type) {
            case 1 -> "minecraft:fortress";
            case 2 -> "minecraft:swamp_hut";
            case 3 -> "minecraft:monument";
            case 5 -> "minecraft:pillager_outpost";
            default -> "minecraft:hardcoded_spawner_type_" + type;
        };
    }

    /**
     * Read an unsigned count and check that the entries following it can fit in the rest of the buffer.
     *
     * @param buffer    the buffer to read from.
     * @param entrySize the minimum number of bytes each entry needs.
     * @return the count.
     */
    private static int readCount(ByteBuffer buffer, int entrySize) {
        long count = buffer.getInt() & 0xFFFFFFFFL;
        if (count * entrySize > buffer.remaining()) {
            throw new IllegalArgumentException("Count " + count + " exceeds the value");
        }
        return (int) count;
    }

    /**
     * Skip over a number of fixed size entries.
     *
     * @param buffer    the buffer to skip in.
     * @param count     the number of entries.
     * @param entrySize the size of each entry.
     */
    private static void skip(ByteBuffer buffer, int count, int entrySize) {
        buffer.position(buffer.position() + count * entrySize);
    }
}

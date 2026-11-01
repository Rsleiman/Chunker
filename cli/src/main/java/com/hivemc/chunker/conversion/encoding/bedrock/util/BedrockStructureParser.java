package com.hivemc.chunker.conversion.encoding.bedrock.util;

import com.hivemc.chunker.conversion.handlers.structure.JavaStructurePlacement;
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
     * The prefix of the abandoned camp identifiers, which end with the name of the biome on both editions.
     */
    private static final String ABANDONED_CAMP_PREFIX = "minecraft:abandoned_camp_";

    /**
     * The camp biomes which Bedrock names with the biome's old name, mapped to the Java name (as in ChunkerBiome).
     */
    private static final Map<String, String> ABANDONED_CAMP_BIOMES = Map.of(
            "birch_forest_mutated", "old_growth_birch_forest",
            "cold_taiga", "snowy_taiga",
            "extreme_hills_plus_trees", "windswept_forest",
            "jungle_edge", "sparse_jungle",
            "mega_taiga", "old_growth_pine_taiga",
            "mesa_plateau_stone", "wooded_badlands",
            "redwood_taiga_mutated", "old_growth_spruce_taiga",
            "swampland", "swamp"
    );

    /**
     * Get the Java structure identifier for a structure read from Bedrock.
     *
     * @param bedrockId the identifier read from Bedrock.
     * @return the Java identifier or null if the structure is not supported.
     */
    @Nullable
    public static String toJavaStructureId(String bedrockId) {
        if (SUPPORTED_STRUCTURES.contains(bedrockId)) return bedrockId;
        if (bedrockId.startsWith(ABANDONED_CAMP_PREFIX)) {
            String biome = bedrockId.substring(ABANDONED_CAMP_PREFIX.length());
            String javaId = ABANDONED_CAMP_PREFIX + ABANDONED_CAMP_BIOMES.getOrDefault(biome, biome);
            return JavaStructurePlacement.ABANDONED_CAMPS.contains(javaId) ? javaId : null;
        }
        return null;
    }

    /**
     * Parse the AABBVolumes value (chunk key tag 119) of a chunk. Every box is already clipped to the chunk.
     * <p>
     * The value holds a table of the structure identifiers in the chunk, then every box with its key, then two lists
     * which link each box key to a structure in the table and say whether it is that structure's whole box (the
     * second list, used by structures with mob spawns such as fortresses, has one more value).
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

            // Read the structure identifiers, keyed by their index
            int idCount = readCount(buffer, 4 + 2);
            Map<Integer, String> ids = new LinkedHashMap<>(idCount);
            for (int i = 0; i < idCount; i++) {
                int index = buffer.getInt();
                int idLength = buffer.getShort() & 0xFFFF;
                if (idLength > buffer.remaining()) throw new IllegalArgumentException("Structure id exceeds the value");
                byte[] idBytes = new byte[idLength];
                buffer.get(idBytes);
                if (ids.put(index, new String(idBytes, StandardCharsets.UTF_8)) != null) {
                    throw new IllegalArgumentException("Duplicate structure index " + index);
                }
            }

            // Read the boxes, keyed by their key
            int boxCount = readCount(buffer, 28);
            Map<Integer, ChunkerBoundingBox> boxes = new LinkedHashMap<>(boxCount);
            for (int i = 0; i < boxCount; i++) {
                int key = buffer.getInt();
                ChunkerBoundingBox box = new ChunkerBoundingBox(
                        buffer.getInt(), buffer.getInt(), buffer.getInt(),
                        buffer.getInt(), buffer.getInt(), buffer.getInt()
                );
                if (box.minX() > box.maxX() || box.minY() > box.maxY() || box.minZ() > box.maxZ()) {
                    throw new IllegalArgumentException("Inverted bounding box " + box);
                }
                if (boxes.put(key, box) != null) throw new IllegalArgumentException("Duplicate box key " + key);
            }

            // Read the links (key, structure index, isRoot) and (key, structure index, unknown, isRoot)
            Map<Integer, int[]> links = new HashMap<>();
            int linkCount = readCount(buffer, 12);
            for (int i = 0; i < linkCount; i++) {
                links.put(buffer.getInt(), new int[]{buffer.getInt(), buffer.getInt()});
            }
            linkCount = readCount(buffer, 16);
            for (int i = 0; i < linkCount; i++) {
                int key = buffer.getInt();
                int index = buffer.getInt();
                buffer.getInt(); // Unknown (0, or -3 for outposts and swamp huts)
                links.put(key, new int[]{index, buffer.getInt()});
            }

            if (buffer.hasRemaining()) {
                throw new IllegalArgumentException(buffer.remaining() + " unexpected trailing bytes");
            }

            // Group the boxes by structure, in the order of the table
            Map<Integer, ChunkerBoundingBox> roots = new HashMap<>();
            Map<Integer, List<ChunkerBoundingBox>> pieces = new HashMap<>();
            for (Map.Entry<Integer, ChunkerBoundingBox> entry : boxes.entrySet()) {
                int[] link = links.get(entry.getKey());
                int index;
                boolean isRoot;
                if (link != null) {
                    index = link[0];
                    isRoot = link[1] != 0;
                } else if (ids.size() == 1) {
                    // An unlinked box in a chunk with a single structure belongs to it
                    index = ids.keySet().iterator().next();
                    isRoot = entry.getKey() == ROOT_KEY;
                } else {
                    throw new IllegalArgumentException("Box " + entry.getKey() + " is not linked to a structure");
                }
                if (!ids.containsKey(index)) throw new IllegalArgumentException("Unknown structure index " + index);
                if (isRoot) {
                    if (roots.put(index, entry.getValue()) != null) {
                        throw new IllegalArgumentException("Two root boxes for structure index " + index);
                    }
                } else {
                    pieces.computeIfAbsent(index, (ignored) -> new ArrayList<>()).add(entry.getValue());
                }
            }

            List<ChunkerStructureVolume> output = new ArrayList<>(ids.size());
            for (Map.Entry<Integer, String> entry : ids.entrySet()) {
                ChunkerBoundingBox root = roots.get(entry.getKey());
                List<ChunkerBoundingBox> structurePieces = pieces.getOrDefault(entry.getKey(), new ArrayList<>());
                if (root != null || !structurePieces.isEmpty()) {
                    output.add(new ChunkerStructureVolume(dimension, entry.getValue(), chunk, root, structurePieces));
                }
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
}

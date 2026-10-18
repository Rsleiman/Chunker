package com.hivemc.chunker.conversion.encoding.bedrock.util;

import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parses the points of interest Bedrock villages record for their villagers.
 * <p>
 * Each village has a {@code VILLAGE_<Dimension>_<uuid>_POI} record listing, per villager ({@code VillagerID} is the
 * villager's UniqueID), up to three instances: {@code Type} 0 is the bed, 1 the meeting point (bell) and 2 the
 * workstation, each with {@code X}, {@code Y} and {@code Z}. {@code Skip: 1b} marks an empty slot.
 */
public final class BedrockVillageParser {
    /**
     * The prefix of the village record keys.
     */
    public static final String KEY_PREFIX = "VILLAGE_";
    /**
     * The suffix of the village points of interest record keys.
     */
    public static final String POI_SUFFIX = "_POI";

    private BedrockVillageParser() {
    }

    /**
     * Get the dimension from a village record key.
     *
     * @param key the key, e.g. VILLAGE_Overworld_093a6b34-a4e9-469e-9401-e7aba25e23f7_POI.
     * @return the dimension or null if the key isn't a village points of interest record.
     */
    @Nullable
    public static Dimension getDimension(String key) {
        if (!key.startsWith(KEY_PREFIX) || !key.endsWith(POI_SUFFIX)) return null;
        String rest = key.substring(KEY_PREFIX.length());
        int separator = rest.indexOf('_');
        if (separator < 0) return null;
        return switch (rest.substring(0, separator)) {
            case "Overworld" -> Dimension.OVERWORLD;
            case "Nether" -> Dimension.NETHER;
            case "TheEnd" -> Dimension.THE_END;
            default -> null;
        };
    }

    /**
     * Parse a village points of interest record.
     *
     * @param value     the record value (little-endian NBT).
     * @param dimension the dimension of the village.
     * @param output    the memories per villager UniqueID to add to.
     * @throws Exception if the record could not be decoded.
     */
    public static void parsePOI(byte[] value, Dimension dimension, Map<Long, Map<ChunkerVillagerMemory, ChunkerGlobalPosition>> output) throws Exception {
        CompoundTag record = Objects.requireNonNull(Tag.readBedrockNBT(value));
        ListTag<CompoundTag, Map<String, Tag<?>>> villagers = record.getList("POI", CompoundTag.class, null);
        if (villagers == null) return;

        for (CompoundTag villager : villagers) {
            if (!villager.contains("VillagerID")) continue;
            long villagerId = villager.getLong("VillagerID");
            ListTag<CompoundTag, Map<String, Tag<?>>> instances = villager.getList("instances", CompoundTag.class, null);
            if (instances == null) continue;

            Map<ChunkerVillagerMemory, ChunkerGlobalPosition> memories = new EnumMap<>(ChunkerVillagerMemory.class);
            for (CompoundTag instance : instances) {
                if (instance.getByte("Skip", (byte) 0) == 1) continue;
                ChunkerVillagerMemory memory = switch (instance.getInt("Type", -1)) {
                    case 0 -> ChunkerVillagerMemory.HOME;
                    case 1 -> ChunkerVillagerMemory.MEETING_POINT;
                    case 2 -> ChunkerVillagerMemory.JOB_SITE;
                    default -> null;
                };
                if (memory == null || !instance.contains("X")) continue;
                memories.put(memory, new ChunkerGlobalPosition(
                        dimension,
                        instance.getInt("X", 0),
                        instance.getInt("Y", 0),
                        instance.getInt("Z", 0)
                ));
            }

            if (!memories.isEmpty()) {
                output.put(villagerId, memories);
            }
        }
    }
}

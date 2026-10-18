package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.FloatTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;

import java.util.Map;

/**
 * Builders for Bedrock actor NBT, shaped like the records BDS 1.26.52 saves.
 */
public final class BedrockActors {
    private BedrockActors() {
    }

    /**
     * Create an actor with the keys every Bedrock actor has.
     *
     * @param identifier the identifier (e.g. minecraft:wolf).
     * @param uniqueId   the UniqueID.
     * @param x          the X position.
     * @param y          the Y position.
     * @param z          the Z position.
     * @return the actor NBT.
     */
    public static CompoundTag actor(String identifier, long uniqueId, float x, float y, float z) {
        CompoundTag tag = new CompoundTag();
        tag.put("identifier", identifier);
        tag.put("UniqueID", uniqueId);
        tag.put("Pos", floats(x, y, z));
        tag.put("Motion", floats(0, 0, 0));
        tag.put("Rotation", floats(90, 0));
        tag.put("FallDistance", 0f);
        tag.put("OnGround", (byte) 1);
        tag.put("Invulnerable", (byte) 0);
        tag.put("PortalCooldown", 0);
        tag.put("Air", (short) 300);
        tag.put("Tags", new ListTag<>(TagType.STRING));
        tag.put("definitions", new ListTag<>(TagType.STRING));
        return tag;
    }

    /**
     * Create a list of floats.
     *
     * @param values the values.
     * @return the list tag.
     */
    public static ListTag<FloatTag, Float> floats(float... values) {
        ListTag<FloatTag, Float> list = new ListTag<>(TagType.FLOAT, values.length);
        for (float value : values) {
            list.add(new FloatTag(value));
        }
        return list;
    }

    /**
     * Create a list of strings.
     *
     * @param values the values.
     * @return the list tag.
     */
    public static ListTag<StringTag, String> strings(String... values) {
        ListTag<StringTag, String> list = new ListTag<>(TagType.STRING, values.length);
        for (String value : values) {
            list.add(new StringTag(value));
        }
        return list;
    }

    /**
     * Create a list of compounds.
     *
     * @param values the values.
     * @return the list tag.
     */
    public static ListTag<CompoundTag, Map<String, Tag<?>>> compounds(CompoundTag... values) {
        ListTag<CompoundTag, Map<String, Tag<?>>> list = new ListTag<>(TagType.COMPOUND, values.length);
        for (CompoundTag value : values) {
            list.add(value);
        }
        return list;
    }
}

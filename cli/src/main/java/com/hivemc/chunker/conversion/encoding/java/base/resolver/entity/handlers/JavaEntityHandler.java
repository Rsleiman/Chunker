package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.google.gson.JsonElement;
import com.hivemc.chunker.conversion.encoding.base.resolver.entity.EntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.DoubleTag;
import com.hivemc.chunker.nbt.tags.primitive.FloatTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import com.hivemc.chunker.util.JsonTextUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base entity handler which converts position, motion and rotation tags, and the data every entity has.
 */
public class JavaEntityHandler extends EntityHandler<JavaResolvers, CompoundTag, Entity> {
    public JavaEntityHandler() {
        super(null, Entity.class, () -> {
            throw new IllegalArgumentException("Unable to construct Entity, invalid type!");
        });
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull Entity entity) {
        // Save original NBT
        if (resolvers.converter().shouldAllowNBTCopying()) {
            entity.setOriginalNBT(input);
        }

        // Position
        List<Double> position = input.getListValues("Pos", DoubleTag.class, null);
        if (position != null && position.size() >= 3) {
            entity.setPositionX(position.get(0));
            entity.setPositionY(position.get(1));
            entity.setPositionZ(position.get(2));
        }

        // Motion
        List<Double> motion = input.getListValues("Motion", DoubleTag.class, null);
        if (motion != null && motion.size() >= 3) {
            entity.setMotionX(motion.get(0));
            entity.setMotionY(motion.get(1));
            entity.setMotionZ(motion.get(2));
        }

        // Rotation
        List<Float> rotation = input.getListValues("Rotation", FloatTag.class, null);
        if (rotation != null && rotation.size() >= 2) {
            entity.setYaw(rotation.get(0));
            entity.setPitch(rotation.get(1));
        }

        // Shared data
        UUID uuid;
        if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 16, 0)) {
            uuid = JavaEntityNBT.fromIntArray(input.getIntArray("UUID", null));
        } else {
            uuid = input.contains("UUIDMost") ? new UUID(input.getLong("UUIDMost"), input.getLong("UUIDLeast", 0)) : null;
        }
        if (uuid != null) {
            entity.put(ChunkerEntityProperty.UUID, uuid);
        }
        input.getOptional("CustomName", Tag.class)
                .map(JsonTextUtil::fromNBT)
                .ifPresent(name -> entity.put(ChunkerEntityProperty.CUSTOM_NAME, name));
        input.getOptionalValue("CustomNameVisible", Byte.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.CUSTOM_NAME_VISIBLE, value == 1));
        input.getOptionalValue("Air", Short.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.AIR, (int) value));
        input.getOptionalValue("OnGround", Byte.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.ON_GROUND, value == 1));
        input.getOptionalValue("Invulnerable", Byte.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.INVULNERABLE, value == 1));
        input.getOptionalValue("PortalCooldown", Integer.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.PORTAL_COOLDOWN, value));
        if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 21, 5)) {
            input.getOptionalValue("fall_distance", Double.class)
                    .ifPresent(value -> entity.put(ChunkerEntityProperty.FALL_DISTANCE, value));
        } else {
            input.getOptionalValue("FallDistance", Float.class)
                    .ifPresent(value -> entity.put(ChunkerEntityProperty.FALL_DISTANCE, (double) value));
        }
        List<String> tags = input.getListValues("Tags", StringTag.class, null);
        if (tags != null && !tags.isEmpty()) {
            entity.put(ChunkerEntityProperty.TAGS, new ArrayList<>(tags));
        }
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull Entity entity) {
        // Restore original NBT
        if (resolvers.converter().shouldAllowNBTCopying() && entity.getOriginalNBT() != null) {
            // Copy all the original tags
            for (Map.Entry<String, Tag<?>> namedPair : entity.getOriginalNBT()) {
                output.put(namedPair.getKey(), namedPair.getValue().clone());
            }
        }

        // Position
        ListTag<DoubleTag, Double> pos = new ListTag<>(TagType.DOUBLE, 3);
        pos.add(new DoubleTag(entity.getPositionX()));
        pos.add(new DoubleTag(entity.getPositionY()));
        pos.add(new DoubleTag(entity.getPositionZ()));
        output.put("Pos", pos);

        // Motion
        ListTag<DoubleTag, Double> motion = new ListTag<>(TagType.DOUBLE, 3);
        motion.add(new DoubleTag(entity.getMotionX()));
        motion.add(new DoubleTag(entity.getMotionY()));
        motion.add(new DoubleTag(entity.getMotionZ()));
        output.put("Motion", motion);

        // Motion
        ListTag<FloatTag, Float> rotation = new ListTag<>(TagType.FLOAT, 2);
        rotation.add(new FloatTag(entity.getYaw()));
        rotation.add(new FloatTag(entity.getPitch()));
        output.put("Rotation", rotation);

        // Shared data
        UUID uuid = entity.get(ChunkerEntityProperty.UUID);
        if (uuid != null) {
            if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 16, 0)) {
                output.put("UUID", JavaEntityNBT.toIntArray(uuid));
            } else {
                output.put("UUIDMost", uuid.getMostSignificantBits());
                output.put("UUIDLeast", uuid.getLeastSignificantBits());
            }
        }
        JsonElement customName = entity.get(ChunkerEntityProperty.CUSTOM_NAME);
        if (customName != null) {
            output.put("CustomName", JsonTextUtil.toNBT(customName, resolvers.dataVersion()));
        }
        Boolean customNameVisible = entity.get(ChunkerEntityProperty.CUSTOM_NAME_VISIBLE);
        if (customNameVisible != null) {
            output.put("CustomNameVisible", customNameVisible ? (byte) 1 : (byte) 0);
        }
        Integer air = entity.get(ChunkerEntityProperty.AIR);
        if (air != null) {
            output.put("Air", air.shortValue());
        }
        Boolean onGround = entity.get(ChunkerEntityProperty.ON_GROUND);
        if (onGround != null) {
            output.put("OnGround", onGround ? (byte) 1 : (byte) 0);
        }
        Boolean invulnerable = entity.get(ChunkerEntityProperty.INVULNERABLE);
        if (invulnerable != null) {
            output.put("Invulnerable", invulnerable ? (byte) 1 : (byte) 0);
        }
        Integer portalCooldown = entity.get(ChunkerEntityProperty.PORTAL_COOLDOWN);
        if (portalCooldown != null) {
            output.put("PortalCooldown", portalCooldown.intValue());
        }
        Double fallDistance = entity.get(ChunkerEntityProperty.FALL_DISTANCE);
        if (fallDistance != null) {
            if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 21, 5)) {
                output.put("fall_distance", fallDistance.doubleValue());
            } else {
                output.put("FallDistance", fallDistance.floatValue());
            }
        }
        List<String> tags = entity.get(ChunkerEntityProperty.TAGS);
        if (tags != null && !tags.isEmpty()) {
            ListTag<StringTag, String> tagList = new ListTag<>(TagType.STRING, tags.size());
            for (String tag : tags) {
                tagList.add(new StringTag(tag));
            }
            output.put("Tags", tagList);
        }
    }
}

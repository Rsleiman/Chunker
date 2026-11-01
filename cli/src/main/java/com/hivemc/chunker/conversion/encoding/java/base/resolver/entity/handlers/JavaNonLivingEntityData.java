package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.handlers.JavaRandomizableContainerBlockEntityHandler;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Writes and reads the data of Java entities which aren't living (dropped items, experience orbs, arrows, falling blocks, minecarts
 * and boats), as stored from 1.21.5 onwards.
 */
public final class JavaNonLivingEntityData {
    private JavaNonLivingEntityData() {
    }

    /**
     * Write the data of a non-living entity.
     *
     * @param resolvers the resolvers.
     * @param output    the entity NBT.
     * @param entity    the entity.
     */
    public static void write(JavaResolvers resolvers, CompoundTag output, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();
        if (type == ChunkerVanillaEntityType.ITEM) {
            JavaLivingEntityData.writeItem(resolvers, entity.get(ChunkerEntityProperty.ITEM)).ifPresent(item -> output.put("Item", item));
            writeAgeAndHealth(output, entity);
            UUID thrower = entity.get(ChunkerEntityProperty.OWNER);
            if (thrower != null) {
                output.put("Thrower", JavaEntityNBT.toIntArray(thrower));
            }
        } else if (type == ChunkerVanillaEntityType.EXPERIENCE_ORB) {
            Integer value = entity.get(ChunkerEntityProperty.EXPERIENCE_VALUE);
            output.put("Value", (short) (value == null ? 1 : value));
            output.put("Count", 1);
            writeAgeAndHealth(output, entity);
        } else if (type == ChunkerVanillaEntityType.ARROW) {
            Boolean inGround = entity.get(ChunkerEntityProperty.IN_GROUND);
            if (inGround != null) {
                output.put("inGround", inGround ? (byte) 1 : (byte) 0);
            }
            Integer pickup = entity.get(ChunkerEntityProperty.PICKUP);
            if (pickup != null) {
                output.put("pickup", pickup.byteValue());
            }
            UUID owner = entity.get(ChunkerEntityProperty.OWNER);
            if (owner != null) {
                output.put("Owner", JavaEntityNBT.toIntArray(owner));
            }
        } else if (type == ChunkerVanillaEntityType.FALLING_BLOCK) {
            ChunkerBlockIdentifier blockState = entity.get(ChunkerEntityProperty.BLOCK_STATE);
            if (blockState != null) {
                output.put("BlockState", resolvers.writeBlock(blockState));
            }
            Integer time = entity.get(ChunkerEntityProperty.AGE);
            output.put("Time", time == null ? 0 : time);
            output.put("DropItem", (byte) 1);
        }

        if (GenericEntityTypes.CONTAINERS.contains(type)) {
            String lootTable = entity.get(ChunkerEntityProperty.LOOT_TABLE);
            if (lootTable != null && !JavaRandomizableContainerBlockEntityHandler.UNSUPPORTED_LOOT_TABLES.contains(lootTable)) {
                output.put("LootTable", lootTable);
                Long seed = entity.get(ChunkerEntityProperty.LOOT_TABLE_SEED);
                if (seed != null) {
                    output.put("LootTableSeed", seed.longValue());
                }
            }

            Map<Integer, ChunkerItemStack> inventory = entity.get(ChunkerEntityProperty.INVENTORY);
            if (inventory != null) {
                ListTag<CompoundTag, Map<String, Tag<?>>> items = new ListTag<>(TagType.COMPOUND, inventory.size());
                for (Map.Entry<Integer, ChunkerItemStack> entry : inventory.entrySet()) {
                    JavaLivingEntityData.writeItem(resolvers, entry.getValue()).ifPresent(item -> {
                        item.put("Slot", entry.getKey().byteValue());
                        items.add(item);
                    });
                }
                output.put("Items", items);
            }
        }
    }

    /**
     * Read the data of a non-living entity.
     *
     * @param resolvers the resolvers.
     * @param input     the entity NBT.
     * @param entity    the entity to fill, its type is already set.
     */
    public static void read(JavaResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();
        if (type == ChunkerVanillaEntityType.ITEM) {
            CompoundTag item = input.getCompound("Item");
            if (item != null) {
                entity.put(ChunkerEntityProperty.ITEM, resolvers.readItem(item));
            }
            readAgeAndHealth(input, entity);
            UUID thrower = JavaEntityNBT.fromIntArray(input.getIntArray("Thrower", null));
            if (thrower != null) {
                entity.put(ChunkerEntityProperty.OWNER, thrower);
            }
        } else if (type == ChunkerVanillaEntityType.EXPERIENCE_ORB) {
            input.getOptionalValue("Value", Number.class).ifPresent(value -> entity.put(ChunkerEntityProperty.EXPERIENCE_VALUE, value.intValue()));
            readAgeAndHealth(input, entity);
        } else if (type == ChunkerVanillaEntityType.ARROW) {
            input.getOptionalValue("inGround", Byte.class).ifPresent(value -> entity.put(ChunkerEntityProperty.IN_GROUND, value == 1));
            input.getOptionalValue("pickup", Byte.class).ifPresent(value -> entity.put(ChunkerEntityProperty.PICKUP, value.intValue()));
            UUID owner = JavaEntityNBT.fromIntArray(input.getIntArray("Owner", null));
            if (owner != null) {
                entity.put(ChunkerEntityProperty.OWNER, owner);
            }
        } else if (type == ChunkerVanillaEntityType.FALLING_BLOCK) {
            CompoundTag blockState = input.getCompound("BlockState");
            if (blockState != null) {
                entity.put(ChunkerEntityProperty.BLOCK_STATE, resolvers.readBlock(blockState));
            }
            input.getOptionalValue("Time", Integer.class).ifPresent(time -> entity.put(ChunkerEntityProperty.AGE, time));
        }

        if (GenericEntityTypes.CONTAINERS.contains(type)) {
            String lootTable = input.getString("LootTable", null);
            if (lootTable != null && !lootTable.isEmpty()) {
                entity.put(ChunkerEntityProperty.LOOT_TABLE, lootTable);
                input.getOptionalValue("LootTableSeed", Long.class).ifPresent(seed -> entity.put(ChunkerEntityProperty.LOOT_TABLE_SEED, seed));
            }

            ListTag<CompoundTag, Map<String, Tag<?>>> items = input.getList("Items", CompoundTag.class, null);
            if (items != null) {
                Map<Integer, ChunkerItemStack> inventory = new TreeMap<>();
                for (CompoundTag item : items) {
                    inventory.put((int) item.getByte("Slot", (byte) inventory.size()), resolvers.readItem(item));
                }
                entity.put(ChunkerEntityProperty.INVENTORY, inventory);
            }
        }
    }

    private static void writeAgeAndHealth(CompoundTag output, GenericEntity entity) {
        Integer age = entity.get(ChunkerEntityProperty.AGE);
        if (age != null) {
            output.put("Age", age.shortValue());
        }
        Float health = entity.get(ChunkerEntityProperty.HEALTH);
        if (health != null) {
            output.put("Health", (short) Math.round(health));
        }
    }

    private static void readAgeAndHealth(CompoundTag input, GenericEntity entity) {
        input.getOptionalValue("Age", Number.class).ifPresent(age -> entity.put(ChunkerEntityProperty.AGE, age.intValue()));
        input.getOptionalValue("Health", Number.class).ifPresent(health -> entity.put(ChunkerEntityProperty.HEALTH, health.floatValue()));
    }
}

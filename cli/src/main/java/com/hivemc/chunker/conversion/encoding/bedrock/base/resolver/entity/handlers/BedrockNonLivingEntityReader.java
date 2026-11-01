package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.blockentity.handlers.BedrockRandomizableContainerBlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.identifier.BedrockBlockCompoundTag;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.IntTag;

import java.util.List;

/**
 * Reads the data of Bedrock actors which aren't living (dropped items, experience orbs, arrows, falling blocks, minecarts and boats)
 * into a GenericEntity.
 */
public final class BedrockNonLivingEntityReader {
    private BedrockNonLivingEntityReader() {
    }

    /**
     * Read the data of a non-living actor.
     *
     * @param resolvers the resolvers.
     * @param input     the actor NBT.
     * @param entity    the entity to fill, its type is already set.
     */
    public static void read(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();

        // Bedrock has one boat type, the wood is the Variant
        if (type == ChunkerVanillaEntityType.OAK_BOAT) {
            type = getBoatType(GenericEntityTypes.BOATS, input.getInt("Variant", 0));
            entity.setEntityType(type);
        } else if (type == ChunkerVanillaEntityType.OAK_CHEST_BOAT) {
            type = getBoatType(GenericEntityTypes.CHEST_BOATS, input.getInt("Variant", 0));
            entity.setEntityType(type);
        }

        if (type == ChunkerVanillaEntityType.ITEM) {
            ChunkerItemStack item = BedrockLivingEntityReader.readItem(resolvers, input.getCompound("Item"));
            if (item != null) {
                entity.put(ChunkerEntityProperty.ITEM, item);
            }
            readAgeAndHealth(input, entity);

            // The player who dropped the item
            BedrockEntityUUIDs.referenceUUID(resolvers.converter(), input.getLong("OwnerID", -1)).ifPresent(owner -> entity.put(ChunkerEntityProperty.OWNER, owner));
        } else if (type == ChunkerVanillaEntityType.EXPERIENCE_ORB) {
            entity.put(ChunkerEntityProperty.EXPERIENCE_VALUE, input.getOptionalValue("experience value", Number.class).map(Number::intValue).orElse(1));
            readAgeAndHealth(input, entity);
        } else if (type == ChunkerVanillaEntityType.ARROW) {
            readArrow(resolvers, input, entity);
        } else if (type == ChunkerVanillaEntityType.FALLING_BLOCK) {
            CompoundTag block = input.getCompound("FallingBlock");
            if (block != null) {
                entity.put(ChunkerEntityProperty.BLOCK_STATE, resolvers.readBlock(new BedrockBlockCompoundTag(block, false)));
            }
            input.getOptionalValue("Time", Number.class).ifPresent(time -> entity.put(ChunkerEntityProperty.AGE, time.intValue()));
        }

        if (GenericEntityTypes.CONTAINERS.contains(type)) {
            String lootTable = input.getString("LootTable", null);
            if (lootTable != null && !lootTable.isEmpty() && resolvers.converter().shouldProcessLootTables()) {
                entity.put(ChunkerEntityProperty.LOOT_TABLE, BedrockRandomizableContainerBlockEntityHandler.bedrockLootTableToChunker(lootTable));
                input.getOptionalValue("LootTableSeed", Number.class).ifPresent(seed -> entity.put(ChunkerEntityProperty.LOOT_TABLE_SEED, seed.longValue()));
            } else {
                BedrockLivingEntityReader.readInventory(resolvers, input, entity);
            }
        }
    }

    private static ChunkerEntityType getBoatType(List<ChunkerVanillaEntityType> types, int variant) {
        // Unknown woods become oak, so the boat is kept
        return variant >= 0 && variant < types.size() ? types.get(variant) : types.get(0);
    }

    private static void readAgeAndHealth(CompoundTag input, GenericEntity entity) {
        input.getOptionalValue("Age", Number.class).ifPresent(age -> entity.put(ChunkerEntityProperty.AGE, age.intValue()));
        input.getOptionalValue("Health", Number.class).ifPresent(health -> entity.put(ChunkerEntityProperty.HEALTH, health.floatValue()));
    }

    private static void readArrow(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity) {
        // A stuck arrow records the block it's in, a flying one records 0, 0, 0
        ListTag<IntTag, Integer> stuckToBlock = input.getList("StuckToBlockPos", IntTag.class, null);
        boolean inGround = stuckToBlock != null && stuckToBlock.size() == 3
                && (stuckToBlock.get(0).getValue() != 0 || stuckToBlock.get(1).getValue() != 0 || stuckToBlock.get(2).getValue() != 0);
        entity.put(ChunkerEntityProperty.IN_GROUND, inGround);

        // Arrows shot by players can be picked up, in creative mode only by creative players
        int pickup = input.getByte("isCreative", (byte) 0) == 1 ? 2 : input.getByte("player", (byte) 0) == 1 ? 1 : 0;
        entity.put(ChunkerEntityProperty.PICKUP, pickup);

        BedrockEntityUUIDs.referenceUUID(resolvers.converter(), input.getLong("OwnerID", -1)).ifPresent(owner -> entity.put(ChunkerEntityProperty.OWNER, owner));
    }
}

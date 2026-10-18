package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.IntTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for converting dropped items, experience orbs, minecarts, boats and arrows.
 */
public class BedrockNonLivingEntityTests {
    private static final long PLAYER_ID = -4294967295L;

    private static CompoundTag item(String name, int count) {
        CompoundTag item = new CompoundTag();
        item.put("Name", name);
        item.put("Count", (byte) count);
        item.put("Damage", (short) 0);
        item.put("WasPickedUp", (byte) 0);
        return item;
    }

    private static ListTag<IntTag, Integer> ints(int... values) {
        ListTag<IntTag, Integer> list = new ListTag<>(TagType.INT, values.length);
        for (int value : values) {
            list.add(new IntTag(value));
        }
        return list;
    }

    @Test
    public void testDroppedItem() {
        CompoundTag bedrock = actor("minecraft:item", 60, 37.6f, 16.6f, 76.9f);
        bedrock.put("Item", item("minecraft:name_tag", 3));
        bedrock.put("Age", (short) 120);
        bedrock.put("Health", (short) 5);
        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:item", java.getString("id"));
        assertEquals("minecraft:name_tag", java.getCompound("Item").getString("id"));
        assertEquals(3, java.getCompound("Item").getInt("count"));
        assertEquals((short) 120, java.getShort("Age"));
        assertEquals((short) 5, java.getShort("Health"));
    }

    @Test
    public void testDroppedItemWithoutItemIsRemoved() {
        CompoundTag bedrock = actor("minecraft:item", 61, 0, 0, 0);
        bedrock.put("Item", item("minecraft:air", 0));
        Entity entity = readBedrock(bedrock);
        assertTrue(BEDROCK.entityResolver().shouldRemoveBeforeProcess(new ChunkerColumn(new ChunkCoordPair(0, 0)), entity));
    }

    @Test
    public void testExperienceOrb() {
        CompoundTag bedrock = actor("minecraft:xp_orb", 62, 41.9f, 70f, -52.9f);
        bedrock.put("experience value", 7);
        bedrock.put("Age", (short) 30);
        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:experience_orb", java.getString("id"));
        assertEquals((short) 7, java.getShort("Value"));
        assertEquals(1, java.getInt("Count"));
        assertEquals((short) 30, java.getShort("Age"));
    }

    @Test
    public void testChestMinecartWithLootTable() {
        CompoundTag bedrock = actor("minecraft:chest_minecart", 63, 455.5f, 28.4f, -163.4f);
        bedrock.put("LootTable", "loot_tables/chests/abandoned_mineshaft.json");
        bedrock.put("LootTableSeed", -1234567);
        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:chest_minecart", java.getString("id"));
        assertEquals("minecraft:chests/abandoned_mineshaft", java.getString("LootTable"));
        assertEquals(-1234567L, java.getLong("LootTableSeed"));
        assertFalse(java.contains("Items"));
    }

    @Test
    public void testChestMinecartWithItems() {
        CompoundTag bedrock = actor("minecraft:chest_minecart", 64, 0, 0, 0);
        CompoundTag nameTag = item("minecraft:name_tag", 2);
        nameTag.put("Slot", (byte) 5);
        bedrock.put("ChestItems", compounds(nameTag));
        CompoundTag java = convert(bedrock);
        ListTag<CompoundTag, Map<String, Tag<?>>> items = java.getList("Items", CompoundTag.class);
        assertEquals(1, items.size());
        assertEquals((byte) 5, items.get(0).getByte("Slot"));
        assertEquals("minecraft:name_tag", items.get(0).getString("id"));
    }

    @Test
    public void testBoatWoodFromVariant() {
        assertEquals("minecraft:oak_boat", convert(boat("minecraft:boat", 0)).getString("id"));
        assertEquals("minecraft:bamboo_raft", convert(boat("minecraft:boat", 7)).getString("id"));
        assertEquals("minecraft:pale_oak_boat", convert(boat("minecraft:boat", 9)).getString("id"));
        assertEquals("minecraft:poplar_boat", convert(boat("minecraft:boat", 10)).getString("id"));
        assertEquals("minecraft:oak_boat", convert(boat("minecraft:boat", 42)).getString("id"), "Unknown woods are kept as oak");

        CompoundTag chestBoat = boat("minecraft:chest_boat", 2);
        CompoundTag nameTag = item("minecraft:name_tag", 1);
        nameTag.put("Slot", (byte) 0);
        chestBoat.put("ChestItems", compounds(nameTag));
        CompoundTag java = convert(chestBoat);
        assertEquals("minecraft:birch_chest_boat", java.getString("id"));
        assertEquals(1, java.getList("Items", CompoundTag.class).size());
    }

    private static CompoundTag boat(String identifier, int variant) {
        CompoundTag bedrock = actor(identifier, 65 + variant, 1360.9f, 63f, 5.4f);
        bedrock.put("Variant", variant);
        return bedrock;
    }

    @Test
    public void testArrowStuckInGround() {
        CompoundTag bedrock = actor("minecraft:arrow", 70, 1578.2f, 69f, 152.9f);
        bedrock.put("StuckToBlockPos", ints(1578, 68, 152));
        bedrock.put("player", (byte) 1);
        bedrock.put("isCreative", (byte) 0);
        bedrock.put("OwnerID", PLAYER_ID);
        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:arrow", java.getString("id"));
        assertEquals((byte) 1, java.getByte("inGround"));
        assertEquals((byte) 1, java.getByte("pickup"), "Player arrows can be picked up");
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(PLAYER_ID)), java.getIntArray("Owner"));
    }

    @Test
    public void testFlyingSkeletonArrow() {
        CompoundTag bedrock = actor("minecraft:arrow", 71, 0, 0, 0);
        bedrock.put("Motion", floats(-0.8f, -1.5f, 0.1f));
        bedrock.put("StuckToBlockPos", ints(0, 0, 0));
        bedrock.put("player", (byte) 0);
        bedrock.put("OwnerID", 72L);
        CompoundTag java = convert(bedrock);
        assertEquals((byte) 0, java.getByte("inGround"));
        assertEquals((byte) 0, java.getByte("pickup"), "Skeleton arrows can't be picked up");
    }
}

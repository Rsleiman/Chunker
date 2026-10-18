package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.primitive.DoubleTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import com.hivemc.chunker.util.JsonTextUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.hivemc.chunker.conversion.entity.BedrockActors.actor;
import static com.hivemc.chunker.conversion.entity.BedrockActors.strings;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the data every converted entity has.
 */
public class BedrockEntityBaseTests {
    @Test
    public void testUUIDMatchesPrototype() {
        // Values from tools/entity-probe/translate.py: uuid.uuid5(uuid.NAMESPACE_OID, f"bedrock-actor:{uid}")
        assertEquals(UUID.fromString("31a1aa7b-f86d-5ff1-a1db-6dd9b3f44428"), BedrockEntityUUIDs.toUUID(-17179862338L));
        assertEquals(UUID.fromString("5f8f5a16-4c37-5f2a-b3ea-c88c9c585b9a"), BedrockEntityUUIDs.toUUID(-4294967295L));
        assertTrue(BedrockEntityUUIDs.referenceUUID(CONVERTER, -1).isEmpty());
        assertEquals(BedrockEntityUUIDs.toUUID(-4294967295L), BedrockEntityUUIDs.referenceUUID(CONVERTER, -4294967295L).orElseThrow());
    }

    @Test
    public void testUUIDIntArray() {
        UUID uuid = UUID.fromString("31a1aa7b-f86d-5ff1-a1db-6dd9b3f44428");
        assertEquals(uuid, JavaEntityNBT.fromIntArray(JavaEntityNBT.toIntArray(uuid)));
        assertNull(JavaEntityNBT.fromIntArray(new int[3]));
    }

    @Test
    public void testMobReadAsGenericEntity() {
        Entity entity = readBedrock(actor("minecraft:zombie", 42, 1.5f, 64f, -2.5f));
        GenericEntity generic = assertInstanceOf(GenericEntity.class, entity);
        assertEquals(ChunkerVanillaEntityType.ZOMBIE, generic.getEntityType());
        assertEquals(1.5, generic.getPositionX());
        assertEquals(BedrockEntityUUIDs.toUUID(42), generic.get(ChunkerEntityProperty.UUID));
    }

    @Test
    public void testRenamedIdentifiers() {
        assertEquals("minecraft:villager", convert(actor("minecraft:villager_v2", 1, 0, 0, 0)).getString("id"));
        assertEquals("minecraft:zombie_villager", convert(actor("minecraft:zombie_villager_v2", 2, 0, 0, 0)).getString("id"));
        assertEquals("minecraft:zombified_piglin", convert(actor("minecraft:zombie_pigman", 3, 0, 0, 0)).getString("id"));
        assertEquals("minecraft:tropical_fish", convert(actor("minecraft:tropicalfish", 4, 0, 0, 0)).getString("id"));
    }

    @Test
    public void testSharedData() {
        CompoundTag bedrock = actor("minecraft:cat", -17179862338L, 10.25f, 70f, -3.75f);
        bedrock.put("CustomName", "Tom");
        bedrock.put("CustomNameVisible", (byte) 1);
        bedrock.put("Air", (short) 120);
        bedrock.put("FallDistance", 1.5f);
        bedrock.put("PortalCooldown", 40);
        bedrock.put("Invulnerable", (byte) 1);
        bedrock.put("Tags", strings("pet", "home"));

        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:cat", java.getString("id"));
        assertArrayEquals(JavaEntityNBT.toIntArray(UUID.fromString("31a1aa7b-f86d-5ff1-a1db-6dd9b3f44428")), java.getIntArray("UUID"));
        assertEquals("Tom", JsonTextUtil.toStripped(JsonTextUtil.fromNBT(java.get("CustomName"))));
        assertEquals((byte) 1, java.getByte("CustomNameVisible"));
        assertEquals((short) 120, java.getShort("Air"));
        assertEquals(1.5, java.getDouble("fall_distance"));
        assertEquals(40, java.getInt("PortalCooldown"));
        assertEquals((byte) 1, java.getByte("Invulnerable"));
        assertEquals((byte) 1, java.getByte("OnGround"));
        assertEquals(List.of("pet", "home"), java.getListValues("Tags", StringTag.class));
        assertEquals(List.of(10.25, 70.0, -3.75), java.getListValues("Pos", DoubleTag.class));
    }

    @Test
    public void testEmptyCustomNameIgnored() {
        CompoundTag bedrock = actor("minecraft:cow", 5, 0, 0, 0);
        bedrock.put("CustomName", "");
        CompoundTag java = convert(bedrock);
        assertFalse(java.contains("CustomName"));
        assertFalse(java.contains("CustomNameVisible"));
    }

    @Test
    public void testExcludedTypesAreNotRead() {
        // The ender dragon belongs to the dragon fight state
        assertTrue(BEDROCK.entityResolver().to(actor("minecraft:ender_dragon", 6, 0, 0, 0)).isEmpty());
    }

    @Test
    public void testBedrockWriterDropsGenericEntitiesWithoutOriginalNBT() {
        GenericEntity entity = new GenericEntity(ChunkerVanillaEntityType.COW);
        ChunkerColumn column = new ChunkerColumn(new ChunkCoordPair(0, 0));
        assertTrue(BEDROCK.entityResolver().shouldRemoveBeforeWrite(column, entity));
        entity.setOriginalNBT(actor("minecraft:cow", 8, 0, 0, 0));
        assertFalse(BEDROCK.entityResolver().shouldRemoveBeforeWrite(column, entity));
    }

    @Test
    public void testCushionIsAttachedToItsBlock() {
        CompoundTag bedrock = actor("minecraft:cushion", -17179862400L, 1190.5f, 66.5625f, -185.5f);
        bedrock.put("Variant", 1);
        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:cushion", java.getString("id"));
        assertArrayEquals(new int[]{1190, 66, -186}, java.getIntArray("block_pos"), "Java needs the block the cushion is in");
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(-17179862400L)), java.getIntArray("UUID"));
    }
}

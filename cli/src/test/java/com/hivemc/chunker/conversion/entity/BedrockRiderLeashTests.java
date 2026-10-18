package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for converting riders (Bedrock LinksTag to Java Passengers) and leashes.
 */
public class BedrockRiderLeashTests {
    private static final long HORSE_ID = -47244622124L;
    private static final long ZOMBIE_ID = -47244622123L;

    private static CompoundTag zombieHorse() {
        CompoundTag horse = actor("minecraft:zombie_horse", HORSE_ID, 351.9f, 64f, -190f);
        CompoundTag link = new CompoundTag();
        link.put("entityID", ZOMBIE_ID);
        link.put("linkID", 0);
        horse.put("LinksTag", compounds(link));
        return horse;
    }

    /**
     * Run the entity steps of the Bedrock column post-process (BedrockColumnReader.postProcess).
     */
    private static void postProcess(ChunkerColumn column) {
        List<Entity> entities = column.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            entities.set(i, BEDROCK.entityResolver().updateBeforeProcess(column, entities.get(i)));
        }
        entities.removeIf(entity -> BEDROCK.entityResolver().shouldRemoveBeforeProcess(column, entity));
    }

    @Test
    public void testRiderInTheSameColumnIsNested() {
        ChunkerColumn column = new ChunkerColumn(new ChunkCoordPair(21, -12));
        column.getEntities().add(readBedrock(zombieHorse()));
        column.getEntities().add(readBedrock(actor("minecraft:zombie", ZOMBIE_ID, 351.9f, 64.9f, -190f)));
        postProcess(column);

        assertEquals(1, column.getEntities().size(), "The rider is only stored inside its vehicle");
        Entity horse = column.getEntities().get(0);
        assertNull(horse.get(ChunkerEntityProperty.PASSENGER_IDS));

        CompoundTag java = writeJava(horse);
        assertEquals("minecraft:zombie_horse", java.getString("id"));
        ListTag<CompoundTag, Map<String, Tag<?>>> passengers = java.getList("Passengers", CompoundTag.class);
        assertEquals(1, passengers.size());
        assertEquals("minecraft:zombie", passengers.get(0).getString("id"));
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(ZOMBIE_ID)), passengers.get(0).getIntArray("UUID"));

        // Reading the Java vehicle back gives the same entity with its rider
        assertEquals(horse, JAVA.entityResolver().to(java).orElseThrow());
    }

    @Test
    public void testRiderInAnotherColumnStaysSeparate() {
        ChunkerColumn column = new ChunkerColumn(new ChunkCoordPair(21, -12));
        column.getEntities().add(readBedrock(zombieHorse()));
        postProcess(column);

        Entity horse = column.getEntities().get(0);
        assertNull(horse.get(ChunkerEntityProperty.PASSENGER_IDS));
        assertNull(horse.get(ChunkerEntityProperty.PASSENGERS));
        assertFalse(writeJava(horse).contains("Passengers"));
    }

    @Test
    public void testLeashHeldByAnEntity() {
        long traderId = -17179868888L;
        CompoundTag llama = actor("minecraft:trader_llama", -17179868887L, 0, 0, 0);
        llama.put("LeasherID", traderId);
        CompoundTag java = convert(llama);
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(traderId)), java.getCompound("leash").getIntArray("UUID"));

        CompoundTag unleashed = actor("minecraft:trader_llama", -17179868886L, 0, 0, 0);
        unleashed.put("LeasherID", -1L);
        assertFalse(convert(unleashed).contains("leash"));
    }
}

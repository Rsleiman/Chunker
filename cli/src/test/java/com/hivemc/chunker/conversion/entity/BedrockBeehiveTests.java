package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BeehiveBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for converting the bees inside beehives and bee nests.
 */
public class BedrockBeehiveTests {
    private static final long BEE_ID = -51539598715L;

    private static CompoundTag occupant(CompoundTag saveData, int ticksLeft) {
        CompoundTag occupant = new CompoundTag();
        occupant.put("ActorIdentifier", "minecraft:bee<>");
        occupant.put("SaveData", saveData);
        occupant.put("TicksLeftToStay", ticksLeft);
        return occupant;
    }

    @Test
    public void testBeesInsideAHive() {
        CompoundTag bee = actor("minecraft:bee", BEE_ID, 888.5f, 88f, -642.5f);
        bee.put("HomePos", floats(888, 88, -643));
        CompoundTag hive = new CompoundTag();
        hive.put("id", "Beehive");
        hive.put("x", 888);
        hive.put("y", 88);
        hive.put("z", -643);
        hive.put("isMovable", (byte) 1);
        hive.put("ShouldSpawnBees", (byte) 0);
        hive.put("Occupants", compounds(occupant(bee, 573), occupant(new CompoundTag(), 0)));

        BlockEntity blockEntity = BEDROCK.blockEntityResolver().to(hive).orElseThrow();
        assertEquals(2, ((BeehiveBlockEntity) blockEntity).getOccupants().size());

        CompoundTag java = JAVA.blockEntityResolver().from(blockEntity).orElseThrow();
        assertEquals("minecraft:beehive", java.getString("id"));
        ListTag<CompoundTag, Map<String, Tag<?>>> bees = java.getList("bees", CompoundTag.class);
        assertEquals(2, bees.size());

        CompoundTag first = bees.get(0);
        assertEquals("minecraft:bee", first.getCompound("entity_data").getString("id"));
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(BEE_ID)), first.getCompound("entity_data").getIntArray("UUID"));
        assertArrayEquals(new int[]{888, 88, -643}, first.getCompound("entity_data").getIntArray("hive_pos"));
        assertEquals(0, first.getInt("ticks_in_hive"));
        assertEquals(573, first.getInt("min_ticks_in_hive"), "The bee leaves after the same number of ticks");

        // A bee of a newly generated nest has no saved actor
        assertEquals("minecraft:bee", bees.get(1).getCompound("entity_data").getString("id"));

        // Reading the Java hive back gives the same bees
        BeehiveBlockEntity readBack = (BeehiveBlockEntity) JAVA.blockEntityResolver().to(java).orElseThrow();
        assertEquals(((BeehiveBlockEntity) blockEntity).getOccupants(), readBack.getOccupants());
    }
}

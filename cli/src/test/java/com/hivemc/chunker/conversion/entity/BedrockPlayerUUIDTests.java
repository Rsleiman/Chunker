package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.bedrock.resolver.MockConverter;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockDataVersion;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockEncoders;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.hivemc.chunker.conversion.entity.BedrockActors.actor;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.JAVA;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for giving Bedrock players the Java UUIDs of their accounts.
 */
public class BedrockPlayerUUIDTests {
    private static final long MAPPED_PLAYER = -4294967295L;
    private static final long UNMAPPED_PLAYER = -17179869183L;
    private static final UUID JAVA_ACCOUNT = UUID.fromString("907d4c76-76a0-4d98-80ab-92b41cb7c4a2");

    private static BedrockResolvers resolvers(Map<Long, UUID> playerUUIDs) {
        WorldConverter converter = new MockConverter(new ChunkerLevel(null, null, Collections.emptyList(), null, Collections.emptyList()));
        converter.setBedrockPlayerUUIDs(playerUUIDs);
        return BedrockEncoders.getNearestEncoder(BedrockDataVersion.V1_26_50)
                .readerConstructor()
                .construct(null, BedrockDataVersion.V1_26_50.getVersion(), converter)
                .buildResolvers(converter)
                .build();
    }

    private static CompoundTag tamedWolf(long owner, long leasher) {
        CompoundTag wolf = actor("minecraft:wolf", 10, 0.5f, 64f, 0.5f);
        wolf.put("IsTamed", (byte) 1);
        wolf.put("OwnerNew", owner);
        wolf.put("LeasherID", leasher);
        return wolf;
    }

    @Test
    public void testMappedPlayerOwnsTheirPets() {
        BedrockResolvers bedrock = resolvers(Map.of(MAPPED_PLAYER, JAVA_ACCOUNT));
        Entity wolf = bedrock.entityResolver().to(tamedWolf(MAPPED_PLAYER, UNMAPPED_PLAYER)).orElseThrow();
        CompoundTag java = JAVA.entityResolver().from(wolf).orElseThrow();

        assertArrayEquals(JavaEntityNBT.toIntArray(JAVA_ACCOUNT), java.getIntArray("Owner"));

        // Players without an entry keep the derived UUID
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(UNMAPPED_PLAYER)), java.getCompound("leash").getIntArray("UUID"));
    }

    @Test
    public void testNoMappingKeepsDerivedUUIDs() {
        BedrockResolvers bedrock = resolvers(Map.of());
        Entity wolf = bedrock.entityResolver().to(tamedWolf(MAPPED_PLAYER, -1)).orElseThrow();
        CompoundTag java = JAVA.entityResolver().from(wolf).orElseThrow();

        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(MAPPED_PLAYER)), java.getIntArray("Owner"));
        assertFalse(java.contains("leash"));
    }
}

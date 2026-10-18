package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.bedrock.resolver.MockConverter;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockDataVersion;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockEncoders;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.java.JavaDataVersion;
import com.hivemc.chunker.conversion.encoding.java.JavaEncoders;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolvers for converting entities from Bedrock 1.26.50 to Java 26.3 in tests.
 */
public final class EntityTestResolvers {
    public static final WorldConverter CONVERTER = new MockConverter(new ChunkerLevel(null, null, Collections.emptyList(), null, Collections.emptyList()));
    public static final BedrockResolvers BEDROCK = BedrockEncoders.getNearestEncoder(BedrockDataVersion.V1_26_50)
            .readerConstructor()
            .construct(null, BedrockDataVersion.V1_26_50.getVersion(), CONVERTER)
            .buildResolvers(CONVERTER)
            .build();
    public static final JavaResolvers JAVA = JavaEncoders.getNearestEncoder(JavaDataVersion.V26_3)
            .writerConstructor()
            .construct(null, JavaDataVersion.V26_3.getVersion(), CONVERTER)
            .buildResolvers(CONVERTER)
            .build();

    private EntityTestResolvers() {
    }

    /**
     * Read a Bedrock actor.
     *
     * @param bedrock the actor NBT.
     * @return the entity.
     */
    public static Entity readBedrock(CompoundTag bedrock) {
        return BEDROCK.entityResolver().to(bedrock).orElseThrow(() -> new AssertionError("Bedrock entity was not read: " + bedrock));
    }

    /**
     * Write an entity as Java NBT.
     *
     * @param entity the entity.
     * @return the Java NBT.
     */
    public static CompoundTag writeJava(Entity entity) {
        return JAVA.entityResolver().from(entity).orElseThrow(() -> new AssertionError("Java entity was not written: " + entity));
    }

    /**
     * Convert a Bedrock actor to Java NBT, and check reading the Java NBT back gives the same entity.
     *
     * @param bedrock the actor NBT.
     * @return the Java NBT.
     */
    public static CompoundTag convert(CompoundTag bedrock) {
        Entity entity = readBedrock(bedrock);
        CompoundTag java = writeJava(entity);
        Entity roundTrip = JAVA.entityResolver().to(java).orElseThrow(() -> new AssertionError("Java entity was not read back: " + java));
        assertTrue(entity.equals(roundTrip), () -> "Java round trip differs:\n  " + entity + "\n  " + roundTrip);
        return java;
    }
}

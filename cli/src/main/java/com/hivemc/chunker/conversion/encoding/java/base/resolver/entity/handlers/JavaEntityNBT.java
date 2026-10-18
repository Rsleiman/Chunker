package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Helpers for encoding entity values in Java NBT.
 */
public final class JavaEntityNBT {
    private JavaEntityNBT() {
    }

    /**
     * Encode a UUID as the int array Java uses (most significant first).
     *
     * @param uuid the UUID.
     * @return the 4 ints of the UUID.
     */
    public static int[] toIntArray(UUID uuid) {
        return new int[]{
                (int) (uuid.getMostSignificantBits() >> 32),
                (int) uuid.getMostSignificantBits(),
                (int) (uuid.getLeastSignificantBits() >> 32),
                (int) uuid.getLeastSignificantBits()
        };
    }

    /**
     * Decode a UUID from the int array Java uses.
     *
     * @param ints the int array.
     * @return the UUID or null if the array isn't a UUID.
     */
    @Nullable
    public static UUID fromIntArray(int @Nullable [] ints) {
        if (ints == null || ints.length != 4) return null;
        return new UUID(
                ((long) ints[0] << 32) | (ints[1] & 0xFFFFFFFFL),
                ((long) ints[2] << 32) | (ints[3] & 0xFFFFFFFFL)
        );
    }
}

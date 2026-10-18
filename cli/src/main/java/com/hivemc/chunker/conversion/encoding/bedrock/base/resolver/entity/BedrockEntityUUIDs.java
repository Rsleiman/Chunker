package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns Bedrock actor IDs (the signed 64-bit UniqueID) into UUIDs.
 * <p>
 * Every entity gets a name-based UUID (version 5, OID namespace) of {@code "bedrock-actor:<UniqueID>"}, so references
 * between entities (owners, riders, leashes, love causes) still resolve after conversion and the result is the same on
 * every run.
 * <p>
 * Players are referenced the same way: Bedrock worlds don't store a player's XUID, so the owner of a tamed mob gets the
 * derived UUID of the player's UniqueID, which matches no real account. Mapping players to their real Java UUIDs
 * (Floodgate uses {@code new UUID(0, xuid)} for Bedrock accounts) only needs a change to
 * {@link #referenceUUID(long)}, which every player reference goes through.
 */
public final class BedrockEntityUUIDs {
    /**
     * The RFC 4122 namespace for ISO OIDs.
     */
    private static final UUID NAMESPACE_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");

    private BedrockEntityUUIDs() {
    }

    /**
     * Get the UUID used for a Bedrock actor.
     *
     * @param uniqueId the UniqueID of the actor.
     * @return the derived UUID.
     */
    public static UUID toUUID(long uniqueId) {
        return nameUUIDv5(NAMESPACE_OID, "bedrock-actor:" + uniqueId);
    }

    /**
     * Get the UUID of an actor referenced by its UniqueID, which may be a player (owners, leash holders, love causes,
     * projectile owners).
     *
     * @param uniqueId the UniqueID which is referenced, -1 is used by Bedrock when there is none.
     * @return the UUID to use or empty if nothing is referenced.
     */
    public static Optional<UUID> referenceUUID(long uniqueId) {
        if (uniqueId == -1) return Optional.empty();
        return Optional.of(toUUID(uniqueId));
    }

    /**
     * Create a name-based version 5 (SHA-1) UUID.
     *
     * @param namespace the namespace UUID.
     * @param name      the name.
     * @return the UUID.
     */
    static UUID nameUUIDv5(UUID namespace, String name) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(ByteBuffer.allocate(16)
                    .putLong(namespace.getMostSignificantBits())
                    .putLong(namespace.getLeastSignificantBits())
                    .array());
            byte[] hash = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0F) | 0x50); // Version 5
            hash[8] = (byte) ((hash[8] & 0x3F) | 0x80); // IETF variant
            ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 16);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }
}

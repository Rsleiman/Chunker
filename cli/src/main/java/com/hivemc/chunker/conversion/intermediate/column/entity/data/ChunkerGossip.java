package com.hivemc.chunker.conversion.intermediate.column.entity.data;

import java.util.UUID;

/**
 * A piece of gossip a villager keeps about a player.
 *
 * @param type   the type of gossip.
 * @param target the UUID of the player it is about.
 * @param value  the strength, from 1 to the type's maximum.
 */
public record ChunkerGossip(ChunkerGossipType type, UUID target, int value) {
}

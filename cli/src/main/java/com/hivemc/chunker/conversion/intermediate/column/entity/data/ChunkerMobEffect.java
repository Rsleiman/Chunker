package com.hivemc.chunker.conversion.intermediate.column.entity.data;

import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.potion.ChunkerEffectType;

/**
 * An active status effect on a living entity.
 *
 * @param type          the effect type.
 * @param amplifier     the amplifier (0 is level 1).
 * @param duration      the remaining duration in ticks.
 * @param ambient       whether the effect came from a beacon or conduit.
 * @param showParticles whether the effect shows particles.
 */
public record ChunkerMobEffect(ChunkerEffectType type, int amplifier, int duration, boolean ambient,
                               boolean showParticles) {
}

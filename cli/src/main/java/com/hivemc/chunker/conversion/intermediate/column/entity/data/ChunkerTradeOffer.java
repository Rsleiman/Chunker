package com.hivemc.chunker.conversion.intermediate.column.entity.data;

import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A trade offered by a villager or wandering trader.
 *
 * @param buy             the first item asked for.
 * @param buyB            the second item asked for, if any.
 * @param sell            the item given.
 * @param uses            how many times the trade has been used since it was restocked.
 * @param maxUses         how many times the trade can be used before restocking.
 * @param rewardExp       whether the trade rewards the player with experience.
 * @param xp              the experience the trader gains.
 * @param priceMultiplier the price multiplier used for demand and reputation.
 * @param demand          the current demand.
 * @param specialPrice    the current price adjustment.
 */
public record ChunkerTradeOffer(ChunkerItemStack buy, @Nullable ChunkerItemStack buyB, ChunkerItemStack sell,
                                int uses, int maxUses, boolean rewardExp, int xp, float priceMultiplier, int demand,
                                int specialPrice) {
}

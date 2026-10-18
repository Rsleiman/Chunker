package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerTradeOffer;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerProfession;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Reads Bedrock villagers, zombie villagers and wandering traders.
 * <p>
 * Bedrock rolls the offers of all five tiers when a villager gets its profession, while Java only stores the
 * unlocked ones and rolls the next tier at each level-up. Only offers up to the villager's TradeTier are kept, and
 * the level is TradeTier + 1 (the experience thresholds are the same on both sides).
 */
public final class BedrockVillagerReader {
    /**
     * Bedrock villager Variant to the profession.
     */
    private static final ChunkerVillagerProfession[] PROFESSIONS = {
            ChunkerVillagerProfession.NONE,
            ChunkerVillagerProfession.FARMER,
            ChunkerVillagerProfession.FISHERMAN,
            ChunkerVillagerProfession.SHEPHERD,
            ChunkerVillagerProfession.FLETCHER,
            ChunkerVillagerProfession.LIBRARIAN,
            ChunkerVillagerProfession.CARTOGRAPHER,
            ChunkerVillagerProfession.CLERIC,
            ChunkerVillagerProfession.ARMORER,
            ChunkerVillagerProfession.WEAPONSMITH,
            ChunkerVillagerProfession.TOOLSMITH,
            ChunkerVillagerProfession.BUTCHER,
            ChunkerVillagerProfession.LEATHERWORKER,
            ChunkerVillagerProfession.MASON,
            ChunkerVillagerProfession.NITWIT
    };
    /**
     * Bedrock villager MarkVariant to the biome type.
     */
    private static final ChunkerVillagerType[] TYPES = {
            ChunkerVillagerType.PLAINS,
            ChunkerVillagerType.DESERT,
            ChunkerVillagerType.JUNGLE,
            ChunkerVillagerType.SAVANNA,
            ChunkerVillagerType.SNOW,
            ChunkerVillagerType.SWAMP,
            ChunkerVillagerType.TAIGA
    };

    private BedrockVillagerReader() {
    }

    /**
     * Read the villager data.
     *
     * @param resolvers the resolvers.
     * @param input     the actor NBT.
     * @param entity    the entity to fill.
     * @param groups    the active component groups.
     */
    public static void read(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity, Set<String> groups) {
        boolean villager = entity.getEntityType() == ChunkerVanillaEntityType.VILLAGER;
        boolean zombieVillager = entity.getEntityType() == ChunkerVanillaEntityType.ZOMBIE_VILLAGER;
        int tradeTier = Math.max(0, input.getInt("TradeTier", 0));

        if (villager || zombieVillager) {
            entity.put(ChunkerEntityProperty.VILLAGER_PROFESSION, readProfession(input, groups, zombieVillager));
            int markVariant = input.getInt("MarkVariant", 0);
            entity.put(ChunkerEntityProperty.VILLAGER_TYPE, markVariant >= 0 && markVariant < TYPES.length ? TYPES[markVariant] : ChunkerVillagerType.PLAINS);
            entity.put(ChunkerEntityProperty.VILLAGER_LEVEL, Math.min(5, tradeTier + 1));
            entity.put(ChunkerEntityProperty.VILLAGER_XP, input.getInt("TradeExperience", 0));
        }

        if (villager || entity.getEntityType() == ChunkerVanillaEntityType.WANDERING_TRADER) {
            readOffers(resolvers, input, entity, villager ? tradeTier : Integer.MAX_VALUE);
            BedrockLivingEntityReader.readInventory(resolvers, input, entity);
        }

        if (villager) {
            input.getOptionalValue("UniqueID", Long.class).ifPresent(uniqueId -> {
                Map<ChunkerVillagerMemory, ChunkerGlobalPosition> memories = resolvers.getVillagerMemories(uniqueId);
                if (!memories.isEmpty()) {
                    entity.put(ChunkerEntityProperty.VILLAGER_MEMORIES, new EnumMap<>(memories));
                }
            });
        }
    }

    private static ChunkerVillagerProfession readProfession(CompoundTag input, Set<String> groups, boolean zombieVillager) {
        // Zombie villagers keep their profession only in the component groups (their Variant is always 0)
        if (zombieVillager) {
            for (ChunkerVillagerProfession profession : ChunkerVillagerProfession.values()) {
                if (profession != ChunkerVillagerProfession.NONE && groups.contains(profession.getName())) {
                    return profession;
                }
            }
            return ChunkerVillagerProfession.NONE;
        }

        int variant = input.getInt("Variant", 0);
        return variant >= 0 && variant < PROFESSIONS.length ? PROFESSIONS[variant] : ChunkerVillagerProfession.NONE;
    }

    private static void readOffers(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity, int maxTier) {
        CompoundTag offers = input.getCompound("Offers");
        if (offers == null) return;
        ListTag<CompoundTag, Map<String, Tag<?>>> recipes = offers.getList("Recipes", CompoundTag.class, null);
        if (recipes == null) return;

        List<ChunkerTradeOffer> tradeOffers = new ArrayList<>(recipes.size());
        for (CompoundTag recipe : recipes) {
            if (recipe.getInt("tier", 0) > maxTier) continue;

            // buyCountA / buyCountB hold the base price, the item counts include the current demand
            ChunkerItemStack buy = readCost(resolvers, recipe.getCompound("buyA"), recipe.getInt("buyCountA", 0));
            ChunkerItemStack sell = BedrockLivingEntityReader.readItem(resolvers, recipe.getCompound("sell"));
            if (buy == null || sell == null) continue;
            ChunkerItemStack buyB = recipe.getInt("buyCountB", 0) > 0 ? readCost(resolvers, recipe.getCompound("buyB"), recipe.getInt("buyCountB", 0)) : null;

            tradeOffers.add(new ChunkerTradeOffer(
                    buy,
                    buyB,
                    sell,
                    recipe.getInt("uses", 0),
                    recipe.getInt("maxUses", 12),
                    recipe.getByte("rewardExp", (byte) 1) == 1,
                    recipe.getInt("traderExp", 1),
                    recipe.getFloat("priceMultiplierA", 0.05F),
                    recipe.getInt("demand", 0),
                    0
            ));
        }
        entity.put(ChunkerEntityProperty.TRADE_OFFERS, tradeOffers);
    }

    @Nullable
    private static ChunkerItemStack readCost(BedrockResolvers resolvers, @Nullable CompoundTag itemTag, int count) {
        ChunkerItemStack item = BedrockLivingEntityReader.readItem(resolvers, itemTag);
        if (item == null) return null;
        if (count > 0) {
            item.put(ChunkerItemProperty.AMOUNT, count);
        }
        return item;
    }
}

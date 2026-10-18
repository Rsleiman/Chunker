package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerTradeOffer;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerProfession;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerType;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Writes and reads Java villager data (VillagerData, Xp, Offers and brain memories), as stored from 1.21.5 onwards.
 */
public final class JavaVillagerData {
    private JavaVillagerData() {
    }

    /**
     * Write the villager data.
     *
     * @param resolvers the resolvers.
     * @param output    the entity NBT.
     * @param entity    the entity.
     */
    public static void write(JavaResolvers resolvers, CompoundTag output, GenericEntity entity) {
        ChunkerVillagerProfession profession = entity.get(ChunkerEntityProperty.VILLAGER_PROFESSION);
        ChunkerVillagerType type = entity.get(ChunkerEntityProperty.VILLAGER_TYPE);
        Integer level = entity.get(ChunkerEntityProperty.VILLAGER_LEVEL);
        if (profession != null || type != null || level != null) {
            CompoundTag villagerData = new CompoundTag();
            villagerData.put("type", "minecraft:" + (type == null ? ChunkerVillagerType.PLAINS : type).getName());
            villagerData.put("profession", "minecraft:" + (profession == null ? ChunkerVillagerProfession.NONE : profession).getName());
            villagerData.put("level", level == null ? 1 : level);
            output.put("VillagerData", villagerData);
        }
        Integer xp = entity.get(ChunkerEntityProperty.VILLAGER_XP);
        if (xp != null) {
            output.put("Xp", xp.intValue());
        }

        List<ChunkerTradeOffer> offers = entity.get(ChunkerEntityProperty.TRADE_OFFERS);
        if (offers != null) {
            ListTag<CompoundTag, Map<String, Tag<?>>> recipes = new ListTag<>(TagType.COMPOUND, offers.size());
            for (ChunkerTradeOffer offer : offers) {
                Optional<CompoundTag> buy = writeCost(resolvers, offer.buy());
                Optional<CompoundTag> sell = JavaLivingEntityData.writeItem(resolvers, offer.sell());
                if (buy.isEmpty() || sell.isEmpty()) continue;

                CompoundTag recipe = new CompoundTag();
                recipe.put("buy", buy.get());
                writeCost(resolvers, offer.buyB()).ifPresent(buyB -> recipe.put("buyB", buyB));
                recipe.put("sell", sell.get());
                recipe.put("uses", offer.uses());
                recipe.put("maxUses", offer.maxUses());
                recipe.put("rewardExp", offer.rewardExp() ? (byte) 1 : (byte) 0);
                recipe.put("xp", offer.xp());
                recipe.put("priceMultiplier", offer.priceMultiplier());
                recipe.put("demand", offer.demand());
                recipe.put("specialPrice", offer.specialPrice());
                recipes.add(recipe);
            }
            CompoundTag offersTag = new CompoundTag();
            offersTag.put("Recipes", recipes);
            output.put("Offers", offersTag);
        }

        Map<ChunkerVillagerMemory, ChunkerGlobalPosition> memories = entity.get(ChunkerEntityProperty.VILLAGER_MEMORIES);
        if (memories != null && !memories.isEmpty()) {
            CompoundTag memoriesTag = new CompoundTag();
            for (Map.Entry<ChunkerVillagerMemory, ChunkerGlobalPosition> entry : memories.entrySet()) {
                ChunkerGlobalPosition position = entry.getValue();
                CompoundTag value = new CompoundTag();
                value.put("dimension", position.dimension().getIdentifier());
                value.put("pos", new int[]{position.x(), position.y(), position.z()});
                CompoundTag memory = new CompoundTag();
                memory.put("value", value);
                memoriesTag.put("minecraft:" + entry.getKey().getName(), memory);
            }
            CompoundTag brain = new CompoundTag();
            brain.put("memories", memoriesTag);
            output.put("Brain", brain);
        }
    }

    /**
     * Write the item a trade asks for (an item cost: id, count and components).
     *
     * @param resolvers the resolvers.
     * @param item      the item.
     * @return the cost NBT if it could be written.
     */
    private static Optional<CompoundTag> writeCost(JavaResolvers resolvers, @Nullable ChunkerItemStack item) {
        return JavaLivingEntityData.writeItem(resolvers, item).map(itemTag -> {
            CompoundTag cost = new CompoundTag();
            cost.put("id", itemTag.getString("id"));
            cost.put("count", itemTag.getInt("count", 1));
            CompoundTag components = itemTag.getCompound("components");
            if (components != null) {
                cost.put("components", components);
            }
            return cost;
        });
    }

    /**
     * Read the villager data.
     *
     * @param resolvers the resolvers.
     * @param input     the entity NBT.
     * @param entity    the entity to fill.
     */
    public static void read(JavaResolvers resolvers, CompoundTag input, GenericEntity entity) {
        CompoundTag villagerData = input.getCompound("VillagerData");
        if (villagerData != null) {
            entity.put(ChunkerEntityProperty.VILLAGER_TYPE, parseEnum(ChunkerVillagerType.class, villagerData.getString("type", ""), ChunkerVillagerType.PLAINS));
            entity.put(ChunkerEntityProperty.VILLAGER_PROFESSION, parseEnum(ChunkerVillagerProfession.class, villagerData.getString("profession", ""), ChunkerVillagerProfession.NONE));
            entity.put(ChunkerEntityProperty.VILLAGER_LEVEL, villagerData.getInt("level", 1));
        }
        input.getOptionalValue("Xp", Integer.class).ifPresent(value -> entity.put(ChunkerEntityProperty.VILLAGER_XP, value));

        CompoundTag offersTag = input.getCompound("Offers");
        if (offersTag != null) {
            ListTag<CompoundTag, Map<String, Tag<?>>> recipes = offersTag.getList("Recipes", CompoundTag.class, null);
            List<ChunkerTradeOffer> offers = new ArrayList<>();
            if (recipes != null) {
                for (CompoundTag recipe : recipes) {
                    CompoundTag buy = recipe.getCompound("buy");
                    CompoundTag sell = recipe.getCompound("sell");
                    if (buy == null || sell == null) continue;
                    CompoundTag buyB = recipe.getCompound("buyB");
                    offers.add(new ChunkerTradeOffer(
                            resolvers.readItem(buy),
                            buyB == null ? null : resolvers.readItem(buyB),
                            resolvers.readItem(sell),
                            recipe.getInt("uses", 0),
                            recipe.getInt("maxUses", 4),
                            recipe.getByte("rewardExp", (byte) 1) == 1,
                            recipe.getInt("xp", 1),
                            recipe.getFloat("priceMultiplier", 0),
                            recipe.getInt("demand", 0),
                            recipe.getInt("specialPrice", 0)
                    ));
                }
            }
            entity.put(ChunkerEntityProperty.TRADE_OFFERS, offers);
        }

        CompoundTag brain = input.getCompound("Brain");
        CompoundTag memoriesTag = brain == null ? null : brain.getCompound("memories");
        if (memoriesTag != null) {
            Map<ChunkerVillagerMemory, ChunkerGlobalPosition> memories = new EnumMap<>(ChunkerVillagerMemory.class);
            for (ChunkerVillagerMemory memory : ChunkerVillagerMemory.values()) {
                CompoundTag memoryTag = memoriesTag.getCompound("minecraft:" + memory.getName());
                CompoundTag value = memoryTag == null ? null : memoryTag.getCompound("value");
                if (value == null) continue;
                int[] pos = value.getIntArray("pos", null);
                Dimension dimension = getDimension(value.getString("dimension", ""));
                if (pos == null || pos.length != 3 || dimension == null) continue;
                memories.put(memory, new ChunkerGlobalPosition(dimension, pos[0], pos[1], pos[2]));
            }
            if (!memories.isEmpty()) {
                entity.put(ChunkerEntityProperty.VILLAGER_MEMORIES, memories);
            }
        }
    }

    @Nullable
    private static Dimension getDimension(String identifier) {
        for (Dimension dimension : List.of(Dimension.OVERWORLD, Dimension.NETHER, Dimension.THE_END)) {
            if (dimension.getIdentifier().equals(identifier)) return dimension;
        }
        return null;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String identifier, E fallback) {
        String name = identifier.startsWith("minecraft:") ? identifier.substring("minecraft:".length()) : identifier;
        try {
            return Enum.valueOf(enumClass, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

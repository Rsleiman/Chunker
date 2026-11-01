package com.hivemc.chunker.conversion.encoding.java.base.resolver.itemstack;

import com.google.gson.JsonElement;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerEntityFlag;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.util.JsonTextUtil;

import java.util.Map;

/**
 * Writes the components of a Java mob bucket (1.21.5 onwards) from the mob inside it: the variant components and
 * bucket_entity_data, as Java 26.3 saves them (Bucketable, Axolotl, Salmon and TropicalFish saveToBucketTag).
 */
public final class JavaBucketData {
    /**
     * The Java axolotl variants by ID.
     */
    private static final String[] AXOLOTL_VARIANTS = {"lucy", "wild", "gold", "cyan", "blue"};
    /**
     * The Java tropical fish patterns by body size (small, large) and pattern index.
     */
    private static final String[][] TROPICAL_FISH_PATTERNS = {
            {"kob", "sunstreak", "snooper", "dasher", "brinely", "spotty"},
            {"flopper", "stripey", "glitter", "blockfish", "betty", "clayfish"}
    };

    private JavaBucketData() {
    }

    /**
     * Write the bucket components.
     *
     * @param resolvers  the resolvers.
     * @param components the components of the item.
     * @param entity     the mob in the bucket.
     */
    public static void write(JavaResolvers resolvers, CompoundTag components, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();
        JsonElement customName = entity.get(ChunkerEntityProperty.CUSTOM_NAME);
        if (customName != null) {
            components.put("minecraft:custom_name", JsonTextUtil.toNBT(customName, resolvers.dataVersion()));
        }

        CompoundTag data = new CompoundTag();
        if (Boolean.TRUE.equals(entity.get(ChunkerEntityProperty.INVULNERABLE))) {
            data.put("Invulnerable", (byte) 1);
        }
        if (Boolean.TRUE.equals(entity.get(ChunkerEntityProperty.PERSISTENT))) {
            data.put("PersistenceRequired", (byte) 1);
        }
        Float health = entity.get(ChunkerEntityProperty.HEALTH);
        if (health != null) {
            data.put("Health", health.floatValue());
        }

        Integer variantId = entity.get(ChunkerEntityProperty.VARIANT_ID);
        if (type == ChunkerVanillaEntityType.AXOLOTL) {
            if (variantId != null && variantId >= 0 && variantId < AXOLOTL_VARIANTS.length) {
                components.put("minecraft:axolotl/variant", AXOLOTL_VARIANTS[variantId]);
            }
            Integer age = entity.get(ChunkerEntityProperty.AGE);
            data.put("Age", age == null ? 0 : age);
            Map<ChunkerEntityFlag, Boolean> flags = entity.get(ChunkerEntityProperty.FLAGS);
            data.put("AgeLocked", flags != null && Boolean.TRUE.equals(flags.get(ChunkerEntityFlag.AGE_LOCKED)) ? (byte) 1 : (byte) 0);
        } else if (type == ChunkerVanillaEntityType.TROPICAL_FISH && variantId != null) {
            // Packed as size | pattern << 8 | base color << 16 | pattern color << 24
            int size = variantId & 0xFF;
            int pattern = (variantId >> 8) & 0xFF;
            if (size < TROPICAL_FISH_PATTERNS.length && pattern < TROPICAL_FISH_PATTERNS[size].length) {
                components.put("minecraft:tropical_fish/pattern", TROPICAL_FISH_PATTERNS[size][pattern]);
            }
            ChunkerDyeColor.getColorByID((variantId >> 16) & 0xFF)
                    .ifPresent(color -> components.put("minecraft:tropical_fish/base_color", color.getName()));
            ChunkerDyeColor.getColorByID((variantId >> 24) & 0xFF)
                    .ifPresent(color -> components.put("minecraft:tropical_fish/pattern_color", color.getName()));
        } else if (type == ChunkerVanillaEntityType.SALMON) {
            String size = entity.get(ChunkerEntityProperty.VARIANT);
            if (size != null) {
                components.put("minecraft:salmon/size", size);
            }
        }

        components.put("minecraft:bucket_entity_data", data);
    }
}

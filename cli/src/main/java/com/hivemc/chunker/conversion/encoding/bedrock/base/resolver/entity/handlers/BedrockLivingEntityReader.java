package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.item.ChunkerVanillaItemType;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.potion.ChunkerEffectType;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerBlockPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerEquipmentSlot;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerMobEffect;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.FloatTag;
import com.hivemc.chunker.nbt.tags.primitive.IntTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Reads the data of Bedrock living entities (health, equipment, effects, age, taming) and the type specific data
 * (variants, colors, inventories). The decoding tables come from the component groups of the vanilla behaviour pack
 * of BDS 1.26.52.
 */
public final class BedrockLivingEntityReader {
    /**
     * Bedrock cat Variant to the Java cat variant (Bedrock "tuxedo" is Java "black", Bedrock "black" is "all_black").
     */
    private static final String[] CAT_VARIANTS = {
            "white", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "tabby",
            "all_black", "jellie"
    };
    /**
     * The wolf coats, set by the minecraft:wolf_&lt;coat&gt; component group.
     */
    private static final Set<String> WOLF_VARIANTS = Set.of(
            "ashen", "black", "chestnut", "pale", "rusty", "snowy", "spotted", "striped", "woods"
    );
    /**
     * Bedrock mooshroom Variant to the Java type.
     */
    private static final String[] MOOSHROOM_VARIANTS = {"red", "brown"};
    /**
     * Bedrock fox Variant to the Java type (not tested in game).
     */
    private static final String[] FOX_VARIANTS = {"red", "snow"};
    /**
     * Bedrock Armor list index to the equipment slot.
     */
    private static final ChunkerEquipmentSlot[] ARMOR_SLOTS = {
            ChunkerEquipmentSlot.HEAD, ChunkerEquipmentSlot.CHEST, ChunkerEquipmentSlot.LEGS,
            ChunkerEquipmentSlot.FEET, ChunkerEquipmentSlot.BODY
    };
    /**
     * The drop chance Java uses for items which must drop (e.g. picked up from a player).
     */
    private static final float GUARANTEED_DROP_CHANCE = 2.0F;
    /**
     * The age of a newborn baby in ticks.
     */
    private static final int BABY_AGE = -24000;

    private BedrockLivingEntityReader() {
    }

    /**
     * Read the living entity data and the type specific data.
     *
     * @param resolvers the resolvers.
     * @param input     the actor NBT.
     * @param entity    the entity to fill.
     */
    public static void read(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();
        Set<String> groups = getComponentGroups(input);

        readHealth(input, entity, type);
        readEquipment(resolvers, input, entity, type, groups);
        readEffects(resolvers, input, entity);

        input.getOptionalValue("Persistent", Byte.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.PERSISTENT, value == 1));
        input.getOptionalValue("canPickupItems", Byte.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.CAN_PICK_UP_LOOT, value == 1));

        readAge(input, entity, type);
        readTaming(input, entity, type);
        readTypeSpecific(resolvers, input, entity, type, groups);
    }

    /**
     * Get the active component groups of an actor, without the minecraft: namespace (Bedrock uses both forms).
     *
     * @param input the actor NBT.
     * @return the names of the active groups.
     */
    public static Set<String> getComponentGroups(CompoundTag input) {
        List<String> definitions = input.getListValues("definitions", StringTag.class, null);
        if (definitions == null) return Collections.emptySet();

        Set<String> groups = new HashSet<>();
        for (String definition : definitions) {
            if (!definition.startsWith("+")) continue;
            String name = definition.substring(1);
            groups.add(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
        }
        return groups;
    }

    private static void readHealth(CompoundTag input, GenericEntity entity, ChunkerEntityType type) {
        CompoundTag health = getAttribute(input, "minecraft:health");
        if (health != null) {
            entity.put(ChunkerEntityProperty.HEALTH, health.getFloat("Current", health.getFloat("Base", 0)));

            // Only horses and wolves have a maximum which differs per entity
            if (GenericEntityTypes.HORSES.contains(type) || type == ChunkerVanillaEntityType.WOLF) {
                entity.put(ChunkerEntityProperty.MAX_HEALTH, (double) health.getFloat("Max", health.getFloat("Base", 0)));
            }
        }

        if (GenericEntityTypes.HORSES.contains(type)) {
            CompoundTag movement = getAttribute(input, "minecraft:movement");
            if (movement != null) {
                entity.put(ChunkerEntityProperty.MOVEMENT_SPEED, (double) movement.getFloat("Base", 0));
            }
            CompoundTag jumpStrength = getAttribute(input, "minecraft:horse.jump_strength");
            if (jumpStrength != null) {
                entity.put(ChunkerEntityProperty.JUMP_STRENGTH, (double) jumpStrength.getFloat("Base", 0));
            }
        }
    }

    @Nullable
    private static CompoundTag getAttribute(CompoundTag input, String name) {
        ListTag<CompoundTag, Map<String, Tag<?>>> attributes = input.getList("Attributes", CompoundTag.class, null);
        if (attributes == null) return null;
        for (CompoundTag attribute : attributes) {
            if (name.equals(attribute.getString("Name", null))) return attribute;
        }
        return null;
    }

    private static void readEquipment(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity, ChunkerEntityType type, Set<String> groups) {
        Map<ChunkerEquipmentSlot, ChunkerItemStack> equipment = new EnumMap<>(ChunkerEquipmentSlot.class);
        Map<ChunkerEquipmentSlot, Float> dropChances = new EnumMap<>(ChunkerEquipmentSlot.class);

        // Armor (head, chest, legs, feet, body), main hand and off hand
        ListTag<CompoundTag, Map<String, Tag<?>>> armor = input.getList("Armor", CompoundTag.class, null);
        if (armor != null) {
            for (int i = 0; i < Math.min(armor.size(), ARMOR_SLOTS.length); i++) {
                readEquipmentItem(resolvers, armor.get(i), ARMOR_SLOTS[i], equipment, dropChances);
            }
        }
        readHandItem(resolvers, input, "Mainhand", ChunkerEquipmentSlot.MAINHAND, equipment, dropChances);
        readHandItem(resolvers, input, "Offhand", ChunkerEquipmentSlot.OFFHAND, equipment, dropChances);

        // Explicit drop chances
        ListTag<CompoundTag, Map<String, Tag<?>>> slotDropChances = input.getList("SlotDropChances", CompoundTag.class, null);
        if (slotDropChances != null) {
            for (CompoundTag dropChance : slotDropChances) {
                ChunkerEquipmentSlot slot = getSlotByName(dropChance.getString("Slot", ""));
                if (slot == null || dropChances.containsKey(slot)) continue;
                dropChances.put(slot, dropChance.getFloat("DropChance", 0));
            }
        }

        // Horses keep the saddle (slot 0) and armor or carpet (slot 1) in their inventory, then the chest
        Map<Integer, ChunkerItemStack> inventory = new TreeMap<>();
        if (GenericEntityTypes.HORSES.contains(type)) {
            ListTag<CompoundTag, Map<String, Tag<?>>> chestItems = input.getList("ChestItems", CompoundTag.class, null);
            if (chestItems != null) {
                boolean chested = GenericEntityTypes.CHESTED_HORSES.contains(type) && input.getByte("Chested", (byte) 0) == 1;
                for (CompoundTag chestItem : chestItems) {
                    int slot = chestItem.getByte("Slot", (byte) 0);
                    ChunkerItemStack item = readItem(resolvers, chestItem);
                    if (item == null) continue;
                    if (slot == 0) {
                        equipment.put(ChunkerEquipmentSlot.SADDLE, item);
                    } else if (slot == 1) {
                        equipment.put(ChunkerEquipmentSlot.BODY, item);
                    } else if (chested) {
                        inventory.put(slot - 2, item);
                    }
                }
            }
        }

        // Saddled mobs without a saddle item (pigs, striders)
        boolean saddled = input.getByte("Saddled", (byte) 0) == 1 || groups.contains("strider_saddled");
        if (saddled && !equipment.containsKey(ChunkerEquipmentSlot.SADDLE)) {
            ChunkerItemStack saddle = new ChunkerItemStack(ChunkerVanillaItemType.SADDLE);
            saddle.put(ChunkerItemProperty.AMOUNT, 1);
            equipment.put(ChunkerEquipmentSlot.SADDLE, saddle);
        }

        if (!equipment.isEmpty()) {
            entity.put(ChunkerEntityProperty.EQUIPMENT, equipment);
        }
        if (!dropChances.isEmpty()) {
            entity.put(ChunkerEntityProperty.DROP_CHANCES, dropChances);
        }
        if (!inventory.isEmpty()) {
            entity.put(ChunkerEntityProperty.INVENTORY, inventory);
        }
    }

    private static void readHandItem(BedrockResolvers resolvers, CompoundTag input, String key, ChunkerEquipmentSlot slot, Map<ChunkerEquipmentSlot, ChunkerItemStack> equipment, Map<ChunkerEquipmentSlot, Float> dropChances) {
        ListTag<CompoundTag, Map<String, Tag<?>>> hand = input.getList(key, CompoundTag.class, null);
        if (hand != null && hand.size() > 0) {
            readEquipmentItem(resolvers, hand.get(0), slot, equipment, dropChances);
        }
    }

    private static void readEquipmentItem(BedrockResolvers resolvers, CompoundTag itemTag, ChunkerEquipmentSlot slot, Map<ChunkerEquipmentSlot, ChunkerItemStack> equipment, Map<ChunkerEquipmentSlot, Float> dropChances) {
        ChunkerItemStack item = readItem(resolvers, itemTag);
        if (item == null) return;
        equipment.put(slot, item);

        // Items picked up (e.g. a player's armor) always drop, as in Java
        if (itemTag.getByte("WasPickedUp", (byte) 0) == 1) {
            dropChances.put(slot, GUARANTEED_DROP_CHANCE);
        }
    }

    /**
     * Read an item stack, ignoring empty slots.
     *
     * @param resolvers the resolvers.
     * @param itemTag   the item NBT.
     * @return the item or null if the slot is empty.
     */
    @Nullable
    public static ChunkerItemStack readItem(BedrockResolvers resolvers, @Nullable CompoundTag itemTag) {
        if (itemTag == null || itemTag.getString("Name", "").isEmpty()) return null;
        if (itemTag.getByte("Count", (byte) 0) <= 0) return null;
        ChunkerItemStack item = resolvers.readItem(itemTag);
        return item.getIdentifier().isAir() ? null : item;
    }

    @Nullable
    private static ChunkerEquipmentSlot getSlotByName(String name) {
        for (ChunkerEquipmentSlot slot : ChunkerEquipmentSlot.values()) {
            if (slot.getName().equals(name)) return slot;
        }
        return null;
    }

    private static void readEffects(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ListTag<CompoundTag, Map<String, Tag<?>>> activeEffects = input.getList("ActiveEffects", CompoundTag.class, null);
        if (activeEffects == null || activeEffects.size() == 0) return;

        List<ChunkerMobEffect> effects = new ArrayList<>(activeEffects.size());
        for (CompoundTag effect : activeEffects) {
            ChunkerEffectType effectType = resolvers.readEffectID(effect.getByte("Id", (byte) 0) & 0xFF);
            if (effectType == null || effectType == ChunkerEffectType.EMPTY) continue;
            effects.add(new ChunkerMobEffect(
                    effectType,
                    effect.getByte("Amplifier", (byte) 0),
                    effect.getInt("Duration", 0),
                    effect.getByte("Ambient", (byte) 0) == 1,
                    effect.getByte("ShowParticles", (byte) 1) == 1
            ));
        }
        if (!effects.isEmpty()) {
            entity.put(ChunkerEntityProperty.EFFECTS, effects);
        }
    }

    private static void readAge(CompoundTag input, GenericEntity entity, ChunkerEntityType type) {
        boolean baby = input.getByte("IsBaby", (byte) 0) == 1;
        if (GenericEntityTypes.AGEABLE.contains(type)) {
            entity.put(ChunkerEntityProperty.BABY, baby);

            // Babies count up from about -24000 like Java, adults store the breeding cooldown separately
            if (baby) {
                int age = input.getOptionalValue("Age", Integer.class).filter(value -> value < 0).orElse(BABY_AGE);
                entity.put(ChunkerEntityProperty.AGE, age);
            } else {
                int breedCooldown = input.getInt("BreedCooldown", 0);
                if (breedCooldown > 0) {
                    entity.put(ChunkerEntityProperty.AGE, breedCooldown);
                }
            }

            int inLove = input.getInt("InLove", 0);
            if (inLove > 0) {
                entity.put(ChunkerEntityProperty.IN_LOVE, inLove);
                BedrockEntityUUIDs.referenceUUID(input.getLong("LoveCause", -1))
                        .ifPresent(uuid -> entity.put(ChunkerEntityProperty.LOVE_CAUSE, uuid));
            }
        } else if (GenericEntityTypes.BABY_FLAG.contains(type)) {
            entity.put(ChunkerEntityProperty.BABY, baby);
        }
    }

    private static void readTaming(CompoundTag input, GenericEntity entity, ChunkerEntityType type) {
        if (!GenericEntityTypes.SITTING_PETS.contains(type) && !GenericEntityTypes.HORSES.contains(type)) return;

        boolean tamed = input.getByte("IsTamed", (byte) 0) == 1;
        entity.put(ChunkerEntityProperty.TAMED, tamed);
        if (tamed) {
            BedrockEntityUUIDs.referenceUUID(input.getLong("OwnerNew", -1))
                    .ifPresent(uuid -> entity.put(ChunkerEntityProperty.OWNER, uuid));
        }
        if (GenericEntityTypes.SITTING_PETS.contains(type)) {
            entity.put(ChunkerEntityProperty.SITTING, input.getByte("Sitting", (byte) 0) == 1);
        }
    }

    private static void readTypeSpecific(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity, ChunkerEntityType type, Set<String> groups) {
        if (!(type instanceof ChunkerVanillaEntityType vanillaType)) return;
        CompoundTag properties = input.getCompound("properties", new CompoundTag());
        int variant = input.getInt("Variant", 0);

        // Sound variants (cats, cows, pigs, chickens, wolves), Bedrock "default" is Java "classic"
        properties.getOptionalValue("minecraft:sound_variant", String.class)
                .ifPresent(sound -> entity.put(ChunkerEntityProperty.SOUND_VARIANT, sound.equals("default") ? "classic" : sound));

        switch (vanillaType) {
            case CAT -> {
                if (variant >= 0 && variant < CAT_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT, CAT_VARIANTS[variant]);
                }
                readCollar(input, entity);
            }
            case WOLF -> {
                for (String group : groups) {
                    if (group.startsWith("wolf_") && WOLF_VARIANTS.contains(group.substring("wolf_".length()))) {
                        entity.put(ChunkerEntityProperty.VARIANT, group.substring("wolf_".length()));
                    }
                }
                readCollar(input, entity);
            }
            case COW, PIG, CHICKEN -> properties.getOptionalValue("minecraft:climate_variant", String.class)
                    .ifPresent(climate -> entity.put(ChunkerEntityProperty.VARIANT, climate));
            case MOOSHROOM -> {
                if (variant >= 0 && variant < MOOSHROOM_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT, MOOSHROOM_VARIANTS[variant]);
                }
            }
            case FOX -> {
                if (variant >= 0 && variant < FOX_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT, FOX_VARIANTS[variant]);
                }
            }
            case SHEEP -> {
                ChunkerDyeColor.getColorByID(input.getByte("Color", (byte) 0))
                        .ifPresent(color -> entity.put(ChunkerEntityProperty.COLOR, color));
                entity.put(ChunkerEntityProperty.SHEARED, input.getByte("Sheared", (byte) 0) == 1);
            }
            case HORSE -> entity.put(ChunkerEntityProperty.VARIANT_ID, variant | (input.getInt("MarkVariant", 0) << 8));
            case LLAMA, TRADER_LLAMA -> entity.put(ChunkerEntityProperty.VARIANT_ID, variant);
            case TROPICAL_FISH -> entity.put(ChunkerEntityProperty.VARIANT_ID, variant
                    | input.getInt("MarkVariant", 0) << 8
                    | (input.getByte("Color", (byte) 0) & 0xFF) << 16
                    | (input.getByte("Color2", (byte) 0) & 0xFF) << 24);
            case IRON_GOLEM -> entity.put(ChunkerEntityProperty.PLAYER_CREATED, groups.contains("player_created"));
            case BEE -> {
                readHomePosition(input, entity);
                boolean hasNectar = properties.getOptionalValue("minecraft:has_nectar", Byte.class).map(value -> value == 1)
                        .orElse(groups.contains("has_nectar"));
                entity.put(ChunkerEntityProperty.HAS_NECTAR, hasNectar);
            }
            case TURTLE -> readHomePosition(input, entity);
            case PIGLIN -> {
                entity.put(ChunkerEntityProperty.CANNOT_HUNT, !groups.contains("hunter"));
                readInventory(resolvers, input, entity);
            }
            case ALLAY -> {
                readInventory(resolvers, input, entity);
                input.getOptionalValue("AllayDuplicationCooldown", Long.class)
                        .ifPresent(value -> entity.put(ChunkerEntityProperty.DUPLICATION_COOLDOWN, value));
            }
            case ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER ->
                    entity.put(ChunkerEntityProperty.CAN_BREAK_DOORS, groups.contains("can_break_doors"));
            case SLIME, MAGMA_CUBE, SULFUR_CUBE -> {
                int size = input.getOptional("Size", Tag.class)
                        .map(tag -> tag.getBoxedValue() instanceof Number number ? number.intValue() : 1)
                        .orElse(1);
                entity.put(ChunkerEntityProperty.SIZE, Math.max(1, size));
            }
            default -> {
            }
        }

        // Horses
        if (GenericEntityTypes.HORSES.contains(type)) {
            entity.put(ChunkerEntityProperty.TEMPER, input.getInt("Temper", 0));
            if (GenericEntityTypes.CHESTED_HORSES.contains(type)) {
                entity.put(ChunkerEntityProperty.CHESTED, input.getByte("Chested", (byte) 0) == 1);
            }
            if (type == ChunkerVanillaEntityType.LLAMA || type == ChunkerVanillaEntityType.TRADER_LLAMA) {
                entity.put(ChunkerEntityProperty.STRENGTH, Math.max(1, input.getInt("Strength", 3)));
            }
        }

        // Bedrock doesn't save a despawn timer which could be decoded, so it restarts (as for a new trader)
        if (type == ChunkerVanillaEntityType.WANDERING_TRADER || type == ChunkerVanillaEntityType.TRADER_LLAMA) {
            entity.put(ChunkerEntityProperty.DESPAWN_DELAY, 48000);
        }
    }

    private static void readCollar(CompoundTag input, GenericEntity entity) {
        if (input.getByte("IsTamed", (byte) 0) != 1) return;
        ChunkerDyeColor.getColorByID(input.getByte("Color", (byte) 14))
                .ifPresent(color -> entity.put(ChunkerEntityProperty.COLLAR_COLOR, color));
    }

    private static void readHomePosition(CompoundTag input, GenericEntity entity) {
        List<Float> homePos = input.getListValues("HomePos", FloatTag.class, null);
        if (homePos != null && homePos.size() >= 3) {
            entity.put(ChunkerEntityProperty.HOME_POSITION, new ChunkerBlockPosition(
                    (int) Math.floor(homePos.get(0)),
                    (int) Math.floor(homePos.get(1)),
                    (int) Math.floor(homePos.get(2))
            ));
            return;
        }
        List<Integer> homePosInts = input.getListValues("HomePos", IntTag.class, null);
        if (homePosInts != null && homePosInts.size() >= 3) {
            entity.put(ChunkerEntityProperty.HOME_POSITION, new ChunkerBlockPosition(homePosInts.get(0), homePosInts.get(1), homePosInts.get(2)));
        }
    }

    /**
     * Read the unslotted inventory (ChestItems) of a mob, e.g. a villager or piglin.
     *
     * @param resolvers the resolvers.
     * @param input     the actor NBT.
     * @param entity    the entity.
     */
    public static void readInventory(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ListTag<CompoundTag, Map<String, Tag<?>>> chestItems = input.getList("ChestItems", CompoundTag.class, null);
        if (chestItems == null) return;

        Map<Integer, ChunkerItemStack> inventory = new TreeMap<>();
        for (CompoundTag chestItem : chestItems) {
            ChunkerItemStack item = readItem(resolvers, chestItem);
            if (item == null) continue;
            inventory.put((int) chestItem.getByte("Slot", (byte) inventory.size()), item);
        }
        if (!inventory.isEmpty()) {
            entity.put(ChunkerEntityProperty.INVENTORY, inventory);
        }
    }
}

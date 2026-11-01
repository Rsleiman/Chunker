package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.identifier.BedrockBlockCompoundTag;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.item.ChunkerVanillaItemType;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.potion.ChunkerEffectType;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerBlockPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerEntityFlag;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerEquipmentSlot;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerMobEffect;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
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
     * Bedrock axolotl Variant (lucy, cyan, gold, wild, blue) to the Java variant ID (lucy, wild, gold, cyan, blue).
     */
    private static final int[] AXOLOTL_VARIANTS = {0, 3, 2, 1, 4};
    /**
     * Bedrock salmon size groups to the Java type.
     */
    private static final Map<String, String> SALMON_SIZES = Map.of(
            "scale_small", "small", "scale_normal", "medium", "scale_large", "large"
    );
    /**
     * Bedrock frog Variant to the Java variant.
     */
    private static final String[] FROG_VARIANTS = {"temperate", "cold", "warm"};
    /**
     * Bedrock zombie nautilus variant property to the Java variant.
     */
    private static final Map<String, String> ZOMBIE_NAUTILUS_VARIANTS = Map.of("default", "temperate", "coral", "warm");
    /**
     * Bedrock sound variants which Java names differently.
     */
    private static final Map<String, String> SOUND_VARIANTS = Map.of("default", "classic", "mad", "angry");
    /**
     * Mobs which keep a saddle (slot 0) and armor (slot 1) in a horse-style inventory.
     */
    private static final Set<ChunkerEntityType> NAUTILUSES = Set.of(
            ChunkerVanillaEntityType.NAUTILUS, ChunkerVanillaEntityType.ZOMBIE_NAUTILUS
    );
    /**
     * Mobs which can lead a patrol (Bedrock marks the captain with the illager_squad_captain or patrol_captain group).
     */
    private static final Set<ChunkerEntityType> PATROL_CAPTAINS = Set.of(
            ChunkerVanillaEntityType.PILLAGER, ChunkerVanillaEntityType.VINDICATOR
    );
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

        readAge(resolvers, input, entity, type);
        readTaming(resolvers, input, entity, type);
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

    /**
     * Get an entry of the Attributes list by name.
     *
     * @param input the actor NBT.
     * @param name  the name of the attribute, e.g. minecraft:health.
     * @return the attribute or null if not present.
     */
    @Nullable
    public static CompoundTag getAttribute(CompoundTag input, String name) {
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
        if (GenericEntityTypes.HORSES.contains(type) || NAUTILUSES.contains(type)) {
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

        // An enderman holds its block as an item, Java keeps it as a block state
        if (type == ChunkerVanillaEntityType.ENDERMAN) {
            readCarriedBlock(resolvers, input, entity);
            equipment.remove(ChunkerEquipmentSlot.MAINHAND);
            dropChances.remove(ChunkerEquipmentSlot.MAINHAND);
        }

        // A Bedrock captain wears the ominous banner in the chest slot, Java on the head, where it always drops
        if (PATROL_CAPTAINS.contains(type) && isPatrolCaptain(input, groups) && !equipment.containsKey(ChunkerEquipmentSlot.HEAD)) {
            ChunkerItemStack banner = equipment.remove(ChunkerEquipmentSlot.CHEST);
            if (banner != null) {
                equipment.put(ChunkerEquipmentSlot.HEAD, banner);
                dropChances.remove(ChunkerEquipmentSlot.CHEST);
                dropChances.put(ChunkerEquipmentSlot.HEAD, GUARANTEED_DROP_CHANCE);
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
        List<ChunkerMobEffect> effects = readEffectList(resolvers, input);
        if (!effects.isEmpty()) {
            entity.put(ChunkerEntityProperty.EFFECTS, effects);
        }
    }

    /**
     * Read the active effects of an actor or player (ActiveEffects), skipping effects which can't be converted.
     *
     * @param resolvers the resolvers.
     * @param input     the actor or player NBT.
     * @return the effects, empty if there are none.
     */
    public static List<ChunkerMobEffect> readEffectList(BedrockResolvers resolvers, CompoundTag input) {
        ListTag<CompoundTag, Map<String, Tag<?>>> activeEffects = input.getList("ActiveEffects", CompoundTag.class, null);
        if (activeEffects == null || activeEffects.size() == 0) return Collections.emptyList();

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
        return effects;
    }

    private static void readAge(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity, ChunkerEntityType type) {
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

            if (input.getByte("GrowthPaused", (byte) 0) == 1) {
                putFlag(entity, ChunkerEntityFlag.AGE_LOCKED, true);
            }

            int inLove = input.getInt("InLove", 0);
            if (inLove > 0) {
                entity.put(ChunkerEntityProperty.IN_LOVE, inLove);
                BedrockEntityUUIDs.referenceUUID(resolvers.converter(), input.getLong("LoveCause", -1))
                        .ifPresent(uuid -> entity.put(ChunkerEntityProperty.LOVE_CAUSE, uuid));
            }
        } else if (GenericEntityTypes.BABY_FLAG.contains(type)) {
            entity.put(ChunkerEntityProperty.BABY, baby);
        }
    }

    private static void readTaming(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity, ChunkerEntityType type) {
        if (!GenericEntityTypes.SITTING_PETS.contains(type) && !GenericEntityTypes.HORSES.contains(type) && !NAUTILUSES.contains(type)) return;

        boolean tamed = input.getByte("IsTamed", (byte) 0) == 1;
        entity.put(ChunkerEntityProperty.TAMED, tamed);
        if (tamed) {
            BedrockEntityUUIDs.referenceUUID(resolvers.converter(), input.getLong("OwnerNew", -1))
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
                .ifPresent(sound -> entity.put(ChunkerEntityProperty.SOUND_VARIANT, SOUND_VARIANTS.getOrDefault(sound, sound)));

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
            case COW, PIG -> properties.getOptionalValue("minecraft:climate_variant", String.class)
                    .ifPresent(climate -> entity.put(ChunkerEntityProperty.VARIANT, climate));
            case CHICKEN -> {
                String climate = properties.getString("minecraft:climate_variant", null);
                if (climate != null) {
                    entity.put(ChunkerEntityProperty.VARIANT, climate);
                }

                // One egg timer per climate (temperate, warm, cold), the one for the chicken's climate counts
                int index = climate == null ? 0 : switch (climate) {
                    case "warm" -> 1;
                    case "cold" -> 2;
                    default -> 0;
                };
                readSpawnTimer(input, entity, index);
            }
            case ARMADILLO -> readSpawnTimer(input, entity, 0);
            case AXOLOTL -> {
                int axolotl = getGroupVariant(groups, "axolotl_", List.of("lucy", "cyan", "gold", "wild", "blue"), variant);
                if (axolotl >= 0 && axolotl < AXOLOTL_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT_ID, AXOLOTL_VARIANTS[axolotl]);
                }
            }
            case PARROT -> entity.put(ChunkerEntityProperty.VARIANT_ID,
                    getGroupVariant(groups, "parrot_", List.of("red", "blue", "green", "cyan", "silver"), variant));
            case RABBIT -> entity.put(ChunkerEntityProperty.VARIANT_ID,
                    getGroupVariant(groups, "coat_", List.of("brown", "white", "black", "splotched", "desert", "salt"), variant));
            case FROG -> {
                int frog = getGroupVariant(groups, "", List.of("temperate_frog", "cold_frog", "warm_frog"), variant);
                if (frog >= 0 && frog < FROG_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT, FROG_VARIANTS[frog]);
                }
            }
            case ZOMBIE_NAUTILUS -> properties.getOptionalValue("minecraft:variant", String.class)
                    .map(ZOMBIE_NAUTILUS_VARIANTS::get)
                    .ifPresent(value -> entity.put(ChunkerEntityProperty.VARIANT, value));
            case SALMON -> {
                for (String group : groups) {
                    String size = SALMON_SIZES.get(group);
                    if (size != null) {
                        entity.put(ChunkerEntityProperty.VARIANT, size);
                    }
                }
            }
            case SHULKER -> {
                // Bedrock counts the colors from black, 16 is the undyed shulker (Java's default)
                if (variant >= 0 && variant < 16) {
                    ChunkerDyeColor.getColorByReversedID(variant)
                            .ifPresent(color -> entity.put(ChunkerEntityProperty.COLOR, color));
                }
            }
            case HOGLIN -> putFlag(entity, ChunkerEntityFlag.CANNOT_BE_HUNTED, groups.contains("unhuntable_adult"));
            case PILLAGER, VINDICATOR -> putFlag(entity, ChunkerEntityFlag.PATROL_LEADER, isPatrolCaptain(input, groups));
            case CREEPER -> putFlag(entity, ChunkerEntityFlag.POWERED, groups.contains("charged_creeper"));
            case BOGGED -> putFlag(entity, ChunkerEntityFlag.BOGGED_SHEARED,
                    groups.contains("bogged_sheared") || input.getByte("Sheared", (byte) 0) == 1);
            case SNOW_GOLEM -> putFlag(entity, ChunkerEntityFlag.PUMPKIN,
                    !groups.contains("snowman_sheared") && input.getByte("Sheared", (byte) 0) != 1);
            case OCELOT -> putFlag(entity, ChunkerEntityFlag.TRUSTING,
                    groups.contains("ocelot_trusting") || input.getByte("IsTrusting", (byte) 0) == 1);
            case SKELETON_HORSE -> putFlag(entity, ChunkerEntityFlag.SKELETON_TRAP, groups.contains("skeleton_trap"));
            case GOAT -> {
                putFlag(entity, ChunkerEntityFlag.SCREAMING_GOAT, groups.contains("goat_screamer") || variant == 1);

                // Bedrock counts the horns left, a goat which lost one keeps its left horn (which one isn't saved)
                int horns = input.getOptionalValue("GoatHornCount", Number.class).map(Number::intValue).orElse(2);
                putFlag(entity, ChunkerEntityFlag.LEFT_HORN, horns >= 1);
                putFlag(entity, ChunkerEntityFlag.RIGHT_HORN, horns >= 2);
            }
            case CAMEL, CAMEL_HUSK -> {
                if (groups.contains("camel_sitting") || groups.contains("camel_baby_sitting")) {
                    entity.put(ChunkerEntityProperty.SITTING, true);
                }
            }
            case MOOSHROOM -> {
                if (variant >= 0 && variant < MOOSHROOM_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT, MOOSHROOM_VARIANTS[variant]);
                }
            }
            case FOX -> {
                if (variant >= 0 && variant < FOX_VARIANTS.length) {
                    entity.put(ChunkerEntityProperty.VARIANT, FOX_VARIANTS[variant]);
                }
                entity.put(ChunkerEntityProperty.SITTING, input.getByte("Sitting", (byte) 0) == 1);
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

                // A bee which has stung counts down to its death (mark variant 1)
                putFlag(entity, ChunkerEntityFlag.HAS_STUNG, groups.contains("countdown_to_perish") || input.getInt("MarkVariant", 0) == 1);
            }
            case TURTLE -> {
                readHomePosition(input, entity);
                putFlag(entity, ChunkerEntityFlag.HAS_EGG, groups.contains("pregnant") || input.getByte("IsPregnant", (byte) 0) == 1);
            }
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

        // Villagers and wandering traders
        if (type == ChunkerVanillaEntityType.VILLAGER || type == ChunkerVanillaEntityType.ZOMBIE_VILLAGER
                || type == ChunkerVanillaEntityType.WANDERING_TRADER) {
            BedrockVillagerReader.read(resolvers, input, entity, groups);
        }

        // A piglin brute guards the bastion it spawned in
        if (type == ChunkerVanillaEntityType.PIGLIN_BRUTE) {
            readBruteHome(input, entity);
        }

        // Bedrock doesn't save a despawn timer which could be decoded, so it restarts (as for a new trader)
        if (type == ChunkerVanillaEntityType.WANDERING_TRADER || type == ChunkerVanillaEntityType.TRADER_LLAMA) {
            entity.put(ChunkerEntityProperty.DESPAWN_DELAY, 48000);
        }
    }

    private static void readCollar(CompoundTag input, GenericEntity entity) {
        if (input.getByte("IsTamed", (byte) 0) != 1) return;
        // Bedrock leaves out a Color of 0 (white)
        ChunkerDyeColor.getColorByID(input.getByte("Color", (byte) 0))
                .ifPresent(color -> entity.put(ChunkerEntityProperty.COLLAR_COLOR, color));
    }

    /**
     * Get a variant from the component group which sets it (e.g. axolotl_wild), as Bedrock leaves out a Variant of 0.
     *
     * @param groups   the active component groups.
     * @param prefix   the prefix of the groups.
     * @param names    the group names after the prefix, in variant order.
     * @param fallback the Variant to use if no group is active.
     * @return the variant.
     */
    private static int getGroupVariant(Set<String> groups, String prefix, List<String> names, int fallback) {
        for (int i = 0; i < names.size(); i++) {
            if (groups.contains(prefix + names.get(i))) return i;
        }
        return fallback;
    }

    private static boolean isPatrolCaptain(CompoundTag input, Set<String> groups) {
        return input.getByte("IsIllagerCaptain", (byte) 0) == 1 || groups.contains("illager_squad_captain")
                || groups.contains("patrol_captain");
    }

    private static void putFlag(GenericEntity entity, ChunkerEntityFlag flag, boolean value) {
        Map<ChunkerEntityFlag, Boolean> flags = entity.get(ChunkerEntityProperty.FLAGS);
        if (flags == null) {
            flags = new EnumMap<>(ChunkerEntityFlag.class);
            entity.put(ChunkerEntityProperty.FLAGS, flags);
        }
        flags.put(flag, value);
    }

    private static void readSpawnTimer(CompoundTag input, GenericEntity entity, int index) {
        // The ticks until a spawn_entity component drops its item (egg or scute), one entry per component entry
        ListTag<CompoundTag, Map<String, Tag<?>>> entries = input.getList("entries", CompoundTag.class, null);
        if (entries == null || entries.size() == 0) return;
        CompoundTag entry = entries.get(index < entries.size() ? index : 0);
        if (entry.getByte("StopSpawning", (byte) 0) == 1) return;
        entry.getOptionalValue("SpawnTimer", Integer.class)
                .ifPresent(timer -> entity.put(ChunkerEntityProperty.ITEM_DROP_TIME, Math.max(0, timer)));
    }

    private static void readCarriedBlock(BedrockResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ListTag<CompoundTag, Map<String, Tag<?>>> hand = input.getList("Mainhand", CompoundTag.class, null);
        if (hand == null || hand.size() == 0) return;
        CompoundTag block = hand.get(0).getCompound("Block");
        if (block == null || hand.get(0).getByte("Count", (byte) 0) <= 0) return;
        ChunkerBlockIdentifier identifier = resolvers.readBlock(new BedrockBlockCompoundTag(block, false));
        if (!identifier.isAir()) {
            entity.put(ChunkerEntityProperty.BLOCK_STATE, identifier);
        }
    }

    private static void readBruteHome(CompoundTag input, GenericEntity entity) {
        List<Float> homePos = input.getListValues("HomePos", FloatTag.class, null);
        if (homePos == null || homePos.size() < 3) return;
        Dimension dimension = switch (input.getInt("HomeDimensionId", 1)) {
            case 0 -> Dimension.OVERWORLD;
            case 2 -> Dimension.THE_END;
            default -> Dimension.NETHER;
        };
        Map<ChunkerVillagerMemory, ChunkerGlobalPosition> memories = new EnumMap<>(ChunkerVillagerMemory.class);
        memories.put(ChunkerVillagerMemory.HOME, new ChunkerGlobalPosition(dimension,
                (int) Math.floor(homePos.get(0)), (int) Math.floor(homePos.get(1)), (int) Math.floor(homePos.get(2))));
        entity.put(ChunkerEntityProperty.VILLAGER_MEMORIES, memories);
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

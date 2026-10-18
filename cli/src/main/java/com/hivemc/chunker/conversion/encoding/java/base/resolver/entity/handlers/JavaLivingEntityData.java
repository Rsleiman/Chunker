package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
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
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Writes and reads the data of Java living entities (1.21.5 onwards) and the type specific data.
 */
public final class JavaLivingEntityData {
    private JavaLivingEntityData() {
    }

    /**
     * Write the living entity data and the type specific data.
     *
     * @param resolvers the resolvers.
     * @param output    the entity NBT.
     * @param entity    the entity.
     */
    public static void write(JavaResolvers resolvers, CompoundTag output, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();

        // Health and attributes
        Float health = entity.get(ChunkerEntityProperty.HEALTH);
        if (health != null) {
            output.put("Health", health.floatValue());
        }
        ListTag<CompoundTag, Map<String, Tag<?>>> attributes = new ListTag<>(TagType.COMPOUND);
        writeAttribute(attributes, "minecraft:max_health", entity.get(ChunkerEntityProperty.MAX_HEALTH));
        writeAttribute(attributes, "minecraft:movement_speed", entity.get(ChunkerEntityProperty.MOVEMENT_SPEED));
        writeAttribute(attributes, "minecraft:jump_strength", entity.get(ChunkerEntityProperty.JUMP_STRENGTH));
        if (attributes.size() > 0) {
            output.put("attributes", attributes);
        }

        // Equipment
        Map<ChunkerEquipmentSlot, ChunkerItemStack> equipment = entity.get(ChunkerEntityProperty.EQUIPMENT);
        if (equipment != null) {
            CompoundTag equipmentTag = new CompoundTag();
            for (Map.Entry<ChunkerEquipmentSlot, ChunkerItemStack> entry : equipment.entrySet()) {
                writeItem(resolvers, entry.getValue()).ifPresent(item -> equipmentTag.put(entry.getKey().getName(), item));
            }
            if (equipmentTag.size() > 0) {
                output.put("equipment", equipmentTag);
            }
        }
        Map<ChunkerEquipmentSlot, Float> dropChances = entity.get(ChunkerEntityProperty.DROP_CHANCES);
        if (dropChances != null) {
            CompoundTag dropChancesTag = new CompoundTag();
            for (Map.Entry<ChunkerEquipmentSlot, Float> entry : dropChances.entrySet()) {
                dropChancesTag.put(entry.getKey().getName(), entry.getValue().floatValue());
            }
            output.put("drop_chances", dropChancesTag);
        }

        // Effects
        List<ChunkerMobEffect> effects = entity.get(ChunkerEntityProperty.EFFECTS);
        if (effects != null) {
            ListTag<CompoundTag, Map<String, Tag<?>>> effectsTag = new ListTag<>(TagType.COMPOUND, effects.size());
            for (ChunkerMobEffect effect : effects) {
                CompoundTag effectTag = new CompoundTag();
                effectTag.put("id", resolvers.writeEffect(effect.type()));
                effectTag.put("amplifier", (byte) effect.amplifier());
                effectTag.put("duration", effect.duration());
                effectTag.put("ambient", effect.ambient() ? (byte) 1 : (byte) 0);
                effectTag.put("show_particles", effect.showParticles() ? (byte) 1 : (byte) 0);
                effectTag.put("show_icon", effect.showParticles() ? (byte) 1 : (byte) 0);
                effectsTag.add(effectTag);
            }
            output.put("active_effects", effectsTag);
        }

        writeBoolean(output, "PersistenceRequired", entity.get(ChunkerEntityProperty.PERSISTENT));
        writeBoolean(output, "CanPickUpLoot", entity.get(ChunkerEntityProperty.CAN_PICK_UP_LOOT));

        // Age
        if (GenericEntityTypes.AGEABLE.contains(type)) {
            Integer age = entity.get(ChunkerEntityProperty.AGE);
            if (age != null) {
                output.put("Age", age.intValue());
            }
            Integer inLove = entity.get(ChunkerEntityProperty.IN_LOVE);
            if (inLove != null) {
                output.put("InLove", inLove.intValue());
            }
            UUID loveCause = entity.get(ChunkerEntityProperty.LOVE_CAUSE);
            if (loveCause != null) {
                output.put("LoveCause", JavaEntityNBT.toIntArray(loveCause));
            }
        } else if (GenericEntityTypes.BABY_FLAG.contains(type)) {
            writeBoolean(output, "IsBaby", entity.get(ChunkerEntityProperty.BABY));
        }

        // Taming
        UUID owner = entity.get(ChunkerEntityProperty.OWNER);
        if (owner != null) {
            output.put("Owner", JavaEntityNBT.toIntArray(owner));
        }
        if (GenericEntityTypes.HORSES.contains(type)) {
            writeBoolean(output, "Tame", entity.get(ChunkerEntityProperty.TAMED));
        }
        if (GenericEntityTypes.SITTING_PETS.contains(type)) {
            writeBoolean(output, "Sitting", entity.get(ChunkerEntityProperty.SITTING));
        }

        writeTypeSpecific(resolvers, output, entity, type);
    }

    private static void writeTypeSpecific(JavaResolvers resolvers, CompoundTag output, GenericEntity entity, ChunkerEntityType type) {
        String variant = entity.get(ChunkerEntityProperty.VARIANT);
        if (variant != null) {
            if (type == ChunkerVanillaEntityType.FOX || type == ChunkerVanillaEntityType.MOOSHROOM) {
                output.put("Type", variant);
            } else {
                output.put("variant", "minecraft:" + variant);
            }
        }
        String soundVariant = entity.get(ChunkerEntityProperty.SOUND_VARIANT);
        if (soundVariant != null) {
            output.put("sound_variant", "minecraft:" + soundVariant);
        }
        Integer variantId = entity.get(ChunkerEntityProperty.VARIANT_ID);
        if (variantId != null) {
            output.put("Variant", variantId.intValue());
        }
        ChunkerDyeColor color = entity.get(ChunkerEntityProperty.COLOR);
        if (color != null) {
            output.put("Color", (byte) color.getID());
        }
        ChunkerDyeColor collarColor = entity.get(ChunkerEntityProperty.COLLAR_COLOR);
        if (collarColor != null) {
            output.put("CollarColor", (byte) collarColor.getID());
        }
        writeBoolean(output, "Sheared", entity.get(ChunkerEntityProperty.SHEARED));

        // Horses
        Integer temper = entity.get(ChunkerEntityProperty.TEMPER);
        if (temper != null) {
            output.put("Temper", temper.intValue());
        }
        writeBoolean(output, "ChestedHorse", entity.get(ChunkerEntityProperty.CHESTED));
        Integer strength = entity.get(ChunkerEntityProperty.STRENGTH);
        if (strength != null) {
            output.put("Strength", strength.intValue());
        }

        // Inventories: chested horses use slots, other mobs a plain list
        Map<Integer, ChunkerItemStack> inventory = entity.get(ChunkerEntityProperty.INVENTORY);
        if (inventory != null) {
            boolean slotted = GenericEntityTypes.CHESTED_HORSES.contains(type);
            ListTag<CompoundTag, Map<String, Tag<?>>> items = new ListTag<>(TagType.COMPOUND, inventory.size());
            for (Map.Entry<Integer, ChunkerItemStack> entry : inventory.entrySet()) {
                writeItem(resolvers, entry.getValue()).ifPresent(item -> {
                    if (slotted) {
                        item.put("Slot", entry.getKey().byteValue());
                    }
                    items.add(item);
                });
            }
            output.put(slotted ? "Items" : "Inventory", items);
        }

        // Homes
        ChunkerBlockPosition homePosition = entity.get(ChunkerEntityProperty.HOME_POSITION);
        if (homePosition != null) {
            output.put(type == ChunkerVanillaEntityType.BEE ? "hive_pos" : "home_pos", new int[]{homePosition.x(), homePosition.y(), homePosition.z()});
        }

        writeBoolean(output, "HasNectar", entity.get(ChunkerEntityProperty.HAS_NECTAR));
        writeBoolean(output, "PlayerCreated", entity.get(ChunkerEntityProperty.PLAYER_CREATED));
        writeBoolean(output, "CanBreakDoors", entity.get(ChunkerEntityProperty.CAN_BREAK_DOORS));
        writeBoolean(output, "CannotHunt", entity.get(ChunkerEntityProperty.CANNOT_HUNT));
        Long duplicationCooldown = entity.get(ChunkerEntityProperty.DUPLICATION_COOLDOWN);
        if (duplicationCooldown != null) {
            output.put("DuplicationCooldown", duplicationCooldown.longValue());
        }
        Integer despawnDelay = entity.get(ChunkerEntityProperty.DESPAWN_DELAY);
        if (despawnDelay != null) {
            output.put("DespawnDelay", despawnDelay.intValue());
        }
        Integer size = entity.get(ChunkerEntityProperty.SIZE);
        if (size != null) {
            output.put("Size", size - 1); // Java stores the size minus one
        }

        if (isVillager(type)) {
            JavaVillagerData.write(resolvers, output, entity);
        }
    }

    private static boolean isVillager(ChunkerEntityType type) {
        return type == ChunkerVanillaEntityType.VILLAGER || type == ChunkerVanillaEntityType.ZOMBIE_VILLAGER
                || type == ChunkerVanillaEntityType.WANDERING_TRADER;
    }

    /**
     * Read the living entity data and the type specific data.
     *
     * @param resolvers the resolvers.
     * @param input     the entity NBT.
     * @param entity    the entity to fill.
     */
    public static void read(JavaResolvers resolvers, CompoundTag input, GenericEntity entity) {
        ChunkerEntityType type = entity.getEntityType();

        // Health and attributes
        input.getOptionalValue("Health", Float.class).ifPresent(value -> entity.put(ChunkerEntityProperty.HEALTH, value));
        ListTag<CompoundTag, Map<String, Tag<?>>> attributes = input.getList("attributes", CompoundTag.class, null);
        if (attributes != null) {
            for (CompoundTag attribute : attributes) {
                String id = attribute.getString("id", "");
                double base = attribute.getDouble("base", 0);
                switch (id) {
                    case "minecraft:max_health" -> entity.put(ChunkerEntityProperty.MAX_HEALTH, base);
                    case "minecraft:movement_speed" -> entity.put(ChunkerEntityProperty.MOVEMENT_SPEED, base);
                    case "minecraft:jump_strength" -> entity.put(ChunkerEntityProperty.JUMP_STRENGTH, base);
                    default -> {
                    }
                }
            }
        }

        // Equipment
        CompoundTag equipmentTag = input.getCompound("equipment");
        if (equipmentTag != null) {
            Map<ChunkerEquipmentSlot, ChunkerItemStack> equipment = new EnumMap<>(ChunkerEquipmentSlot.class);
            for (ChunkerEquipmentSlot slot : ChunkerEquipmentSlot.values()) {
                CompoundTag item = equipmentTag.getCompound(slot.getName());
                if (item != null) {
                    equipment.put(slot, resolvers.readItem(item));
                }
            }
            if (!equipment.isEmpty()) {
                entity.put(ChunkerEntityProperty.EQUIPMENT, equipment);
            }
        }
        CompoundTag dropChancesTag = input.getCompound("drop_chances");
        if (dropChancesTag != null) {
            Map<ChunkerEquipmentSlot, Float> dropChances = new EnumMap<>(ChunkerEquipmentSlot.class);
            for (ChunkerEquipmentSlot slot : ChunkerEquipmentSlot.values()) {
                dropChancesTag.getOptionalValue(slot.getName(), Float.class).ifPresent(value -> dropChances.put(slot, value));
            }
            entity.put(ChunkerEntityProperty.DROP_CHANCES, dropChances);
        }

        // Effects
        ListTag<CompoundTag, Map<String, Tag<?>>> effectsTag = input.getList("active_effects", CompoundTag.class, null);
        if (effectsTag != null) {
            List<ChunkerMobEffect> effects = new ArrayList<>(effectsTag.size());
            for (CompoundTag effectTag : effectsTag) {
                ChunkerEffectType effectType = resolvers.readEffect(effectTag.getString("id", ""));
                if (effectType == null || effectType == ChunkerEffectType.EMPTY) continue;
                effects.add(new ChunkerMobEffect(
                        effectType,
                        effectTag.getByte("amplifier", (byte) 0),
                        effectTag.getInt("duration", 0),
                        effectTag.getByte("ambient", (byte) 0) == 1,
                        effectTag.getByte("show_particles", (byte) 1) == 1
                ));
            }
            entity.put(ChunkerEntityProperty.EFFECTS, effects);
        }

        readBoolean(input, "PersistenceRequired", entity, ChunkerEntityProperty.PERSISTENT);
        readBoolean(input, "CanPickUpLoot", entity, ChunkerEntityProperty.CAN_PICK_UP_LOOT);

        // Age
        if (GenericEntityTypes.AGEABLE.contains(type)) {
            int age = input.getInt("Age", 0);
            entity.put(ChunkerEntityProperty.BABY, age < 0);
            if (input.contains("Age") && age != 0) {
                entity.put(ChunkerEntityProperty.AGE, age);
            }
            input.getOptionalValue("InLove", Integer.class).filter(value -> value > 0)
                    .ifPresent(value -> entity.put(ChunkerEntityProperty.IN_LOVE, value));
            UUID loveCause = JavaEntityNBT.fromIntArray(input.getIntArray("LoveCause", null));
            if (loveCause != null) {
                entity.put(ChunkerEntityProperty.LOVE_CAUSE, loveCause);
            }
        } else if (GenericEntityTypes.BABY_FLAG.contains(type)) {
            readBoolean(input, "IsBaby", entity, ChunkerEntityProperty.BABY);
        }

        // Taming
        UUID owner = JavaEntityNBT.fromIntArray(input.getIntArray("Owner", null));
        if (owner != null) {
            entity.put(ChunkerEntityProperty.OWNER, owner);
        }
        if (GenericEntityTypes.HORSES.contains(type)) {
            readBoolean(input, "Tame", entity, ChunkerEntityProperty.TAMED);
        } else if (GenericEntityTypes.SITTING_PETS.contains(type)) {
            entity.put(ChunkerEntityProperty.TAMED, owner != null);
            readBoolean(input, "Sitting", entity, ChunkerEntityProperty.SITTING);
        }

        readTypeSpecific(resolvers, input, entity, type);
    }

    private static void readTypeSpecific(JavaResolvers resolvers, CompoundTag input, GenericEntity entity, ChunkerEntityType type) {
        if (type == ChunkerVanillaEntityType.FOX || type == ChunkerVanillaEntityType.MOOSHROOM) {
            input.getOptionalValue("Type", String.class).ifPresent(value -> entity.put(ChunkerEntityProperty.VARIANT, value));
        } else {
            input.getOptionalValue("variant", String.class)
                    .ifPresent(value -> entity.put(ChunkerEntityProperty.VARIANT, stripNamespace(value)));
        }
        input.getOptionalValue("sound_variant", String.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.SOUND_VARIANT, stripNamespace(value)));
        input.getOptionalValue("Variant", Integer.class).ifPresent(value -> entity.put(ChunkerEntityProperty.VARIANT_ID, value));
        input.getOptionalValue("Color", Byte.class).flatMap(value -> ChunkerDyeColor.getColorByID(value))
                .ifPresent(value -> entity.put(ChunkerEntityProperty.COLOR, value));
        input.getOptionalValue("CollarColor", Byte.class).flatMap(value -> ChunkerDyeColor.getColorByID(value))
                .ifPresent(value -> entity.put(ChunkerEntityProperty.COLLAR_COLOR, value));
        readBoolean(input, "Sheared", entity, ChunkerEntityProperty.SHEARED);

        // Horses
        input.getOptionalValue("Temper", Integer.class).ifPresent(value -> entity.put(ChunkerEntityProperty.TEMPER, value));
        readBoolean(input, "ChestedHorse", entity, ChunkerEntityProperty.CHESTED);
        input.getOptionalValue("Strength", Integer.class).ifPresent(value -> entity.put(ChunkerEntityProperty.STRENGTH, value));

        // Inventories
        boolean slotted = GenericEntityTypes.CHESTED_HORSES.contains(type);
        ListTag<CompoundTag, Map<String, Tag<?>>> items = input.getList(slotted ? "Items" : "Inventory", CompoundTag.class, null);
        if (items != null) {
            Map<Integer, ChunkerItemStack> inventory = new TreeMap<>();
            for (CompoundTag item : items) {
                int slot = slotted ? item.getByte("Slot", (byte) inventory.size()) : inventory.size();
                inventory.put(slot, resolvers.readItem(item));
            }
            entity.put(ChunkerEntityProperty.INVENTORY, inventory);
        }

        // Homes
        int[] homePosition = input.getIntArray(type == ChunkerVanillaEntityType.BEE ? "hive_pos" : "home_pos", null);
        if (homePosition != null && homePosition.length == 3) {
            entity.put(ChunkerEntityProperty.HOME_POSITION, new ChunkerBlockPosition(homePosition[0], homePosition[1], homePosition[2]));
        }

        readBoolean(input, "HasNectar", entity, ChunkerEntityProperty.HAS_NECTAR);
        readBoolean(input, "PlayerCreated", entity, ChunkerEntityProperty.PLAYER_CREATED);
        readBoolean(input, "CanBreakDoors", entity, ChunkerEntityProperty.CAN_BREAK_DOORS);
        readBoolean(input, "CannotHunt", entity, ChunkerEntityProperty.CANNOT_HUNT);
        input.getOptionalValue("DuplicationCooldown", Long.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.DUPLICATION_COOLDOWN, value));
        input.getOptionalValue("DespawnDelay", Integer.class)
                .ifPresent(value -> entity.put(ChunkerEntityProperty.DESPAWN_DELAY, value));
        input.getOptionalValue("Size", Integer.class).ifPresent(value -> entity.put(ChunkerEntityProperty.SIZE, value + 1));

        if (isVillager(type)) {
            JavaVillagerData.read(resolvers, input, entity);
        }
    }

    /**
     * Write an item stack, skipping air.
     *
     * @param resolvers the resolvers.
     * @param item      the item.
     * @return the item NBT if it could be written.
     */
    public static Optional<CompoundTag> writeItem(JavaResolvers resolvers, @Nullable ChunkerItemStack item) {
        if (item == null || item.getIdentifier().isAir()) return Optional.empty();
        return resolvers.writeItem(item);
    }

    private static void writeAttribute(ListTag<CompoundTag, Map<String, Tag<?>>> attributes, String id, @Nullable Double base) {
        if (base == null) return;
        CompoundTag attribute = new CompoundTag();
        attribute.put("id", id);
        attribute.put("base", base.doubleValue());
        attributes.add(attribute);
    }

    private static void writeBoolean(CompoundTag output, String key, @Nullable Boolean value) {
        if (value != null) {
            output.put(key, value ? (byte) 1 : (byte) 0);
        }
    }

    private static void readBoolean(CompoundTag input, String key, GenericEntity entity, ChunkerEntityProperty<Boolean> property) {
        input.getOptionalValue(key, Byte.class).ifPresent(value -> entity.put(property, value == 1));
    }

    private static String stripNamespace(String identifier) {
        return identifier.startsWith("minecraft:") ? identifier.substring("minecraft:".length()) : identifier;
    }
}

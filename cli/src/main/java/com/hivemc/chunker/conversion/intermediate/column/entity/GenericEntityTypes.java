package com.hivemc.chunker.conversion.intermediate.column.entity;

import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The entity types which are converted as a {@link GenericEntity}.
 */
public final class GenericEntityTypes {
    /**
     * Living entities which aren't converted: the ender dragon belongs to the dragon fight state of the level.
     */
    private static final Set<ChunkerVanillaEntityType> EXCLUDED_LIVING = EnumSet.of(
            ChunkerVanillaEntityType.ENDER_DRAGON
    );

    /**
     * Every living entity (mobs and armor stands).
     */
    public static final Set<ChunkerEntityType> LIVING = Collections.unmodifiableSet(Arrays.stream(ChunkerVanillaEntityType.values())
            .filter(type -> type.isVanillaSpawnEgg() || type == ChunkerVanillaEntityType.ARMOR_STAND
                    || type == ChunkerVanillaEntityType.GIANT || type == ChunkerVanillaEntityType.ILLUSIONER)
            .filter(type -> !EXCLUDED_LIVING.contains(type))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ChunkerVanillaEntityType.class))));

    /**
     * Entities which grow up and store their age (AgeableMob in Java 26.2).
     */
    public static final Set<ChunkerEntityType> AGEABLE = Collections.unmodifiableSet(EnumSet.of(
            ChunkerVanillaEntityType.ARMADILLO,
            ChunkerVanillaEntityType.AXOLOTL,
            ChunkerVanillaEntityType.BEE,
            ChunkerVanillaEntityType.CAMEL,
            ChunkerVanillaEntityType.CAMEL_HUSK,
            ChunkerVanillaEntityType.CAT,
            ChunkerVanillaEntityType.CHICKEN,
            ChunkerVanillaEntityType.COW,
            ChunkerVanillaEntityType.DOLPHIN,
            ChunkerVanillaEntityType.DONKEY,
            ChunkerVanillaEntityType.FOX,
            ChunkerVanillaEntityType.FROG,
            ChunkerVanillaEntityType.GLOW_SQUID,
            ChunkerVanillaEntityType.GOAT,
            ChunkerVanillaEntityType.HAPPY_GHAST,
            ChunkerVanillaEntityType.HOGLIN,
            ChunkerVanillaEntityType.HORSE,
            ChunkerVanillaEntityType.LLAMA,
            ChunkerVanillaEntityType.MAGMA_CUBE,
            ChunkerVanillaEntityType.MOOSHROOM,
            ChunkerVanillaEntityType.MULE,
            ChunkerVanillaEntityType.NAUTILUS,
            ChunkerVanillaEntityType.OCELOT,
            ChunkerVanillaEntityType.PANDA,
            ChunkerVanillaEntityType.PARROT,
            ChunkerVanillaEntityType.PIG,
            ChunkerVanillaEntityType.POLAR_BEAR,
            ChunkerVanillaEntityType.RABBIT,
            ChunkerVanillaEntityType.SHEEP,
            ChunkerVanillaEntityType.SKELETON_HORSE,
            ChunkerVanillaEntityType.SLIME,
            ChunkerVanillaEntityType.SNIFFER,
            ChunkerVanillaEntityType.SQUID,
            ChunkerVanillaEntityType.STRIDER,
            ChunkerVanillaEntityType.SULFUR_CUBE,
            ChunkerVanillaEntityType.TRADER_LLAMA,
            ChunkerVanillaEntityType.TURTLE,
            ChunkerVanillaEntityType.VILLAGER,
            ChunkerVanillaEntityType.WANDERING_TRADER,
            ChunkerVanillaEntityType.WOLF,
            ChunkerVanillaEntityType.ZOMBIE_HORSE,
            ChunkerVanillaEntityType.ZOMBIE_NAUTILUS
    ));

    /**
     * Entities which can be babies but don't age (Java stores IsBaby).
     */
    public static final Set<ChunkerEntityType> BABY_FLAG = Collections.unmodifiableSet(EnumSet.of(
            ChunkerVanillaEntityType.ZOMBIE,
            ChunkerVanillaEntityType.HUSK,
            ChunkerVanillaEntityType.DROWNED,
            ChunkerVanillaEntityType.ZOMBIE_VILLAGER,
            ChunkerVanillaEntityType.ZOMBIFIED_PIGLIN,
            ChunkerVanillaEntityType.PIGLIN,
            ChunkerVanillaEntityType.ZOGLIN
    ));

    /**
     * Horses and their relatives (tamed with temper, saddle and armor in the inventory).
     */
    public static final Set<ChunkerEntityType> HORSES = Collections.unmodifiableSet(EnumSet.of(
            ChunkerVanillaEntityType.HORSE,
            ChunkerVanillaEntityType.DONKEY,
            ChunkerVanillaEntityType.MULE,
            ChunkerVanillaEntityType.SKELETON_HORSE,
            ChunkerVanillaEntityType.ZOMBIE_HORSE,
            ChunkerVanillaEntityType.LLAMA,
            ChunkerVanillaEntityType.TRADER_LLAMA,
            ChunkerVanillaEntityType.CAMEL,
            ChunkerVanillaEntityType.CAMEL_HUSK
    ));

    /**
     * Horses which can carry a chest.
     */
    public static final Set<ChunkerEntityType> CHESTED_HORSES = Collections.unmodifiableSet(EnumSet.of(
            ChunkerVanillaEntityType.DONKEY,
            ChunkerVanillaEntityType.MULE,
            ChunkerVanillaEntityType.LLAMA,
            ChunkerVanillaEntityType.TRADER_LLAMA
    ));

    /**
     * Entities tamed by a player which can sit (Java TamableAnimal).
     */
    public static final Set<ChunkerEntityType> SITTING_PETS = Collections.unmodifiableSet(EnumSet.of(
            ChunkerVanillaEntityType.WOLF,
            ChunkerVanillaEntityType.CAT,
            ChunkerVanillaEntityType.PARROT
    ));

    /**
     * Boats and rafts, in the order of the Bedrock boat Variant (0 oak to 10 poplar).
     */
    public static final List<ChunkerVanillaEntityType> BOATS = List.of(
            ChunkerVanillaEntityType.OAK_BOAT,
            ChunkerVanillaEntityType.SPRUCE_BOAT,
            ChunkerVanillaEntityType.BIRCH_BOAT,
            ChunkerVanillaEntityType.JUNGLE_BOAT,
            ChunkerVanillaEntityType.ACACIA_BOAT,
            ChunkerVanillaEntityType.DARK_OAK_BOAT,
            ChunkerVanillaEntityType.MANGROVE_BOAT,
            ChunkerVanillaEntityType.BAMBOO_RAFT,
            ChunkerVanillaEntityType.CHERRY_BOAT,
            ChunkerVanillaEntityType.PALE_OAK_BOAT,
            ChunkerVanillaEntityType.POPLAR_BOAT
    );

    /**
     * Chest boats and rafts, in the order of the Bedrock boat Variant (0 oak to 10 poplar).
     */
    public static final List<ChunkerVanillaEntityType> CHEST_BOATS = List.of(
            ChunkerVanillaEntityType.OAK_CHEST_BOAT,
            ChunkerVanillaEntityType.SPRUCE_CHEST_BOAT,
            ChunkerVanillaEntityType.BIRCH_CHEST_BOAT,
            ChunkerVanillaEntityType.JUNGLE_CHEST_BOAT,
            ChunkerVanillaEntityType.ACACIA_CHEST_BOAT,
            ChunkerVanillaEntityType.DARK_OAK_CHEST_BOAT,
            ChunkerVanillaEntityType.MANGROVE_CHEST_BOAT,
            ChunkerVanillaEntityType.BAMBOO_CHEST_RAFT,
            ChunkerVanillaEntityType.CHERRY_CHEST_BOAT,
            ChunkerVanillaEntityType.PALE_OAK_CHEST_BOAT,
            ChunkerVanillaEntityType.POPLAR_CHEST_BOAT
    );

    /**
     * Minecarts and boats with a container (slotted Items, or a loot table).
     */
    public static final Set<ChunkerEntityType> CONTAINERS = Collections.unmodifiableSet(Stream.concat(
            Stream.of(ChunkerVanillaEntityType.CHEST_MINECART, ChunkerVanillaEntityType.HOPPER_MINECART),
            CHEST_BOATS.stream()
    ).collect(Collectors.toCollection(() -> EnumSet.noneOf(ChunkerVanillaEntityType.class))));

    /**
     * Entities which aren't living: dropped items, experience orbs, arrows, falling blocks, minecarts and boats.
     */
    public static final Set<ChunkerEntityType> NON_LIVING = Collections.unmodifiableSet(Stream.concat(
            Stream.of(
                    ChunkerVanillaEntityType.ITEM,
                    ChunkerVanillaEntityType.EXPERIENCE_ORB,
                    ChunkerVanillaEntityType.ARROW,
                    ChunkerVanillaEntityType.FALLING_BLOCK,
                    ChunkerVanillaEntityType.MINECART,
                    ChunkerVanillaEntityType.CHEST_MINECART,
                    ChunkerVanillaEntityType.HOPPER_MINECART,
                    ChunkerVanillaEntityType.TNT_MINECART
            ),
            Stream.concat(BOATS.stream(), CHEST_BOATS.stream())
    ).collect(Collectors.toCollection(() -> EnumSet.noneOf(ChunkerVanillaEntityType.class))));

    /**
     * Every entity type which is converted as a generic entity.
     */
    public static final Set<ChunkerEntityType> ALL = Collections.unmodifiableSet(Stream.concat(LIVING.stream(), NON_LIVING.stream())
            .map(ChunkerVanillaEntityType.class::cast)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ChunkerVanillaEntityType.class))));

    private GenericEntityTypes() {
    }

    /**
     * Check whether an entity type is a living entity.
     *
     * @param type the type to check.
     * @return true if it is a living entity.
     */
    public static boolean isLiving(ChunkerEntityType type) {
        return LIVING.contains(type);
    }
}

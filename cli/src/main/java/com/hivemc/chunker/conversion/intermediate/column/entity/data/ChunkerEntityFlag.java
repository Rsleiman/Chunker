package com.hivemc.chunker.conversion.intermediate.column.entity.data;

import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Boolean states of specific mobs, each saved by Java 26.3 as a boolean under its own key.
 */
public enum ChunkerEntityFlag {
    /**
     * A hoglin which piglins don't hunt (Bedrock's unhuntable_adult group).
     */
    CANNOT_BE_HUNTED("CannotBeHunted", ChunkerVanillaEntityType.HOGLIN),
    /**
     * The captain of a patrol, which wears the ominous banner.
     */
    PATROL_LEADER("PatrolLeader", ChunkerVanillaEntityType.PILLAGER, ChunkerVanillaEntityType.VINDICATOR,
            ChunkerVanillaEntityType.EVOKER, ChunkerVanillaEntityType.ILLUSIONER, ChunkerVanillaEntityType.RAVAGER,
            ChunkerVanillaEntityType.WITCH),
    /**
     * A creeper struck by lightning.
     */
    POWERED("powered", ChunkerVanillaEntityType.CREEPER),
    /**
     * A bogged whose mushrooms were sheared.
     */
    BOGGED_SHEARED("sheared", ChunkerVanillaEntityType.BOGGED),
    /**
     * A snow golem which still wears its pumpkin.
     */
    PUMPKIN("Pumpkin", ChunkerVanillaEntityType.SNOW_GOLEM),
    /**
     * An ocelot which trusts players.
     */
    TRUSTING("Trusting", ChunkerVanillaEntityType.OCELOT),
    /**
     * A turtle which will lay eggs on its home beach.
     */
    HAS_EGG("has_egg", ChunkerVanillaEntityType.TURTLE),
    /**
     * A bee which has stung and will die.
     */
    HAS_STUNG("HasStung", ChunkerVanillaEntityType.BEE),
    /**
     * A skeleton horse which spawns skeleton riders when a player comes close.
     */
    SKELETON_TRAP("SkeletonTrap", ChunkerVanillaEntityType.SKELETON_HORSE),
    /**
     * A screaming goat.
     */
    SCREAMING_GOAT("IsScreamingGoat", ChunkerVanillaEntityType.GOAT),
    /**
     * A goat which still has its left horn.
     */
    LEFT_HORN("HasLeftHorn", ChunkerVanillaEntityType.GOAT),
    /**
     * A goat which still has its right horn.
     */
    RIGHT_HORN("HasRightHorn", ChunkerVanillaEntityType.GOAT),
    /**
     * A baby which doesn't grow up (fed a golden dandelion).
     */
    AGE_LOCKED("AgeLocked", GenericEntityTypes.AGEABLE);

    private final String javaKey;
    private final Set<ChunkerEntityType> types;

    ChunkerEntityFlag(String javaKey, ChunkerVanillaEntityType... types) {
        this(javaKey, EnumSet.of(types[0], types));
    }

    ChunkerEntityFlag(String javaKey, Set<? extends ChunkerEntityType> types) {
        this.javaKey = javaKey;
        this.types = Collections.unmodifiableSet(types);
    }

    /**
     * Get the key Java saves the flag under.
     *
     * @return the NBT key.
     */
    public String getJavaKey() {
        return javaKey;
    }

    /**
     * Check whether an entity type has this flag.
     *
     * @param type the entity type.
     * @return true if Java saves this flag for the type.
     */
    public boolean appliesTo(ChunkerEntityType type) {
        return types.contains(type);
    }
}

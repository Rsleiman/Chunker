package com.hivemc.chunker.conversion.intermediate.column.entity;

import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Every entity type which is converted as a generic entity.
     */
    public static final Set<ChunkerEntityType> ALL = LIVING;

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

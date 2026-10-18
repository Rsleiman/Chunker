package com.hivemc.chunker.conversion.intermediate.column.entity.data;

/**
 * The equipment slots of a living entity.
 */
public enum ChunkerEquipmentSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    BODY,
    MAINHAND,
    OFFHAND,
    SADDLE;

    /**
     * Get the lowercase name of the slot (as used by Java).
     *
     * @return the name of the slot.
     */
    public String getName() {
        return name().toLowerCase();
    }
}

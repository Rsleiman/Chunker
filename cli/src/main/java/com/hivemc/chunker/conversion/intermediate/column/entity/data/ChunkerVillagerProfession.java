package com.hivemc.chunker.conversion.intermediate.column.entity.data;

/**
 * The profession of a villager or zombie villager.
 */
public enum ChunkerVillagerProfession {
    NONE,
    ARMORER,
    BUTCHER,
    CARTOGRAPHER,
    CLERIC,
    FARMER,
    FISHERMAN,
    FLETCHER,
    LEATHERWORKER,
    LIBRARIAN,
    MASON,
    NITWIT,
    SHEPHERD,
    TOOLSMITH,
    WEAPONSMITH;

    /**
     * Get the lowercase name of the profession (as used by Java and the Bedrock component groups).
     *
     * @return the name of the profession.
     */
    public String getName() {
        return name().toLowerCase();
    }
}

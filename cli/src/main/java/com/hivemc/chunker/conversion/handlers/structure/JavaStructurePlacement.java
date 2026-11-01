package com.hivemc.chunker.conversion.handlers.structure;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The random spread placement of a Java structure set, which decides the single chunk of each grid cell where Java
 * looks for a structure start (/locate and explorer maps check only that chunk).
 *
 * @param salt       the salt of the structure set.
 * @param spacing    the size of a grid cell in chunks.
 * @param separation the minimum distance in chunks between two starts.
 * @param triangular whether the spread is triangular rather than linear.
 */
public record JavaStructurePlacement(int salt, int spacing, int separation, boolean triangular) {
    /**
     * The abandoned camps, which Java added in 26.3.
     */
    public static final Set<String> ABANDONED_CAMPS = Set.of(
            "minecraft:abandoned_camp_bamboo_jungle", "minecraft:abandoned_camp_birch_forest",
            "minecraft:abandoned_camp_cherry_grove", "minecraft:abandoned_camp_dappled_forest",
            "minecraft:abandoned_camp_flower_forest", "minecraft:abandoned_camp_forest",
            "minecraft:abandoned_camp_meadow", "minecraft:abandoned_camp_old_growth_birch_forest",
            "minecraft:abandoned_camp_old_growth_pine_taiga", "minecraft:abandoned_camp_old_growth_spruce_taiga",
            "minecraft:abandoned_camp_pale_garden", "minecraft:abandoned_camp_savanna",
            "minecraft:abandoned_camp_snowy_taiga", "minecraft:abandoned_camp_sparse_jungle",
            "minecraft:abandoned_camp_swamp", "minecraft:abandoned_camp_taiga",
            "minecraft:abandoned_camp_windswept_forest", "minecraft:abandoned_camp_wooded_badlands"
    );

    /**
     * The placement of each structure, from the structure sets of Java 26.3 (data/minecraft/worldgen/structure_set).
     * Structures placed in every chunk (mineshafts, buried treasure), by concentric rings (strongholds) or with
     * their own start rules (monuments) are left out.
     */
    private static final Map<String, JavaStructurePlacement> BY_STRUCTURE = new HashMap<>();

    static {
        for (String camp : ABANDONED_CAMPS) {
            BY_STRUCTURE.put(camp, new JavaStructurePlacement(91231127, 37, 8, false));
        }
        BY_STRUCTURE.put("minecraft:ancient_city", new JavaStructurePlacement(20083232, 24, 8, false));
        BY_STRUCTURE.put("minecraft:desert_pyramid", new JavaStructurePlacement(14357617, 32, 8, false));
        BY_STRUCTURE.put("minecraft:end_city", new JavaStructurePlacement(10387313, 20, 11, true));
        BY_STRUCTURE.put("minecraft:igloo", new JavaStructurePlacement(14357618, 32, 8, false));
        BY_STRUCTURE.put("minecraft:jungle_pyramid", new JavaStructurePlacement(14357619, 32, 8, false));
        BY_STRUCTURE.put("minecraft:fortress", new JavaStructurePlacement(30084232, 27, 4, false));
        BY_STRUCTURE.put("minecraft:bastion_remnant", new JavaStructurePlacement(30084232, 27, 4, false));
        BY_STRUCTURE.put("minecraft:ocean_ruin_cold", new JavaStructurePlacement(14357621, 20, 8, false));
        BY_STRUCTURE.put("minecraft:ocean_ruin_warm", new JavaStructurePlacement(14357621, 20, 8, false));
        BY_STRUCTURE.put("minecraft:pillager_outpost", new JavaStructurePlacement(165745296, 32, 8, false));
        BY_STRUCTURE.put("minecraft:shipwreck", new JavaStructurePlacement(165745295, 24, 4, false));
        BY_STRUCTURE.put("minecraft:shipwreck_beached", new JavaStructurePlacement(165745295, 24, 4, false));
        BY_STRUCTURE.put("minecraft:swamp_hut", new JavaStructurePlacement(14357620, 32, 8, false));
        BY_STRUCTURE.put("minecraft:trail_ruins", new JavaStructurePlacement(83469867, 34, 8, false));
        BY_STRUCTURE.put("minecraft:trial_chambers", new JavaStructurePlacement(94251327, 34, 12, false));
        BY_STRUCTURE.put("minecraft:mansion", new JavaStructurePlacement(10387319, 80, 20, true));
        for (String village : new String[]{"plains", "desert", "savanna", "snowy", "taiga"}) {
            BY_STRUCTURE.put("minecraft:village_" + village, new JavaStructurePlacement(10387312, 34, 8, false));
        }
        for (String portal : new String[]{"", "_desert", "_jungle", "_mountain", "_nether", "_ocean", "_swamp"}) {
            BY_STRUCTURE.put("minecraft:ruined_portal" + portal, new JavaStructurePlacement(34222645, 40, 15, false));
        }
    }

    /**
     * Get the placement of a Java structure.
     *
     * @param javaStructureId the Java identifier of the structure.
     * @return the placement or null if the structure isn't placed by a random spread with a usable grid.
     */
    @Nullable
    public static JavaStructurePlacement of(String javaStructureId) {
        return BY_STRUCTURE.get(javaStructureId);
    }

    /**
     * Check whether a chunk is the one Java picks for its grid cell (RandomSpreadStructurePlacement#getPotentialStructureChunk).
     *
     * @param seed   the world seed.
     * @param chunkX the chunk X.
     * @param chunkZ the chunk Z.
     * @return true if Java looks for a start of this placement in the chunk.
     */
    public boolean isPlacementChunk(long seed, int chunkX, int chunkZ) {
        int gridX = Math.floorDiv(chunkX, spacing);
        int gridZ = Math.floorDiv(chunkZ, spacing);

        // WorldgenRandom#setLargeFeatureWithSalt on a LegacyRandomSource, which matches java.util.Random
        Random random = new Random(gridX * 341873128712L + gridZ * 132897987541L + seed + salt);
        int limit = spacing - separation;
        int spreadX = triangular ? (random.nextInt(limit) + random.nextInt(limit)) / 2 : random.nextInt(limit);
        int spreadZ = triangular ? (random.nextInt(limit) + random.nextInt(limit)) / 2 : random.nextInt(limit);
        return gridX * spacing + spreadX == chunkX && gridZ * spacing + spreadZ == chunkZ;
    }
}

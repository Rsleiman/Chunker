package com.hivemc.chunker.conversion.encoding.java.base.writer;

import com.hivemc.chunker.conversion.encoding.base.Version;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaLivingEntityData;
import com.hivemc.chunker.conversion.encoding.java.util.JavaRecipeTable;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelPlayer;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerPlayerAbilities;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerRespawnPoint;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerUnlockedRecipe;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Writes the player data beyond the position, inventory and game mode: health, hunger, experience, effects, the
 * ender chest, abilities, the respawn point and the last death location.
 * <p>
 * The field names and formats are those of Java 26.3. The respawn point took its current form (yaw and pitch) in the
 * 1.21.9 cycle (data version 4548), so this data is written from 1.21.9 onwards. The recipe IDs are those of 26.3;
 * an earlier version drops the ones it doesn't have when it loads the player.
 */
public final class JavaPlayerData {
    /**
     * The game mode Bedrock uses for "the world's default".
     */
    public static final int DEFAULT_GAME_MODE = 5;

    private JavaPlayerData() {
    }

    /**
     * Check whether the full player data (and player profiles) are written for a version.
     *
     * @param version the output version.
     * @return true for Java 1.21.9 onwards.
     */
    public static boolean isSupported(Version version) {
        return version.isGreaterThanOrEqual(1, 21, 9);
    }

    /**
     * Write the player data.
     *
     * @param resolvers the resolvers.
     * @param converter the converter, for reporting recipes Java doesn't have.
     * @param output    the player NBT, which already has the position, inventory and game mode.
     * @param player    the player.
     */
    public static void write(JavaResolvers resolvers, Converter converter, CompoundTag output, ChunkerLevelPlayer player) {
        // Without a game mode Java uses the server's default, which is what Bedrock's "world default" means
        if (player.getGameType() == DEFAULT_GAME_MODE) {
            output.remove("playerGameType");
        }

        // Health, air and falling
        if (player.getHealth() != null) {
            output.put("Health", player.getHealth().floatValue());
        }
        if (player.getAbsorption() != null) {
            output.put("AbsorptionAmount", player.getAbsorption().floatValue());
        }
        if (player.getAir() != null) {
            output.put("Air", player.getAir().shortValue());
        }
        if (player.getOnGround() != null) {
            output.put("OnGround", player.getOnGround() ? (byte) 1 : (byte) 0);
        }
        if (player.getFallDistance() != null) {
            output.put("fall_distance", player.getFallDistance().doubleValue());
        }

        // Hunger
        if (player.getFoodLevel() != null) {
            output.put("foodLevel", player.getFoodLevel().intValue());
        }
        if (player.getFoodSaturation() != null) {
            output.put("foodSaturationLevel", player.getFoodSaturation().floatValue());
        }
        if (player.getFoodExhaustion() != null) {
            output.put("foodExhaustionLevel", player.getFoodExhaustion().floatValue());
        }

        // Experience
        if (player.getXpLevel() != null) {
            int level = player.getXpLevel();
            float progress = player.getXpProgress() != null ? player.getXpProgress() : 0F;
            output.put("XpLevel", level);
            output.put("XpP", progress);
            output.put("XpTotal", totalExperience(level, progress));
        }

        // Effects
        if (player.getEffects() != null && !player.getEffects().isEmpty()) {
            output.put("active_effects", JavaLivingEntityData.writeEffects(resolvers, player.getEffects()));
        }

        // Ender chest and the selected slot
        if (player.getEnderChest() != null) {
            ListTag<CompoundTag, Map<String, Tag<?>>> enderItems = new ListTag<>(TagType.COMPOUND, player.getEnderChest().size());
            for (Byte2ObjectMap.Entry<ChunkerItemStack> entry : player.getEnderChest().byte2ObjectEntrySet()) {
                if (entry.getValue().getIdentifier().isAir()) continue;
                Optional<CompoundTag> item = resolvers.writeItem(entry.getValue());
                if (item.isEmpty()) continue;
                item.get().put("Slot", entry.getByteKey());
                enderItems.add(item.get());
            }
            output.put("EnderItems", enderItems);
        }
        if (player.getSelectedSlot() != null) {
            output.put("SelectedItemSlot", player.getSelectedSlot().intValue());
        }

        // Abilities
        ChunkerPlayerAbilities abilities = player.getAbilities();
        if (abilities != null) {
            CompoundTag abilitiesTag = new CompoundTag(7);
            abilitiesTag.put("invulnerable", abilities.invulnerable() ? (byte) 1 : (byte) 0);
            abilitiesTag.put("flying", abilities.flying() ? (byte) 1 : (byte) 0);
            abilitiesTag.put("mayfly", abilities.mayFly() ? (byte) 1 : (byte) 0);
            abilitiesTag.put("instabuild", abilities.instabuild() ? (byte) 1 : (byte) 0);
            abilitiesTag.put("mayBuild", abilities.mayBuild() ? (byte) 1 : (byte) 0);
            abilitiesTag.put("flySpeed", abilities.flySpeed());
            abilitiesTag.put("walkSpeed", abilities.walkSpeed());
            output.put("abilities", abilitiesTag);
        }

        // Respawn point and last death
        ChunkerRespawnPoint respawnPoint = player.getRespawnPoint();
        if (respawnPoint != null) {
            CompoundTag respawn = new CompoundTag(5);
            respawn.put("dimension", respawnPoint.dimension().getIdentifier());
            respawn.put("pos", new int[]{respawnPoint.x(), respawnPoint.y(), respawnPoint.z()});
            respawn.put("yaw", respawnPoint.yaw());
            respawn.put("pitch", 0F);
            respawn.put("forced", respawnPoint.forced() ? (byte) 1 : (byte) 0);
            output.put("respawn", respawn);
        }
        ChunkerGlobalPosition lastDeath = player.getLastDeathLocation();
        if (lastDeath != null) {
            CompoundTag location = new CompoundTag(2);
            location.put("dimension", lastDeath.dimension().getIdentifier());
            location.put("pos", new int[]{lastDeath.x(), lastDeath.y(), lastDeath.z()});
            output.put("LastDeathLocation", location);
        }

        // Unlocked recipes
        if (player.getUnlockedRecipes() != null) {
            Set<String> recipes = javaRecipes(resolvers, converter, player.getUnlockedRecipes());
            if (!recipes.isEmpty()) {
                ListTag<StringTag, String> known = new ListTag<>(TagType.STRING, recipes.size());
                for (String recipe : recipes) {
                    known.add(new StringTag(recipe));
                }
                CompoundTag recipeBook = new CompoundTag(2);
                recipeBook.put("recipes", known);
                recipeBook.put("toBeDisplayed", new ListTag<>(TagType.STRING, 0));
                output.put("recipeBook", recipeBook);
            }
        }
    }

    /**
     * Get the Java recipes for unlocked recipes: every recipe made at the same station that makes the same item.
     * A recipe Java doesn't have (such as the saddle, which Java can't craft) is reported and left out.
     *
     * @param resolvers the resolvers, for the items.
     * @param converter the converter, for reporting.
     * @param unlocked  the unlocked recipes.
     * @return the Java recipe IDs, sorted.
     */
    static Set<String> javaRecipes(JavaResolvers resolvers, Converter converter, List<ChunkerUnlockedRecipe> unlocked) {
        Set<String> recipes = new TreeSet<>();
        for (ChunkerUnlockedRecipe recipe : unlocked) {
            // Written as an item stack, as in an inventory (an item identifier alone can be a block state, such as
            // a wall torch for a torch)
            Optional<CompoundTag> item = resolvers.writeItem(recipe.result());
            if (item.isEmpty()) continue;
            String id = item.get().getString("id", "");
            List<String> ids = JavaRecipeTable.get(recipe.station(), id);
            if (ids.isEmpty()) {
                converter.logMissingMapping(Converter.MissingMappingType.RECIPE, recipe.station().getName() + " " + id);
                continue;
            }
            recipes.addAll(ids);
        }
        return recipes;
    }

    /**
     * Calculate the total experience of a level and the progress towards the next, as Java counts it.
     *
     * @param level    the experience level.
     * @param progress the progress towards the next level (0 to 1).
     * @return the total experience points.
     */
    public static int totalExperience(int level, float progress) {
        long total;
        if (level <= 16) {
            total = (long) level * level + 6L * level;
        } else if (level <= 31) {
            total = (long) Math.floor(2.5 * level * level - 40.5 * level + 360);
        } else {
            total = (long) Math.floor(4.5 * level * level - 162.5 * level + 2220);
        }
        // The progress is a fraction of whole points, rounded to undo float error (e.g. 10.99998 is 11 points)
        total += Math.round(progress * experienceForNextLevel(level));
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /**
     * Get the experience needed to go from a level to the next one (Player.getXpNeededForNextLevel).
     *
     * @param level the level.
     * @return the experience points.
     */
    private static int experienceForNextLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }
}

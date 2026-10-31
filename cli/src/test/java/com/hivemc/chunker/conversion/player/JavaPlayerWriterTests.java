package com.hivemc.chunker.conversion.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.JavaDataVersion;
import com.hivemc.chunker.conversion.encoding.java.JavaEncoders;
import com.hivemc.chunker.conversion.encoding.java.base.writer.JavaLevelWriter;
import com.hivemc.chunker.conversion.encoding.java.base.writer.JavaPlayerData;
import com.hivemc.chunker.conversion.encoding.java.base.writer.JavaPlayerProfiles;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelPlayer;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.DoubleTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hivemc.chunker.conversion.entity.BedrockActors.compounds;
import static com.hivemc.chunker.conversion.player.BedrockPlayers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for writing Bedrock players as Java player files (profiles).
 */
public class JavaPlayerWriterTests {
    private static final long OTHER_PLAYER = -17179869183L;
    private static final UUID OTHER_ACCOUNT = UUID.fromString("1f6f7a5e-27e4-4c1d-9bb4-0b2f2d6d43a1");

    private static JavaLevelWriter writer(JavaDataVersion version, File output, WorldConverter converter) {
        return (JavaLevelWriter) JavaEncoders.getNearestEncoder(version)
                .writerConstructor()
                .construct(output, version.getVersion(), converter);
    }

    private static ChunkerLevel level() {
        ChunkerLevel level = new ChunkerLevel(new ChunkerLevelSettings(), null, Collections.emptyList(), null, Collections.emptyList());
        level.getSettings().LevelName = "Test World";
        return level;
    }

    private static CompoundTag write(CompoundTag record) throws Exception {
        WorldConverter converter = converter(Map.of());
        return writer(JavaDataVersion.V26_3, null, converter).createPlayerTag(level(), read(converter, record));
    }

    /**
     * A record of a player who has played for a while, modelled on the 18 October world.
     */
    private static CompoundTag playedRecord() {
        CompoundTag record = record();
        record.put("DimensionId", 1);
        setAttribute(record, "minecraft:health", 8.473446F);
        setAttribute(record, "minecraft:absorption", 4F);
        setAttribute(record, "minecraft:player.hunger", 12F);
        setAttribute(record, "minecraft:player.saturation", 0.5F);
        setAttribute(record, "minecraft:player.exhaustion", 0.8834662F);
        record.put("Air", (short) 120);
        record.put("PlayerLevel", 17);
        record.put("PlayerLevelProgress", 0.23404229F);
        record.put("SelectedInventorySlot", 5);
        setSlot(record, "Inventory", item(0, "minecraft:diamond_sword", 1));
        setSlot(record, "Inventory", item(35, "minecraft:bread", 12));
        record.put("Armor", compounds(item("minecraft:iron_helmet", 1), item("minecraft:iron_chestplate", 1),
                item("minecraft:iron_leggings", 1), item("minecraft:iron_boots", 1), emptyItem()));
        record.put("Offhand", compounds(item("minecraft:shield", 1)));
        setSlot(record, "EnderChestInventory", item(26, "minecraft:emerald", 7));

        CompoundTag speed = new CompoundTag();
        speed.put("Id", (byte) 1);
        speed.put("Amplifier", (byte) 1);
        speed.put("Duration", 3600);
        speed.put("Ambient", (byte) 0);
        speed.put("ShowParticles", (byte) 1);
        record.put("ActiveEffects", compounds(speed));

        record.put("SpawnBlockPositionX", 2461);
        record.put("SpawnBlockPositionY", -3);
        record.put("SpawnBlockPositionZ", -5467);
        record.put("SpawnDimension", 0);
        record.put("SpawnX", 2463);
        record.put("SpawnY", -3);
        record.put("SpawnZ", -5467);
        record.put("HasDiedBefore", (byte) 1);
        record.put("DeathDimension", 1);
        record.put("DeathPositionX", 275);
        record.put("DeathPositionY", 71);
        record.put("DeathPositionZ", -322);
        return record;
    }

    @Test
    public void testUnlockedRecipesBecomeTheJavaRecipesForTheSameItems() throws Exception {
        CompoundTag record = record();
        setUnlockedRecipes(record, "TrapdoorBirch_recipeId", "harness_color_1_3", "bed_color_bamboo_4",
                "minecraft:furnace_beef", "minecraft:stonecutter_stonebrick_slab", "minecraft:Torch_charcoal_recipeId");
        CompoundTag recipeBook = write(record).getCompound("recipeBook");

        assertNotNull(recipeBook);
        // Every Java recipe at the same station that makes the same item, sorted
        assertEquals(List.of(
                "minecraft:birch_trapdoor",
                "minecraft:brown_harness",
                "minecraft:cooked_beef",
                "minecraft:cooked_beef_from_campfire_cooking",
                "minecraft:cooked_beef_from_smoking",
                "minecraft:dye_brown_harness",
                "minecraft:dye_yellow_bed",
                "minecraft:stone_brick_slab_from_stone_bricks_stonecutting",
                "minecraft:stone_brick_slab_from_stone_stonecutting",
                "minecraft:torch",
                "minecraft:yellow_bed"
        ), recipeBook.getListValues("recipes", StringTag.class, null));
        assertEquals(List.of(), recipeBook.getListValues("toBeDisplayed", StringTag.class, null));

        // No recipe book without unlocked recipes
        assertFalse(write(record()).contains("recipeBook"));
    }

    @Test
    public void testPositionAndIdentity() throws Exception {
        CompoundTag java = write(playedRecord());

        List<Double> pos = java.getListValues("Pos", DoubleTag.class, null);
        assertEquals(100.5, pos.get(0), 1e-6);
        assertEquals(75.0, pos.get(1), 1e-4);
        assertEquals(9.5, pos.get(2), 1e-6);
        assertEquals("minecraft:the_nether", java.getString("Dimension"));
        assertEquals(JavaDataVersion.V26_3.getDataVersion(), java.getInt("DataVersion"));

        // The account which joins decides the UUID
        assertFalse(java.contains("UUID"));
    }

    @Test
    public void testHealthHungerAndAir() throws Exception {
        CompoundTag java = write(playedRecord());

        assertEquals(8.473446F, java.getFloat("Health"));
        assertEquals(4F, java.getFloat("AbsorptionAmount"));
        assertEquals((short) 120, java.getShort("Air"));
        assertEquals(1, java.getByte("OnGround"));
        assertEquals(0D, java.getDouble("fall_distance"));
        assertEquals(12, java.getInt("foodLevel"));
        assertEquals(0.5F, java.getFloat("foodSaturationLevel"));
        assertEquals(0.8834662F, java.getFloat("foodExhaustionLevel"));
    }

    @Test
    public void testExperience() throws Exception {
        CompoundTag java = write(playedRecord());

        assertEquals(17, java.getInt("XpLevel"));
        assertEquals(0.23404229F, java.getFloat("XpP"));
        assertEquals(394 + 11, java.getInt("XpTotal")); // 394 for level 17, then 23% of the 47 needed for 18

        // The totals of Java's three formulas
        assertEquals(0, JavaPlayerData.totalExperience(0, 0F));
        assertEquals(352, JavaPlayerData.totalExperience(16, 0F));
        assertEquals(394, JavaPlayerData.totalExperience(17, 0F));
        assertEquals(1507, JavaPlayerData.totalExperience(31, 0F));
        assertEquals(1628, JavaPlayerData.totalExperience(32, 0F));
    }

    @Test
    public void testEffects() throws Exception {
        CompoundTag effect = write(playedRecord()).getList("active_effects", CompoundTag.class, null).get(0);

        assertEquals("minecraft:speed", effect.getString("id"));
        assertEquals(1, effect.getByte("amplifier"));
        assertEquals(3600, effect.getInt("duration"));
        assertEquals(1, effect.getByte("show_particles"));
    }

    @Test
    public void testInventoryEquipmentAndEnderChest() throws Exception {
        CompoundTag java = write(playedRecord());

        // Armor and the off hand go in equipment, not the inventory
        ListTag<CompoundTag, ?> inventory = java.getList("Inventory", CompoundTag.class, null);
        assertEquals(2, inventory.size());
        assertEquals("minecraft:diamond_sword", inventory.get(0).getString("id"));
        assertEquals(0, inventory.get(0).getByte("Slot"));
        assertEquals("minecraft:bread", inventory.get(1).getString("id"));
        assertEquals(35, inventory.get(1).getByte("Slot"));
        assertEquals(12, inventory.get(1).getInt("count"));

        CompoundTag equipment = java.getCompound("equipment");
        assertEquals("minecraft:iron_helmet", equipment.getCompound("head").getString("id"));
        assertEquals("minecraft:iron_chestplate", equipment.getCompound("chest").getString("id"));
        assertEquals("minecraft:iron_leggings", equipment.getCompound("legs").getString("id"));
        assertEquals("minecraft:iron_boots", equipment.getCompound("feet").getString("id"));
        assertEquals("minecraft:shield", equipment.getCompound("offhand").getString("id"));
        assertEquals(5, java.getInt("SelectedItemSlot"));

        ListTag<CompoundTag, ?> enderItems = java.getList("EnderItems", CompoundTag.class, null);
        assertEquals(1, enderItems.size());
        assertEquals("minecraft:emerald", enderItems.get(0).getString("id"));
        assertEquals(26, enderItems.get(0).getByte("Slot"));
        assertEquals(7, enderItems.get(0).getInt("count"));
    }

    @Test
    public void testGameModeAndAbilities() throws Exception {
        // The world's default (5) is left out, so the server's default applies
        assertFalse(write(record()).contains("playerGameType"));

        CompoundTag creative = record();
        creative.put("PlayerGameMode", 1);
        creative.getCompound("abilities").put("mayfly", (byte) 1);
        creative.getCompound("abilities").put("instabuild", (byte) 1);
        CompoundTag java = write(creative);
        assertEquals(1, java.getInt("playerGameType"));

        CompoundTag abilities = java.getCompound("abilities");
        assertEquals(1, abilities.getByte("mayfly"));
        assertEquals(0, abilities.getByte("flying"));
        assertEquals(1, abilities.getByte("instabuild"));
        assertEquals(0, abilities.getByte("invulnerable"));
        assertEquals(1, abilities.getByte("mayBuild"));
        assertEquals(0.1F, abilities.getFloat("walkSpeed"));
        assertEquals(0.05F, abilities.getFloat("flySpeed"));
    }

    @Test
    public void testRespawnPointAndLastDeath() throws Exception {
        CompoundTag java = write(playedRecord());

        CompoundTag respawn = java.getCompound("respawn");
        assertEquals("minecraft:overworld", respawn.getString("dimension"));
        assertArrayEquals(new int[]{2461, -3, -5467}, respawn.getIntArray("pos"));
        assertEquals(0F, respawn.getFloat("yaw"));
        assertEquals(0F, respawn.getFloat("pitch"));
        assertEquals(0, respawn.getByte("forced")); // A bed

        CompoundTag command = record();
        command.put("SpawnDimension", 0);
        command.put("SpawnX", 64);
        command.put("SpawnY", 75);
        command.put("SpawnZ", 14);
        CompoundTag forced = write(command).getCompound("respawn");
        assertArrayEquals(new int[]{64, 75, 14}, forced.getIntArray("pos"));
        assertEquals(1, forced.getByte("forced"));

        CompoundTag lastDeath = java.getCompound("LastDeathLocation");
        assertEquals("minecraft:the_nether", lastDeath.getString("dimension"));
        assertArrayEquals(new int[]{275, 71, -322}, lastDeath.getIntArray("pos"));

        // Players without either use the world spawn
        CompoundTag fresh = write(record());
        assertFalse(fresh.contains("respawn"));
        assertFalse(fresh.contains("LastDeathLocation"));
    }

    @Test
    public void testOlderVersionsKeepTheOriginalPlayerData() throws Exception {
        WorldConverter converter = converter(Map.of());
        ChunkerLevelPlayer player = read(converter, playedRecord());
        CompoundTag java = writer(JavaDataVersion.V1_21_8, null, converter).createPlayerTag(level(), player);

        // Position, inventory and game mode as before; the rest only from 1.21.9
        assertTrue(java.contains("Pos"));
        assertTrue(java.contains("Inventory"));
        assertTrue(java.contains("playerGameType"));
        assertFalse(java.contains("Health"));
        assertFalse(java.contains("respawn"));
        assertFalse(java.contains("EnderItems"));
    }

    @Test
    public void testProfilesAreWrittenAndMappedPlayersInstalled(@TempDir Path output) throws Exception {
        WorldConverter converter = converter(Map.of(PLAYER, JAVA_ACCOUNT));
        converter.setInputDescription("C:\\worlds\\completed-world");
        CompoundTag otherRecord = record();
        otherRecord.put("UniqueID", OTHER_PLAYER);
        List<ChunkerLevelPlayer> players = List.of(read(converter, playedRecord()), read(converter, otherRecord));
        JavaLevelWriter writer = writer(JavaDataVersion.V26_3, output.toFile(), converter);
        File playersData = output.resolve("players").resolve("data").toFile();
        JavaPlayerProfiles.write(output.toFile(), playersData, players, player -> writer.createPlayerTag(level(), player), converter, "Test World");

        // A profile for every player, named after the derived UUID
        File folder = output.resolve(JavaPlayerProfiles.FOLDER).toFile();
        File mappedProfile = new File(folder, BedrockEntityUUIDs.toUUID(PLAYER) + ".dat");
        File otherProfile = new File(folder, BedrockEntityUUIDs.toUUID(OTHER_PLAYER) + ".dat");
        assertTrue(mappedProfile.isFile());
        assertTrue(otherProfile.isFile());
        assertFalse(Tag.readGZipJavaNBT(Files.readAllBytes(otherProfile.toPath())).contains("UUID"));

        // Only the mapped player is installed, as an identical copy
        String[] installed = playersData.list();
        assertNotNull(installed);
        assertArrayEquals(new String[]{JAVA_ACCOUNT + ".dat"}, installed);
        assertArrayEquals(Files.readAllBytes(mappedProfile.toPath()), Files.readAllBytes(new File(playersData, JAVA_ACCOUNT + ".dat").toPath()));

        // The mapping
        JsonObject mapping = JsonParser.parseString(Files.readString(new File(folder, "mapping.json").toPath())).getAsJsonObject();
        assertEquals("C:\\worlds\\completed-world", mapping.getAsJsonObject("source").get("inputWorld").getAsString());
        assertEquals("Test World", mapping.getAsJsonObject("source").get("levelName").getAsString());
        JsonObject mapped = mapping.getAsJsonObject("players").getAsJsonObject(String.valueOf(PLAYER));
        assertEquals(mappedProfile.getName(), mapped.get("profile").getAsString());
        assertEquals(JAVA_ACCOUNT.toString(), mapped.get("javaUUID").getAsString());
        JsonObject other = mapping.getAsJsonObject("players").getAsJsonObject(String.valueOf(OTHER_PLAYER));
        assertEquals(otherProfile.getName(), other.get("profile").getAsString());
        assertTrue(other.get("javaUUID").isJsonNull());
        assertTrue(new File(folder, "README.md").isFile());
    }

    @Test
    public void testUnmappedProfilesAreTheSameAsMappedOnes(@TempDir Path output) throws Exception {
        // A profile doesn't depend on whether its player is assigned
        WorldConverter unmapped = converter(Map.of());
        WorldConverter mapped = converter(Map.of(PLAYER, JAVA_ACCOUNT));
        CompoundTag unmappedTag = writer(JavaDataVersion.V26_3, null, unmapped).createPlayerTag(level(), read(unmapped, playedRecord()));
        CompoundTag mappedTag = writer(JavaDataVersion.V26_3, null, mapped).createPlayerTag(level(), read(mapped, playedRecord()));
        assertEquals(unmappedTag, mappedTag);
    }

    @Test
    public void testAnAccountGetsOnlyOnePlayer(@TempDir Path output) throws Exception {
        WorldConverter converter = converter(Map.of(PLAYER, JAVA_ACCOUNT, OTHER_PLAYER, JAVA_ACCOUNT));
        CompoundTag otherRecord = record();
        otherRecord.put("UniqueID", OTHER_PLAYER);
        CompoundTag duplicate = playedRecord();
        List<ChunkerLevelPlayer> players = List.of(read(converter, playedRecord()), read(converter, otherRecord), read(converter, duplicate));
        JavaLevelWriter writer = writer(JavaDataVersion.V26_3, output.toFile(), converter);
        File playersData = output.resolve("players").resolve("data").toFile();
        JavaPlayerProfiles.write(output.toFile(), playersData, players, player -> writer.createPlayerTag(level(), player), converter, null);

        // The first player keeps the account, the second keeps only their profile, the duplicate record is skipped
        assertArrayEquals(new String[]{JAVA_ACCOUNT + ".dat"}, playersData.list());
        File folder = output.resolve(JavaPlayerProfiles.FOLDER).toFile();
        JsonObject mapping = JsonParser.parseString(Files.readString(new File(folder, "mapping.json").toPath())).getAsJsonObject();
        assertEquals(JAVA_ACCOUNT.toString(), mapping.getAsJsonObject("players").getAsJsonObject(String.valueOf(PLAYER)).get("javaUUID").getAsString());
        assertTrue(mapping.getAsJsonObject("players").getAsJsonObject(String.valueOf(OTHER_PLAYER)).get("javaUUID").isJsonNull());
        assertEquals(2, mapping.getAsJsonObject("players").size());
        assertEquals(2, folder.list((dir, name) -> name.endsWith(".dat")).length);
    }

    @Test
    public void testRefusesAWorldWithPlayerFiles(@TempDir Path output) throws Exception {
        File playersData = output.resolve("players").resolve("data").toFile();

        // Missing or empty folders are fine
        JavaPlayerProfiles.checkOutputHasNoPlayers(output.toFile(), playersData);
        assertTrue(playersData.mkdirs());
        JavaPlayerProfiles.checkOutputHasNoPlayers(output.toFile(), playersData);

        // A leftover player file
        Files.writeString(new File(playersData, JAVA_ACCOUNT + ".dat").toPath(), "x");
        assertThrows(IllegalStateException.class, () -> JavaPlayerProfiles.checkOutputHasNoPlayers(output.toFile(), playersData));
        Files.delete(new File(playersData, JAVA_ACCOUNT + ".dat").toPath());

        // A leftover profile
        File folder = output.resolve(JavaPlayerProfiles.FOLDER).toFile();
        assertTrue(folder.mkdirs());
        Files.writeString(new File(folder, "mapping.json").toPath(), "{}");
        assertThrows(IllegalStateException.class, () -> JavaPlayerProfiles.checkOutputHasNoPlayers(output.toFile(), playersData));
    }
}

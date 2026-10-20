package com.hivemc.chunker.conversion.player;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.item.ChunkerVanillaItemType;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.potion.ChunkerEffectType;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerMobEffect;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelPlayer;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerPlayerAbilities;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerRespawnPoint;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.hivemc.chunker.conversion.entity.BedrockActors.compounds;
import static com.hivemc.chunker.conversion.player.BedrockPlayers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for reading Bedrock player records.
 */
public class BedrockPlayerReaderTests {
    private static final WorldConverter UNMAPPED = converter(Map.of());

    @Test
    public void testPositionIsAtTheFeet() {
        CompoundTag record = record();
        record.put("DimensionId", 1);
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertEquals(Dimension.NETHER, player.getDimension());
        assertEquals(100.5, player.getPositionX(), 1e-6);
        assertEquals(75.0, player.getPositionY(), 1e-4); // Bedrock saves the eye position, 1.62001 above the feet
        assertEquals(9.5, player.getPositionZ(), 1e-6);
        assertEquals(90F, player.getYaw());
        assertEquals(10F, player.getPitch());
        assertEquals(Boolean.TRUE, player.getOnGround());
        assertEquals(0F, player.getFallDistance());
    }

    @Test
    public void testIdentity() {
        ChunkerLevelPlayer unmapped = read(UNMAPPED, record());
        assertEquals(String.valueOf(PLAYER), unmapped.getSourceId());
        assertEquals(BedrockEntityUUIDs.toUUID(PLAYER), unmapped.getProfileUUID());
        assertNull(unmapped.getAccountUUID());

        // A mapped player keeps the same profile and gets an account
        ChunkerLevelPlayer mapped = read(converter(Map.of(PLAYER, JAVA_ACCOUNT)), record());
        assertEquals(BedrockEntityUUIDs.toUUID(PLAYER), mapped.getProfileUUID());
        assertEquals(JAVA_ACCOUNT, mapped.getAccountUUID());
    }

    @Test
    public void testHealthHungerAndAirAreKeptAsTheyAre() {
        CompoundTag record = record();
        setAttribute(record, "minecraft:health", 8.473446F);
        setAttribute(record, "minecraft:absorption", 4F);
        setAttribute(record, "minecraft:player.hunger", 12F);
        setAttribute(record, "minecraft:player.saturation", 0.5F);
        setAttribute(record, "minecraft:player.exhaustion", 0.8834662F);
        record.put("Air", (short) 120);
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertEquals(8.473446F, player.getHealth());
        assertEquals(4F, player.getAbsorption());
        assertEquals(12, player.getFoodLevel());
        assertEquals(0.5F, player.getFoodSaturation());
        assertEquals(0.8834662F, player.getFoodExhaustion());
        assertEquals((short) 120, player.getAir());
    }

    @Test
    public void testExperience() {
        CompoundTag record = record();
        record.put("PlayerLevel", 17);
        record.put("PlayerLevelProgress", 0.23404229F);
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertEquals(17, player.getXpLevel());
        assertEquals(0.23404229F, player.getXpProgress());
    }

    @Test
    public void testEffects() {
        CompoundTag record = record();
        CompoundTag speed = new CompoundTag();
        speed.put("Id", (byte) 1);
        speed.put("Amplifier", (byte) 1);
        speed.put("Duration", 3600);
        speed.put("DurationEasy", 3600);
        speed.put("DurationNormal", 3600);
        speed.put("DurationHard", 3600);
        speed.put("Ambient", (byte) 0);
        speed.put("ShowParticles", (byte) 1);
        speed.put("DisplayOnScreenTextureAnimation", (byte) 0);
        record.put("ActiveEffects", compounds(speed));
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertEquals(List.of(new ChunkerMobEffect(ChunkerEffectType.SPEED, 1, 3600, false, true)), player.getEffects());

        // No effects are left out
        assertNull(read(UNMAPPED, record()).getEffects());
    }

    @Test
    public void testInventoryArmorAndOffhand() {
        CompoundTag record = record();
        setSlot(record, "Inventory", item(0, "minecraft:diamond_sword", 1));
        setSlot(record, "Inventory", item(9, "minecraft:arrow", 64));
        setSlot(record, "Inventory", item(35, "minecraft:bread", 12));
        record.put("Armor", compounds(
                item("minecraft:iron_helmet", 1),
                item("minecraft:iron_chestplate", 1),
                emptyItem(),
                item("minecraft:iron_boots", 1),
                emptyItem()
        ));
        record.put("Offhand", compounds(item("minecraft:shield", 1)));
        record.put("SelectedInventorySlot", 5);
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        // Empty slots are skipped
        assertEquals(7, player.getInventory().size());
        assertItem(player.getInventory().get((byte) 0), ChunkerVanillaItemType.DIAMOND_SWORD, 1);
        assertItem(player.getInventory().get((byte) 9), ChunkerVanillaItemType.ARROW, 64);
        assertItem(player.getInventory().get((byte) 35), ChunkerVanillaItemType.BREAD, 12);

        // Armor lines up with Java (103 = head, 100 = feet), the off hand is 150
        assertItem(player.getInventory().get((byte) 103), ChunkerVanillaItemType.IRON_HELMET, 1);
        assertItem(player.getInventory().get((byte) 102), ChunkerVanillaItemType.IRON_CHESTPLATE, 1);
        assertNull(player.getInventory().get((byte) 101));
        assertItem(player.getInventory().get((byte) 100), ChunkerVanillaItemType.IRON_BOOTS, 1);
        assertItem(player.getInventory().get((byte) 150), ChunkerVanillaItemType.SHIELD, 1);
        assertEquals(5, player.getSelectedSlot());
    }

    @Test
    public void testEnderChest() {
        CompoundTag record = record();
        setSlot(record, "EnderChestInventory", item(0, "minecraft:diamond", 3));
        setSlot(record, "EnderChestInventory", item(26, "minecraft:emerald", 7));
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertEquals(2, player.getEnderChest().size());
        assertItem(player.getEnderChest().get((byte) 0), ChunkerVanillaItemType.DIAMOND, 3);
        assertItem(player.getEnderChest().get((byte) 26), ChunkerVanillaItemType.EMERALD, 7);

        // An empty ender chest has no items
        assertTrue(read(UNMAPPED, record()).getEnderChest().isEmpty());
    }

    @Test
    public void testItemsLeftInTheUIGoToFreeSlots() {
        CompoundTag record = record();
        setSlot(record, "Inventory", item(0, "minecraft:diamond_sword", 1));
        record.put("PlayerUIItems", compounds(
                item(0, "minecraft:stick", 4), // The cursor
                item(28, "minecraft:paper", 2), // The crafting grid
                item(50, "minecraft:crafting_table", 1) // The crafting result is only a preview
        ));
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertItem(player.getInventory().get((byte) 1), ChunkerVanillaItemType.STICK, 4);
        assertItem(player.getInventory().get((byte) 2), ChunkerVanillaItemType.PAPER, 2);
        assertEquals(3, player.getInventory().size());
    }

    @Test
    public void testGameModeAndAbilities() {
        // Game mode 5 (the world's default) is kept for the writer to decide
        assertEquals(5, read(UNMAPPED, record()).getGameType());

        CompoundTag record = record();
        record.put("PlayerGameMode", 1);
        CompoundTag abilities = record.getCompound("abilities");
        abilities.put("mayfly", (byte) 1);
        abilities.put("flying", (byte) 1);
        abilities.put("instabuild", (byte) 1);
        abilities.put("invulnerable", (byte) 1);
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        assertEquals(1, player.getGameType());
        assertEquals(new ChunkerPlayerAbilities(true, true, true, true, true, 0.1F, 0.05F), player.getAbilities());
    }

    @Test
    public void testBedSpawnPoint() {
        CompoundTag record = record();
        record.put("SpawnBlockPositionX", 2461);
        record.put("SpawnBlockPositionY", -3);
        record.put("SpawnBlockPositionZ", -5467);
        record.put("SpawnDimension", 0);
        record.put("SpawnX", 2463);
        record.put("SpawnY", -3);
        record.put("SpawnZ", -5467);

        // A bed is the block, and isn't forced
        assertEquals(new ChunkerRespawnPoint(Dimension.OVERWORLD, 2461, -3, -5467, 0F, false), read(UNMAPPED, record).getRespawnPoint());
    }

    @Test
    public void testCommandSpawnPoint() {
        CompoundTag record = record();
        record.put("SpawnDimension", 0);
        record.put("SpawnX", 64);
        record.put("SpawnY", 75);
        record.put("SpawnZ", 14);

        // A position set by /spawnpoint is forced
        assertEquals(new ChunkerRespawnPoint(Dimension.OVERWORLD, 64, 75, 14, 0F, true), read(UNMAPPED, record).getRespawnPoint());
    }

    @Test
    public void testNoSpawnPoint() {
        assertNull(read(UNMAPPED, record()).getRespawnPoint());
    }

    @Test
    public void testLastDeath() {
        CompoundTag record = record();
        record.put("HasDiedBefore", (byte) 1);
        record.put("DeathDimension", 1);
        record.put("DeathPositionX", 275);
        record.put("DeathPositionY", 71);
        record.put("DeathPositionZ", -322);
        assertEquals(new ChunkerGlobalPosition(Dimension.NETHER, 275, 71, -322), read(UNMAPPED, record).getLastDeathLocation());

        // Never died
        assertNull(read(UNMAPPED, record()).getLastDeathLocation());
    }

    @Test
    public void testRidingAndShoulderParrotsAreNotRead() {
        CompoundTag record = record();
        record.put("RideID", -4294967251L);
        record.put("LeftShoulderRiderID", -4294967264L);
        ChunkerLevelPlayer player = read(UNMAPPED, record);

        // The player is read the same as without them: the vehicle and the parrot stay in the world
        assertEquals(read(UNMAPPED, record()).toString(), player.toString());
    }

    private static void assertItem(ChunkerItemStack item, ChunkerVanillaItemType type, int count) {
        assertNotNull(item);
        assertEquals(type, item.getIdentifier());
        assertEquals(count, item.get(ChunkerItemProperty.AMOUNT));
    }
}

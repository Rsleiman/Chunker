package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.convert;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for converting the data of living entities from Bedrock to Java.
 */
public class BedrockLivingEntityTests {
    private static final long OWNER = -4294967295L;

    private static CompoundTag health(float current, float max) {
        CompoundTag attribute = new CompoundTag();
        attribute.put("Name", "minecraft:health");
        attribute.put("Base", max);
        attribute.put("Current", current);
        attribute.put("Max", max);
        return attribute;
    }

    private static CompoundTag attribute(String name, float base) {
        CompoundTag attribute = new CompoundTag();
        attribute.put("Name", name);
        attribute.put("Base", base);
        attribute.put("Current", base);
        attribute.put("Max", 3.4028235e+38f);
        return attribute;
    }

    private static CompoundTag item(String name, int count) {
        CompoundTag item = new CompoundTag();
        item.put("Name", name);
        item.put("Count", (byte) count);
        item.put("Damage", (short) 0);
        item.put("WasPickedUp", (byte) 0);
        return item;
    }

    private static CompoundTag emptyItem() {
        return item("", 0);
    }

    private static CompoundTag slotted(CompoundTag item, int slot) {
        item.put("Slot", (byte) slot);
        return item;
    }

    private static CompoundTag mob(String identifier, long uniqueId) {
        CompoundTag tag = actor(identifier, uniqueId, 0.5f, 64f, 0.5f);
        tag.put("Armor", compounds(emptyItem(), emptyItem(), emptyItem(), emptyItem(), emptyItem()));
        tag.put("Mainhand", compounds(emptyItem()));
        tag.put("Offhand", compounds(emptyItem()));
        tag.put("IsBaby", (byte) 0);
        tag.put("IsTamed", (byte) 0);
        tag.put("OwnerNew", -1L);
        tag.put("Sitting", (byte) 0);
        tag.put("Saddled", (byte) 0);
        tag.put("Chested", (byte) 0);
        tag.put("Sheared", (byte) 0);
        tag.put("Variant", 0);
        tag.put("MarkVariant", 0);
        tag.put("Color", (byte) 0);
        tag.put("Color2", (byte) 0);
        tag.put("Persistent", (byte) 0);
        tag.put("canPickupItems", (byte) 0);
        tag.put("LeasherID", -1L);
        return tag;
    }

    private static CompoundTag properties(String... keyValues) {
        CompoundTag properties = new CompoundTag();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }

    @Test
    public void testTamedWolf() {
        CompoundTag bedrock = mob("minecraft:wolf", 10);
        bedrock.put("Attributes", compounds(health(37, 40), attribute("minecraft:movement", 0.3f)));
        bedrock.put("IsTamed", (byte) 1);
        bedrock.put("OwnerNew", OWNER);
        bedrock.put("Sitting", (byte) 1);
        bedrock.put("Color", (byte) 11);
        bedrock.put("Persistent", (byte) 1);
        bedrock.put("definitions", strings("+minecraft:wolf", "+minecraft:wolf_tame", "-minecraft:wolf_wild", "+minecraft:wolf_pale"));
        bedrock.put("properties", properties("minecraft:sound_variant", "big"));

        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:wolf", java.getString("id"));
        assertEquals(37f, java.getFloat("Health"));
        CompoundTag maxHealth = java.getList("attributes", CompoundTag.class).get(0);
        assertEquals("minecraft:max_health", maxHealth.getString("id"));
        assertEquals(40.0, maxHealth.getDouble("base"));
        assertEquals(1, java.getList("attributes", CompoundTag.class).size(), "Only horses keep their movement speed");
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(OWNER)), java.getIntArray("Owner"));
        assertEquals((byte) 1, java.getByte("Sitting"));
        assertEquals((byte) 11, java.getByte("CollarColor"));
        assertEquals("minecraft:pale", java.getString("variant"));
        assertEquals("minecraft:big", java.getString("sound_variant"));
        assertEquals((byte) 1, java.getByte("PersistenceRequired"));
        assertFalse(java.contains("equipment"), "Empty slots are not written");
        assertFalse(java.contains("Age"), "Adults without a breeding cooldown have no age");
    }

    @Test
    public void testCatVariantsAndCollar() {
        CompoundTag tuxedo = mob("minecraft:cat", 11);
        tuxedo.put("Variant", 1);
        tuxedo.put("properties", properties("minecraft:sound_variant", "default"));
        CompoundTag java = convert(tuxedo);
        assertEquals("minecraft:black", java.getString("variant"));
        assertEquals("minecraft:classic", java.getString("sound_variant"));
        assertFalse(java.contains("CollarColor"), "Untamed cats have no collar");
        assertFalse(java.contains("Owner"));

        CompoundTag black = mob("minecraft:cat", 12);
        black.put("Variant", 9);
        black.put("IsTamed", (byte) 1);
        black.put("OwnerNew", OWNER);
        black.put("Color", (byte) 14);
        java = convert(black);
        assertEquals("minecraft:all_black", java.getString("variant"));
        assertEquals((byte) 14, java.getByte("CollarColor"));
    }

    @Test
    public void testHorse() {
        CompoundTag bedrock = mob("minecraft:horse", 13);
        bedrock.put("Attributes", compounds(
                health(20, 26),
                attribute("minecraft:movement", 0.22616895f),
                attribute("minecraft:horse.jump_strength", 0.58685964f)
        ));
        bedrock.put("Variant", 3);
        bedrock.put("MarkVariant", 2);
        bedrock.put("IsTamed", (byte) 1);
        bedrock.put("OwnerNew", OWNER);
        bedrock.put("Temper", 26);
        bedrock.put("ChestItems", compounds(slotted(item("minecraft:saddle", 1), 0), slotted(item("minecraft:iron_horse_armor", 1), 1)));

        CompoundTag java = convert(bedrock);
        assertEquals(3 | 2 << 8, java.getInt("Variant"));
        assertEquals((byte) 1, java.getByte("Tame"));
        assertEquals(26, java.getInt("Temper"));
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(OWNER)), java.getIntArray("Owner"));
        ListTag<CompoundTag, Map<String, Tag<?>>> attributes = java.getList("attributes", CompoundTag.class);
        assertEquals(3, attributes.size());
        assertEquals("minecraft:movement_speed", attributes.get(1).getString("id"));
        assertEquals(0.22616895f, (float) attributes.get(1).getDouble("base"));
        assertEquals("minecraft:jump_strength", attributes.get(2).getString("id"));
        CompoundTag equipment = java.getCompound("equipment");
        assertEquals("minecraft:saddle", equipment.getCompound("saddle").getString("id"));
        assertEquals("minecraft:iron_horse_armor", equipment.getCompound("body").getString("id"));
        assertFalse(java.contains("Items"));
    }

    @Test
    public void testChestedDonkey() {
        CompoundTag bedrock = mob("minecraft:donkey", 14);
        bedrock.put("Chested", (byte) 1);
        bedrock.put("ChestItems", compounds(slotted(emptyItem(), 0), slotted(item("minecraft:bread", 5), 2), slotted(item("minecraft:coal", 9), 4)));

        CompoundTag java = convert(bedrock);
        assertEquals((byte) 1, java.getByte("ChestedHorse"));
        ListTag<CompoundTag, Map<String, Tag<?>>> items = java.getList("Items", CompoundTag.class);
        assertEquals(2, items.size());
        assertEquals("minecraft:bread", items.get(0).getString("id"));
        assertEquals((byte) 0, items.get(0).getByte("Slot"), "Bedrock slot 2 is the first chest slot");
        assertEquals((byte) 2, items.get(1).getByte("Slot"));
    }

    @Test
    public void testLlamas() {
        CompoundTag llama = mob("minecraft:llama", 15);
        llama.put("Variant", 2);
        llama.put("Strength", 4);
        CompoundTag java = convert(llama);
        assertEquals(2, java.getInt("Variant"));
        assertEquals(4, java.getInt("Strength"));
        assertEquals((byte) 0, java.getByte("ChestedHorse"));
        assertFalse(java.contains("DespawnDelay"));

        CompoundTag traderLlama = mob("minecraft:trader_llama", 16);
        java = convert(traderLlama);
        assertEquals("minecraft:trader_llama", java.getString("id"));
        assertEquals(48000, java.getInt("DespawnDelay"));
    }

    @Test
    public void testBabyZombieWithPickedUpHelmet() {
        CompoundTag bedrock = mob("minecraft:zombie", 17);
        bedrock.put("IsBaby", (byte) 1);
        bedrock.put("canPickupItems", (byte) 1);
        bedrock.put("definitions", strings("+minecraft:zombie", "+minecraft:zombie_baby", "+minecraft:can_break_doors"));
        CompoundTag helmet = item("minecraft:iron_helmet", 1);
        helmet.put("WasPickedUp", (byte) 1);
        bedrock.put("Armor", compounds(helmet, emptyItem(), emptyItem(), emptyItem(), emptyItem()));
        bedrock.put("Mainhand", compounds(item("minecraft:iron_sword", 1)));

        CompoundTag java = convert(bedrock);
        assertEquals((byte) 1, java.getByte("IsBaby"));
        assertFalse(java.contains("Age"), "Zombies don't age in Java");
        assertEquals((byte) 1, java.getByte("CanBreakDoors"));
        assertEquals((byte) 1, java.getByte("CanPickUpLoot"));
        CompoundTag equipment = java.getCompound("equipment");
        assertEquals("minecraft:iron_helmet", equipment.getCompound("head").getString("id"));
        assertEquals("minecraft:iron_sword", equipment.getCompound("mainhand").getString("id"));
        assertEquals(2.0f, java.getCompound("drop_chances").getFloat("head"));
        assertFalse(java.getCompound("drop_chances").contains("mainhand"));
    }

    @Test
    public void testSlotDropChances() {
        CompoundTag bedrock = mob("minecraft:piglin", 18);
        CompoundTag dropChance = new CompoundTag();
        dropChance.put("Slot", "mainhand");
        dropChance.put("DropChance", 0f);
        bedrock.put("SlotDropChances", compounds(dropChance));
        bedrock.put("Mainhand", compounds(item("minecraft:golden_sword", 1)));
        CompoundTag java = convert(bedrock);
        assertEquals(0f, java.getCompound("drop_chances").getFloat("mainhand"));
    }

    @Test
    public void testPiglin() {
        CompoundTag hunter = mob("minecraft:piglin", 19);
        hunter.put("definitions", strings("+minecraft:piglin", "+piglin_adult", "+hunter"));
        hunter.put("ChestItems", compounds(slotted(item("minecraft:gold_ingot", 3), 0), slotted(emptyItem(), 1)));
        CompoundTag java = convert(hunter);
        assertEquals((byte) 0, java.getByte("CannotHunt"));
        assertEquals((byte) 0, java.getByte("IsBaby"));
        ListTag<CompoundTag, Map<String, Tag<?>>> inventory = java.getList("Inventory", CompoundTag.class);
        assertEquals(1, inventory.size());
        assertEquals("minecraft:gold_ingot", inventory.get(0).getString("id"));
        assertFalse(inventory.get(0).contains("Slot"), "Mob inventories are plain lists in Java");

        CompoundTag baby = mob("minecraft:piglin", 20);
        baby.put("IsBaby", (byte) 1);
        baby.put("definitions", strings("+minecraft:piglin", "+piglin_baby", "+not_hunter"));
        java = convert(baby);
        assertEquals((byte) 1, java.getByte("CannotHunt"));
        assertEquals((byte) 1, java.getByte("IsBaby"));
    }

    @Test
    public void testFarmAnimals() {
        CompoundTag calf = mob("minecraft:cow", 21);
        calf.put("IsBaby", (byte) 1);
        calf.put("Age", -12000);
        calf.put("properties", properties("minecraft:climate_variant", "cold", "minecraft:sound_variant", "moody"));
        CompoundTag java = convert(calf);
        assertEquals(-12000, java.getInt("Age"));
        assertEquals("minecraft:cold", java.getString("variant"));
        assertEquals("minecraft:moody", java.getString("sound_variant"));

        CompoundTag piglet = mob("minecraft:pig", 22);
        piglet.put("IsBaby", (byte) 1);
        java = convert(piglet);
        assertEquals(-24000, java.getInt("Age"), "Babies without an age start at -24000");

        CompoundTag pig = mob("minecraft:pig", 23);
        pig.put("Saddled", (byte) 1);
        pig.put("BreedCooldown", 4800);
        pig.put("InLove", 400);
        pig.put("LoveCause", OWNER);
        java = convert(pig);
        assertEquals(4800, java.getInt("Age"));
        assertEquals(400, java.getInt("InLove"));
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(OWNER)), java.getIntArray("LoveCause"));
        assertEquals("minecraft:saddle", java.getCompound("equipment").getCompound("saddle").getString("id"));

        CompoundTag sheep = mob("minecraft:sheep", 24);
        sheep.put("Color", (byte) 14);
        sheep.put("Sheared", (byte) 1);
        java = convert(sheep);
        assertEquals((byte) 14, java.getByte("Color"));
        assertEquals((byte) 1, java.getByte("Sheared"));

        CompoundTag mooshroom = mob("minecraft:mooshroom", 25);
        mooshroom.put("Variant", 1);
        assertEquals("brown", convert(mooshroom).getString("Type"));
    }

    @Test
    public void testStriderSaddle() {
        CompoundTag bedrock = mob("minecraft:strider", 26);
        bedrock.put("definitions", strings("+minecraft:strider", "+minecraft:strider_saddled"));
        assertEquals("minecraft:saddle", convert(bedrock).getCompound("equipment").getCompound("saddle").getString("id"));
    }

    @Test
    public void testBee() {
        CompoundTag bedrock = mob("minecraft:bee", 27);
        bedrock.put("HomePos", floats(100f, 65f, -20f));
        bedrock.put("properties", new CompoundTag());
        bedrock.getCompound("properties").put("minecraft:has_nectar", (byte) 1);
        CompoundTag java = convert(bedrock);
        assertArrayEquals(new int[]{100, 65, -20}, java.getIntArray("hive_pos"));
        assertEquals((byte) 1, java.getByte("HasNectar"));
    }

    @Test
    public void testTurtleHome() {
        CompoundTag bedrock = mob("minecraft:turtle", 28);
        bedrock.put("HomePos", floats(-5.5f, 62f, 7f));
        assertArrayEquals(new int[]{-6, 62, 7}, convert(bedrock).getIntArray("home_pos"));
    }

    @Test
    public void testIronGolem() {
        CompoundTag village = mob("minecraft:iron_golem", 29);
        village.put("definitions", strings("+minecraft:iron_golem", "+minecraft:village_created"));
        assertEquals((byte) 0, convert(village).getByte("PlayerCreated"));

        CompoundTag built = mob("minecraft:iron_golem", 30);
        built.put("definitions", strings("+minecraft:iron_golem", "+minecraft:player_created"));
        assertEquals((byte) 1, convert(built).getByte("PlayerCreated"));
    }

    @Test
    public void testCubeSize() {
        CompoundTag sulfurCube = mob("minecraft:sulfur_cube", 31);
        sulfurCube.put("Size", (byte) 2);
        assertEquals(1, convert(sulfurCube).getInt("Size"), "Java stores the size minus one");
    }

    @Test
    public void testTropicalFish() {
        CompoundTag bedrock = mob("minecraft:tropicalfish", 32);
        bedrock.put("Variant", 1);
        bedrock.put("MarkVariant", 2);
        bedrock.put("Color", (byte) 3);
        bedrock.put("Color2", (byte) 4);
        assertEquals(1 | 2 << 8 | 3 << 16 | 4 << 24, convert(bedrock).getInt("Variant"));
    }

    @Test
    public void testAllay() {
        CompoundTag bedrock = mob("minecraft:allay", 33);
        bedrock.put("AllayDuplicationCooldown", 6000L);
        bedrock.put("Mainhand", compounds(item("minecraft:amethyst_shard", 1)));
        CompoundTag java = convert(bedrock);
        assertEquals(6000L, java.getLong("DuplicationCooldown"));
        assertEquals("minecraft:amethyst_shard", java.getCompound("equipment").getCompound("mainhand").getString("id"));
    }

    @Test
    public void testEffects() {
        CompoundTag bedrock = mob("minecraft:skeleton", 34);
        CompoundTag effect = new CompoundTag();
        effect.put("Id", (byte) 1);
        effect.put("Amplifier", (byte) 1);
        effect.put("Duration", 200);
        effect.put("Ambient", (byte) 0);
        effect.put("ShowParticles", (byte) 1);
        bedrock.put("ActiveEffects", compounds(effect));
        CompoundTag java = convert(bedrock);
        CompoundTag activeEffect = java.getList("active_effects", CompoundTag.class).get(0);
        assertEquals("minecraft:speed", activeEffect.getString("id"));
        assertEquals((byte) 1, activeEffect.getByte("amplifier"));
        assertEquals(200, activeEffect.getInt("duration"));
        assertFalse(java.contains("Age"));
        assertFalse(java.contains("IsBaby"));
    }
}

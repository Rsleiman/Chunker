package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.junit.jupiter.api.Test;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.convert;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the type specific mob data (variants, states and timers) which Java saves under its own keys.
 */
public class BedrockMobDataTests {
    private static CompoundTag item(String name, int count) {
        CompoundTag item = new CompoundTag();
        item.put("Name", name);
        item.put("Count", (byte) count);
        item.put("Damage", (short) 0);
        item.put("WasPickedUp", (byte) 0);
        return item;
    }

    private static CompoundTag mob(String identifier, long uniqueId, String... definitions) {
        CompoundTag tag = actor(identifier, uniqueId, 0.5f, 64f, 0.5f);
        tag.put("definitions", strings(definitions));
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
    public void testAxolotlVariants() {
        // Bedrock numbers cyan 1 and wild 3, Java the other way round
        CompoundTag wild = mob("minecraft:axolotl", 1, "+minecraft:axolotl", "+axolotl_adult", "+axolotl_wild");
        wild.put("Variant", 3);
        assertEquals(1, convert(wild).getInt("Variant"));

        CompoundTag cyan = mob("minecraft:axolotl", 2, "+minecraft:axolotl", "+axolotl_adult", "+axolotl_cyan");
        cyan.put("Variant", 1);
        assertEquals(3, convert(cyan).getInt("Variant"));

        // Lucy (0) has no Variant in the 26.60 format, the group still names it
        CompoundTag lucy = mob("minecraft:axolotl", 3, "+minecraft:axolotl", "+axolotl_adult", "+axolotl_lucy");
        assertEquals(0, convert(lucy).getInt("Variant"));

        CompoundTag gold = mob("minecraft:axolotl", 4, "+minecraft:axolotl", "+axolotl_gold");
        gold.put("Variant", 2);
        assertEquals(2, convert(gold).getInt("Variant"));
    }

    @Test
    public void testParrotRabbitAndFrogVariants() {
        CompoundTag parrot = mob("minecraft:parrot", 5, "+minecraft:parrot_adult", "+minecraft:parrot_cyan");
        parrot.put("Variant", 3);
        assertEquals(3, convert(parrot).getInt("Variant"));

        CompoundTag rabbit = mob("minecraft:rabbit", 6, "+coat_desert");
        rabbit.put("Variant", 4);
        CompoundTag javaRabbit = convert(rabbit);
        assertEquals(4, javaRabbit.getInt("RabbitType"));
        assertFalse(javaRabbit.contains("Variant"));

        CompoundTag frog = mob("minecraft:frog", 7, "+cold_frog");
        frog.put("Variant", 1);
        assertEquals("minecraft:cold", convert(frog).getString("variant"));

        CompoundTag nautilus = mob("minecraft:zombie_nautilus", 8);
        nautilus.put("properties", properties("minecraft:variant", "coral"));
        assertEquals("minecraft:warm", convert(nautilus).getString("variant"));
    }

    @Test
    public void testSalmonSizeAndShulkerColor() {
        assertEquals("large", convert(mob("minecraft:salmon", 9, "+minecraft:salmon", "+scale_large")).getString("type"));
        assertEquals("medium", convert(mob("minecraft:salmon", 10, "+minecraft:salmon", "+scale_normal")).getString("type"));

        // Bedrock counts from black (0) to white (15), 16 is undyed
        CompoundTag red = mob("minecraft:shulker", 11);
        red.put("Variant", 1);
        assertEquals((byte) 14, convert(red).getByte("Color"));
        CompoundTag undyed = mob("minecraft:shulker", 12);
        undyed.put("Variant", 16);
        assertFalse(convert(undyed).contains("Color"));
    }

    @Test
    public void testFlags() {
        assertEquals((byte) 1, convert(mob("minecraft:hoglin", 13, "+minecraft:hoglin_adult", "+unhuntable_adult")).getByte("CannotBeHunted"));
        assertEquals((byte) 0, convert(mob("minecraft:hoglin", 14, "+minecraft:hoglin_adult", "+huntable_adult")).getByte("CannotBeHunted"));
        assertEquals((byte) 1, convert(mob("minecraft:creeper", 15, "+minecraft:charged_creeper")).getByte("powered"));
        assertEquals((byte) 1, convert(mob("minecraft:bogged", 16, "+minecraft:bogged_sheared")).getByte("sheared"));
        assertEquals((byte) 0, convert(mob("minecraft:snow_golem", 17, "+minecraft:snowman_sheared")).getByte("Pumpkin"));
        assertEquals((byte) 1, convert(mob("minecraft:snow_golem", 18)).getByte("Pumpkin"));
        assertEquals((byte) 1, convert(mob("minecraft:ocelot", 19, "+minecraft:ocelot_trusting")).getByte("Trusting"));
        assertEquals((byte) 1, convert(mob("minecraft:turtle", 20, "+minecraft:pregnant")).getByte("has_egg"));
        assertEquals((byte) 1, convert(mob("minecraft:bee", 21, "+countdown_to_perish")).getByte("HasStung"));
        assertEquals((byte) 1, convert(mob("minecraft:skeleton_horse", 22, "+minecraft:skeleton_trap")).getByte("SkeletonTrap"));
        assertFalse(convert(mob("minecraft:zombie", 23)).contains("powered"), "Flags are only written for their own mob");
    }

    @Test
    public void testGoatHorns() {
        CompoundTag goat = mob("minecraft:goat", 24, "+goat_screamer");
        goat.put("Variant", 1);
        goat.put("GoatHornCount", 1);
        CompoundTag java = convert(goat);
        assertEquals((byte) 1, java.getByte("IsScreamingGoat"));
        assertEquals((byte) 1, java.getByte("HasLeftHorn"));
        assertEquals((byte) 0, java.getByte("HasRightHorn"));

        java = convert(mob("minecraft:goat", 25));
        assertEquals((byte) 0, java.getByte("IsScreamingGoat"));
        assertEquals((byte) 1, java.getByte("HasRightHorn"), "A goat keeps both horns by default");
    }

    @Test
    public void testSittingFoxAndCamel() {
        CompoundTag fox = mob("minecraft:fox", 26);
        fox.put("Sitting", (byte) 1);
        assertEquals((byte) 1, convert(fox).getByte("Sitting"));

        assertEquals(-1L, convert(mob("minecraft:camel", 27, "+minecraft:camel_sitting")).getLong("LastPoseTick"));
        assertFalse(convert(mob("minecraft:camel", 28)).contains("LastPoseTick"));
    }

    @Test
    public void testAgeLockedBaby() {
        CompoundTag baby = mob("minecraft:cow", 29);
        baby.put("IsBaby", (byte) 1);
        baby.put("Age", -12000);
        baby.put("GrowthPaused", (byte) 1);
        CompoundTag java = convert(baby);
        assertEquals(-12000, java.getInt("Age"));
        assertEquals((byte) 1, java.getByte("AgeLocked"));
    }

    @Test
    public void testEggAndScuteTimers() {
        CompoundTag entries = new CompoundTag();
        entries.put("SpawnTimer", 6612);
        entries.put("StopSpawning", (byte) 0);
        CompoundTag warmEntry = new CompoundTag();
        warmEntry.put("SpawnTimer", 7223);
        warmEntry.put("StopSpawning", (byte) 0);

        CompoundTag chicken = mob("minecraft:chicken", 30);
        chicken.put("entries", compounds(entries, warmEntry));
        chicken.put("properties", properties("minecraft:climate_variant", "warm"));
        assertEquals(7223, convert(chicken).getInt("EggLayTime"), "The timer of the chicken's own egg counts");

        CompoundTag armadillo = mob("minecraft:armadillo", 31);
        armadillo.put("entries", compounds(entries.clone()));
        assertEquals(6612, convert(armadillo).getInt("scute_time"));
    }

    @Test
    public void testCollarDefaultsToWhite() {
        // The 26.60 format leaves out a Color of 0
        CompoundTag wolf = mob("minecraft:wolf", 32, "+minecraft:wolf_tame");
        wolf.put("IsTamed", (byte) 1);
        wolf.put("OwnerNew", -4294967295L);
        assertEquals((byte) 0, convert(wolf).getByte("CollarColor"));
    }

    @Test
    public void testAngryWolfSound() {
        CompoundTag wolf = mob("minecraft:wolf", 33);
        wolf.put("properties", properties("minecraft:sound_variant", "mad"));
        assertEquals("minecraft:angry", convert(wolf).getString("sound_variant"));
    }

    @Test
    public void testPatrolCaptain() {
        CompoundTag banner = item("minecraft:banner", 1);
        CompoundTag bannerTag = new CompoundTag();
        bannerTag.put("Type", 1);
        banner.put("tag", bannerTag);

        CompoundTag captain = mob("minecraft:pillager", 34, "+minecraft:pillager", "+minecraft:illager_squad_captain");
        captain.put("IsIllagerCaptain", (byte) 1);
        captain.put("Armor", compounds(item("", 0), banner, item("", 0), item("", 0), item("", 0)));
        CompoundTag java = convert(captain);
        assertEquals((byte) 1, java.getByte("PatrolLeader"));

        CompoundTag head = java.getCompound("equipment").getCompound("head");
        assertNotNull(head, "Java captains wear the banner on the head");
        assertFalse(java.getCompound("equipment").contains("chest"));
        assertEquals("minecraft:white_banner", head.getString("id"));
        CompoundTag components = head.getCompound("components");
        assertEquals("uncommon", components.getString("minecraft:rarity"));
        assertEquals("block.minecraft.ominous_banner", components.getCompound("minecraft:item_name").getString("translate"));
        assertEquals(8, components.getList("minecraft:banner_patterns", CompoundTag.class).size());
        assertEquals("minecraft:rhombus", components.getList("minecraft:banner_patterns", CompoundTag.class).get(0).getString("pattern"));
        assertFalse(components.contains("minecraft:base_color"));
        assertEquals(2.0f, java.getCompound("drop_chances").getFloat("head"));
    }

    @Test
    public void testEndermanCarriedBlock() {
        CompoundTag block = new CompoundTag();
        block.put("name", "minecraft:warped_nylium");
        block.put("states", new CompoundTag());
        block.put("version", 18168865);
        CompoundTag held = item("minecraft:warped_nylium", 1);
        held.put("Block", block);

        CompoundTag enderman = mob("minecraft:enderman", 35);
        enderman.put("Mainhand", compounds(held));
        CompoundTag java = convert(enderman);
        assertEquals("minecraft:warped_nylium", java.getCompound("carriedBlockState").getString("id"));
        assertFalse(java.contains("equipment"), "The block isn't held as an item");
    }

    @Test
    public void testPiglinBruteHome() {
        CompoundTag brute = mob("minecraft:piglin_brute", 36);
        brute.put("HomePos", floats(239.5f, 69f, -752.5f));
        brute.put("HomeDimensionId", 1);
        CompoundTag home = convert(brute).getCompound("Brain").getCompound("memories").getCompound("minecraft:home").getCompound("value");
        assertEquals("minecraft:the_nether", home.getString("dimension"));
        assertArrayEquals(new int[]{239, 69, -753}, home.getIntArray("pos"));
    }

    @Test
    public void testFallingBlockAndThrower() {
        CompoundTag block = new CompoundTag();
        block.put("name", "minecraft:gravel");
        block.put("states", new CompoundTag());
        block.put("version", 18168865);
        CompoundTag falling = actor("minecraft:falling_block", 37, 0.5f, 64f, 0.5f);
        falling.put("FallingBlock", block);
        falling.put("Time", (byte) 5);
        CompoundTag java = convert(falling);
        assertEquals("minecraft:falling_block", java.getString("id"));
        assertEquals("minecraft:gravel", java.getCompound("BlockState").getString("id"));
        assertEquals(5, java.getInt("Time"));

        CompoundTag dropped = actor("minecraft:item", 38, 0.5f, 64f, 0.5f);
        dropped.put("Item", item("minecraft:stick", 1));
        dropped.put("OwnerID", -4294967295L);
        assertArrayEquals(JavaEntityNBT.toIntArray(BedrockEntityUUIDs.toUUID(-4294967295L)), convert(dropped).getIntArray("Thrower"));
    }
}

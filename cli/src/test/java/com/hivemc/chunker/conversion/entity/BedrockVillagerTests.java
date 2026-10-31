package com.hivemc.chunker.conversion.entity;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.bedrock.resolver.MockConverter;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockDataVersion;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockEncoders;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers.JavaEntityNBT;
import com.hivemc.chunker.conversion.encoding.bedrock.util.BedrockVillageParser;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for converting villagers, zombie villagers and wandering traders, including their village memories.
 */
public class BedrockVillagerTests {
    private static final long VILLAGER_ID = -17179864762L;

    private static CompoundTag item(String name, int count) {
        CompoundTag item = new CompoundTag();
        item.put("Name", name);
        item.put("Count", (byte) count);
        item.put("Damage", (short) 0);
        item.put("WasPickedUp", (byte) 0);
        return item;
    }

    private static CompoundTag recipe(int tier, String buy, int buyCountA, int adjustedCount, String sell, int sellCount, int uses) {
        CompoundTag recipe = new CompoundTag();
        recipe.put("buyA", item(buy, adjustedCount));
        recipe.put("buyCountA", buyCountA);
        recipe.put("buyCountB", 0);
        recipe.put("sell", item(sell, sellCount));
        recipe.put("tier", tier);
        recipe.put("uses", uses);
        recipe.put("maxUses", 16);
        recipe.put("rewardExp", (byte) 1);
        recipe.put("traderExp", 2);
        recipe.put("priceMultiplierA", 0.05f);
        recipe.put("priceMultiplierB", 0f);
        recipe.put("demand", 2);
        return recipe;
    }

    private static CompoundTag villager(long uniqueId, int profession, int type, int tradeTier, int tradeExperience) {
        CompoundTag tag = actor("minecraft:villager_v2", uniqueId, 350.5f, 75f, -200.5f);
        tag.put("Variant", profession);
        tag.put("MarkVariant", type);
        tag.put("TradeTier", tradeTier);
        tag.put("TradeExperience", tradeExperience);
        tag.put("IsBaby", (byte) 0);
        tag.put("definitions", strings("+minecraft:villager_v2", "+unskilled", "+swamp_villager", "+farmer", "+adult"));
        CompoundTag offers = new CompoundTag();
        offers.put("Recipes", compounds(
                recipe(0, "minecraft:wheat", 20, 22, "minecraft:emerald", 1, 3),
                recipe(0, "minecraft:emerald", 1, 1, "minecraft:bread", 6, 0),
                recipe(1, "minecraft:pumpkin", 6, 6, "minecraft:emerald", 1, 0),
                recipe(2, "minecraft:melon", 4, 4, "minecraft:emerald", 1, 0)
        ));
        tag.put("Offers", offers);
        CompoundTag wheat = item("minecraft:wheat", 3);
        wheat.put("Slot", (byte) 0);
        tag.put("ChestItems", compounds(wheat));
        return tag;
    }

    @Test
    public void testVillagerDataAndUnlockedTrades() {
        CompoundTag java = convert(villager(40, 1, 5, 1, 23));
        assertEquals("minecraft:villager", java.getString("id"));
        CompoundTag villagerData = java.getCompound("VillagerData");
        assertEquals("minecraft:farmer", villagerData.getString("profession"));
        assertEquals("minecraft:swamp", villagerData.getString("type"));
        assertEquals(2, villagerData.getInt("level"), "Level is TradeTier + 1");
        assertEquals(23, java.getInt("Xp"));

        ListTag<CompoundTag, Map<String, Tag<?>>> recipes = java.getCompound("Offers").getList("Recipes", CompoundTag.class);
        assertEquals(3, recipes.size(), "Only the offers of unlocked tiers are kept");
        CompoundTag first = recipes.get(0);
        assertEquals("minecraft:wheat", first.getCompound("buy").getString("id"));
        assertEquals(20, first.getCompound("buy").getInt("count"), "The base price is kept, not the demand adjusted one");
        assertEquals("minecraft:emerald", first.getCompound("sell").getString("id"));
        assertEquals(3, first.getInt("uses"));
        assertEquals(16, first.getInt("maxUses"));
        assertEquals(2, first.getInt("xp"));
        assertEquals(2, first.getInt("demand"));
        assertEquals(0.05f, first.getFloat("priceMultiplier"));
        assertEquals((byte) 1, first.getByte("rewardExp"));
        assertFalse(first.contains("buyB"));
        assertEquals(6, recipes.get(1).getCompound("sell").getInt("count"));

        ListTag<CompoundTag, Map<String, Tag<?>>> inventory = java.getList("Inventory", CompoundTag.class);
        assertEquals("minecraft:wheat", inventory.get(0).getString("id"));
        assertEquals(3, inventory.get(0).getInt("count"));
        assertFalse(java.contains("Brain"), "No memories without village records");
    }

    @Test
    public void testNitwitAndUnemployed() {
        assertEquals("minecraft:nitwit", convert(villager(41, 14, 0, 0, 0)).getCompound("VillagerData").getString("profession"));
        CompoundTag unemployed = convert(villager(42, 0, 4, 0, 0));
        assertEquals("minecraft:none", unemployed.getCompound("VillagerData").getString("profession"));
        assertEquals("minecraft:snow", unemployed.getCompound("VillagerData").getString("type"));
        assertEquals(1, unemployed.getCompound("VillagerData").getInt("level"));
    }

    @Test
    public void testZombieVillagerProfessionFromGroups() {
        CompoundTag bedrock = actor("minecraft:zombie_villager_v2", 43, 0, 0, 0);
        bedrock.put("Variant", 0);
        bedrock.put("MarkVariant", 6);
        bedrock.put("IsBaby", (byte) 0);
        bedrock.put("definitions", strings("+minecraft:zombie_villager_v2", "+adult", "+unskilled", "+librarian", "+taiga_villager", "+can_break_doors"));
        CompoundTag java = convert(bedrock);
        assertEquals("minecraft:zombie_villager", java.getString("id"));
        assertEquals("minecraft:librarian", java.getCompound("VillagerData").getString("profession"));
        assertEquals("minecraft:taiga", java.getCompound("VillagerData").getString("type"));
        assertEquals((byte) 1, java.getByte("CanBreakDoors"));
        assertEquals((byte) 0, java.getByte("IsBaby"));
        assertFalse(java.contains("Offers"));
        assertFalse(java.contains("ConversionTime"), "Not converting back to a villager");
    }

    @Test
    public void testWanderingTraderKeepsEveryOffer() {
        CompoundTag bedrock = actor("minecraft:wandering_trader", 44, 0, 0, 0);
        CompoundTag offers = new CompoundTag();
        offers.put("Recipes", compounds(
                recipe(0, "minecraft:emerald", 1, 1, "minecraft:name_tag", 1, 0),
                recipe(1, "minecraft:emerald", 5, 5, "minecraft:blue_ice", 1, 0)
        ));
        bedrock.put("Offers", offers);
        CompoundTag java = convert(bedrock);
        assertEquals(2, java.getCompound("Offers").getList("Recipes", CompoundTag.class).size());
        assertFalse(java.contains("VillagerData"));
        assertEquals(48000, java.getInt("DespawnDelay"));
    }

    @Test
    public void testVillageMemoriesBecomeBrainMemories() throws Exception {
        // A village POI record as BDS 1.26.52 saves it
        CompoundTag bed = poi(0, "villager", 396, 100, -191, false);
        CompoundTag bell = poi(1, "villager", 346, 67, -197, false);
        CompoundTag workstation = poi(2, "farmer", 356, 75, -201, false);
        CompoundTag villagerPOI = new CompoundTag();
        villagerPOI.put("VillagerID", VILLAGER_ID);
        villagerPOI.put("instances", compounds(bed, bell, workstation));
        CompoundTag skippedPOI = new CompoundTag();
        skippedPOI.put("VillagerID", 7L);
        skippedPOI.put("instances", compounds(poi(0, "", 0, 0, 0, true)));
        CompoundTag record = new CompoundTag();
        record.put("POI", compounds(villagerPOI, skippedPOI));

        String key = "VILLAGE_Overworld_093a6b34-a4e9-469e-9401-e7aba25e23f7_POI";
        assertEquals(Dimension.OVERWORLD, BedrockVillageParser.getDimension(key));
        assertNull(BedrockVillageParser.getDimension("VILLAGE_Overworld_093a6b34-a4e9-469e-9401-e7aba25e23f7_INFO"));
        Map<Long, Map<ChunkerVillagerMemory, ChunkerGlobalPosition>> memories = new HashMap<>();
        BedrockVillageParser.parsePOI(Tag.writeBedrockNBT(record), Dimension.OVERWORLD, memories);
        assertEquals(1, memories.size(), "Skipped slots are ignored");
        assertEquals(new ChunkerGlobalPosition(Dimension.OVERWORLD, 356, 75, -201), memories.get(VILLAGER_ID).get(ChunkerVillagerMemory.JOB_SITE));

        // Read the villager with resolvers which know the village records
        BedrockResolvers resolvers = BedrockEncoders.getNearestEncoder(BedrockDataVersion.V1_26_50)
                .readerConstructor()
                .construct(null, BedrockDataVersion.V1_26_50.getVersion(), CONVERTER)
                .buildResolvers(CONVERTER)
                .villagerMemories(memories::get)
                .build();
        Entity entity = resolvers.entityResolver().to(villager(VILLAGER_ID, 1, 5, 0, 0)).orElseThrow();
        CompoundTag java = writeJava(entity);
        CompoundTag brainMemories = java.getCompound("Brain").getCompound("memories");
        for (String memory : List.of("minecraft:home", "minecraft:meeting_point", "minecraft:job_site")) {
            assertEquals("minecraft:overworld", brainMemories.getCompound(memory).getCompound("value").getString("dimension"), memory);
        }
        assertArrayEquals(new int[]{356, 75, -201}, brainMemories.getCompound("minecraft:job_site").getCompound("value").getIntArray("pos"));
        assertArrayEquals(new int[]{396, 100, -191}, brainMemories.getCompound("minecraft:home").getCompound("value").getIntArray("pos"));

        // Reading the Java villager back gives the same memories
        assertEquals(entity, JAVA.entityResolver().to(java).orElseThrow());
    }

    @Test
    public void testCuredDiscountBecomesGossipForEveryPlayer() {
        // A cured villager as BDS 1.26.61 saves it, in a world with a mapped and an unmapped player
        long mapped = -4294967295L;
        long unmapped = -17179869183L;
        UUID account = UUID.fromString("00000000-0000-4000-8000-000000000001");
        WorldConverter converter = new MockConverter(new ChunkerLevel(null, null, Collections.emptyList(), null, Collections.emptyList()));
        converter.setBedrockPlayerUUIDs(Map.of(mapped, account));
        BedrockResolvers resolvers = BedrockEncoders.getNearestEncoder(BedrockDataVersion.V1_26_50)
                .readerConstructor()
                .construct(null, BedrockDataVersion.V1_26_50.getVersion(), converter)
                .buildResolvers(converter)
                .playerIds(() -> List.of(mapped, unmapped))
                .build();
        CompoundTag bedrock = villager(45, 1, 0, 4, 0);
        bedrock.put("HighTierCuredDiscount", -20);
        bedrock.put("LowTierCuredDiscount", -25);
        bedrock.put("NearbyCuredDiscount", 0);
        Entity entity = resolvers.entityResolver().to(bedrock).orElseThrow();
        CompoundTag java = writeJava(entity);

        Map<String, Integer> gossips = new HashMap<>();
        for (CompoundTag gossip : java.getList("Gossips", CompoundTag.class)) {
            gossips.put(JavaEntityNBT.fromIntArray(gossip.getIntArray("Target")) + " " + gossip.getString("Type"), gossip.getInt("Value"));
        }
        UUID derived = BedrockEntityUUIDs.toUUID(unmapped);
        assertEquals(Map.of(
                account + " major_positive", 20,
                account + " minor_positive", 25,
                derived + " major_positive", 20,
                derived + " minor_positive", 25
        ), gossips, "Java's gossip for curing, the same values Bedrock keeps for every player");
        assertEquals(entity, JAVA.entityResolver().to(java).orElseThrow());

        // A villager which was never cured has no gossip
        assertFalse(resolvers.entityResolver().to(villager(46, 1, 0, 4, 0)).map(EntityTestResolvers::writeJava).orElseThrow().contains("Gossips"));
    }

    private static CompoundTag poi(int type, String name, int x, int y, int z, boolean skip) {
        CompoundTag instance = new CompoundTag();
        instance.put("Type", type);
        instance.put("Name", name);
        instance.put("X", x);
        instance.put("Y", y);
        instance.put("Z", z);
        instance.put("Skip", skip ? (byte) 1 : (byte) 0);
        instance.put("Capacity", 1L);
        instance.put("OwnerCount", 1L);
        return instance;
    }
}

package com.hivemc.chunker.conversion.bedrock.resolver.blockentity;

import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.blockentity.type.BedrockBrushableBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BannerBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.DecoratedPotBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.hivemc.chunker.conversion.entity.BedrockActors.*;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.BEDROCK;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.JAVA;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for block entity and item data which stock Chunker drops or writes wrongly from Bedrock to Java 26.3.
 */
public class BedrockBlockEntityDataTests {
    private static CompoundTag blockEntity(String id) {
        CompoundTag tag = new CompoundTag();
        tag.put("id", id);
        tag.put("x", 1);
        tag.put("y", 2);
        tag.put("z", 3);
        return tag;
    }

    private static CompoundTag item(String name, int count) {
        CompoundTag item = new CompoundTag();
        item.put("Name", name);
        item.put("Count", (byte) count);
        item.put("Damage", (short) 0);
        item.put("WasPickedUp", (byte) 0);
        return item;
    }

    private static CompoundTag convert(CompoundTag bedrock) {
        BlockEntity blockEntity = BEDROCK.blockEntityResolver().to(bedrock).orElseThrow();

        // Bedrock keeps the block type of suspicious sand and gravel until the column is processed
        if (blockEntity instanceof BedrockBrushableBlockEntity brushable) {
            blockEntity = brushable.toChunker();
        }
        return JAVA.blockEntityResolver().from(blockEntity).orElseThrow();
    }

    private static CompoundTag convertItem(CompoundTag bedrock) {
        ChunkerItemStack item = BEDROCK.readItem(bedrock);
        return JAVA.writeItem(item).orElseThrow();
    }

    @Test
    public void testArchaeologyLootTables() {
        CompoundTag sand = blockEntity("BrushableBlock");
        sand.put("LootTable", "loot_tables/entities/trail_ruins_brushable_block_rare.json");
        assertEquals("minecraft:archaeology/trail_ruins_rare", convert(sand).getString("LootTable"));

        CompoundTag gravel = blockEntity("BrushableBlock");
        gravel.put("LootTable", "loot_tables/entities/cold_ocean_ruins_brushable_block.json");
        assertEquals("minecraft:archaeology/ocean_ruin_cold", convert(gravel).getString("LootTable"));
    }

    @Test
    public void testDecoratedPotKeepsLootTableAndSherds() {
        CompoundTag pot = blockEntity("DecoratedPot");
        pot.put("LootTable", "loot_tables/pots/trial_chambers/corridor.json");
        pot.put("LootTableSeed", 0);
        pot.put("item", item("minecraft:air", 0));
        CompoundTag java = convert(pot);
        assertEquals("minecraft:pots/trial_chambers/corridor", java.getString("LootTable"));
        assertFalse(java.contains("item"), "An empty item isn't written");

        // A pot with fewer than four sherds keeps its item
        CompoundTag sherded = blockEntity("DecoratedPot");
        sherded.put("sherds", new ListTag<>(List.of(new StringTag("minecraft:brick"), new StringTag("minecraft:brick"))));
        sherded.put("item", item("minecraft:diamond", 3));
        DecoratedPotBlockEntity read = (DecoratedPotBlockEntity) BEDROCK.blockEntityResolver().to(sherded).orElseThrow();
        assertNotNull(read.getItem());
        assertEquals(3, JAVA.blockEntityResolver().from(read).orElseThrow().getCompound("item").getInt("count"));
    }

    @Test
    public void testOminousBannerBlock() {
        CompoundTag banner = blockEntity("Banner");
        banner.put("Base", 0);
        banner.put("Type", 1);
        BannerBlockEntity read = (BannerBlockEntity) BEDROCK.blockEntityResolver().to(banner).orElseThrow();
        assertTrue(read.isOminous());
        assertEquals(BannerBlockEntity.OMINOUS_PATTERNS, read.getPatterns());

        CompoundTag java = JAVA.blockEntityResolver().from(read).orElseThrow();
        assertEquals(8, java.getList("patterns", CompoundTag.class).size());
        CompoundTag components = java.getCompound("components");
        assertEquals("block.minecraft.ominous_banner", components.getCompound("minecraft:item_name").getString("translate"));
        assertEquals("uncommon", components.getString("minecraft:rarity"));
        assertEquals(List.of("minecraft:banner_patterns"),
                components.getCompound("minecraft:tooltip_display").getListValues("hidden_components", StringTag.class));

        // Java reads it back as ominous
        assertTrue(((BannerBlockEntity) JAVA.blockEntityResolver().to(java).orElseThrow()).isOminous());
    }

    @Test
    public void testFurnaceAndCampfireTotalTimes() {
        CompoundTag furnace = blockEntity("Furnace");
        furnace.put("BurnTime", (short) 1200);
        furnace.put("CookTime", (short) 40);
        furnace.put("BurnDuration", (short) 1600);
        furnace.put("Items", compounds());
        CompoundTag java = convert(furnace);
        assertEquals(200, java.getInt("cooking_total_time"));
        assertEquals(40, java.getInt("cooking_time_spent"));

        CompoundTag smoker = blockEntity("Smoker");
        smoker.put("Items", compounds());
        assertEquals(100, convert(smoker).getInt("cooking_total_time"));

        CompoundTag campfire = blockEntity("Campfire");
        campfire.put("Item1", item("minecraft:beef", 1));
        campfire.put("ItemTime1", 120);
        java = convert(campfire);
        assertArrayEquals(new int[]{600, 0, 0, 0}, java.getIntArray("CookingTotalTimes"));
        assertArrayEquals(new int[]{120, 0, 0, 0}, java.getIntArray("CookingTimes"));
    }

    @Test
    public void testAxolotlBucket() {
        CompoundTag axolotl = actor("minecraft:axolotl", -115964090292L, 0, 0, 0);
        axolotl.put("Variant", 1);
        axolotl.put("definitions", strings("+minecraft:axolotl", "+axolotl_adult", "+axolotl_cyan"));
        CompoundTag health = new CompoundTag();
        health.put("Name", "minecraft:health");
        health.put("Base", 14.0f);
        axolotl.put("Attributes", compounds(health));
        axolotl.put("Persistent", (byte) 1);
        CompoundTag bucket = item("minecraft:axolotl_bucket", 1);
        bucket.put("tag", axolotl);

        CompoundTag java = convertItem(bucket);
        assertEquals("minecraft:axolotl_bucket", java.getString("id"));
        CompoundTag components = java.getCompound("components");
        assertEquals("cyan", components.getString("minecraft:axolotl/variant"));
        CompoundTag data = components.getCompound("minecraft:bucket_entity_data");
        assertEquals(14.0f, data.getFloat("Health"));
        assertEquals(0, data.getInt("Age"));
        assertEquals((byte) 1, data.getByte("PersistenceRequired"));
        assertFalse(components.contains("minecraft:entity_data"));
    }

    @Test
    public void testChargedCrossbow() {
        CompoundTag crossbow = item("minecraft:crossbow", 1);
        CompoundTag tag = new CompoundTag();
        tag.put("chargedItem", item("minecraft:arrow", 1));
        crossbow.put("tag", tag);
        ListTag<CompoundTag, ?> projectiles = convertItem(crossbow).getCompound("components").getList("minecraft:charged_projectiles", CompoundTag.class);
        assertEquals(1, projectiles.size());
        assertEquals("minecraft:arrow", projectiles.get(0).getString("id"));

        // Multishot (Bedrock enchantment 33) loads three
        CompoundTag multishot = new CompoundTag();
        multishot.put("id", (short) 33);
        multishot.put("lvl", (short) 1);
        tag.put("ench", compounds(multishot));
        projectiles = convertItem(crossbow).getCompound("components").getList("minecraft:charged_projectiles", CompoundTag.class);
        assertEquals(3, projectiles.size());
    }
}

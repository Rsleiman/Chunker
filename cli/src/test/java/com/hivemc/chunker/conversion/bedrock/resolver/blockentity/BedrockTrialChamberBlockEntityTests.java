package com.hivemc.chunker.conversion.bedrock.resolver.blockentity;

import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.TrialSpawnerBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.VaultBlockEntity;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.junit.jupiter.api.Test;

import static com.hivemc.chunker.conversion.entity.BedrockActors.compounds;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.BEDROCK;
import static com.hivemc.chunker.conversion.entity.EntityTestResolvers.JAVA;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for converting the configuration of trial spawners and vaults, which Java needs for them to work.
 */
public class BedrockTrialChamberBlockEntityTests {
    @Test
    public void testTrialSpawnerKeepsItsConfigurations() {
        // A trial spawner as BDS 1.26.61 saves it
        CompoundTag bedrock = new CompoundTag();
        bedrock.put("id", "TrialSpawner");
        bedrock.put("x", 4);
        bedrock.put("y", -22);
        bedrock.put("z", -285);
        bedrock.put("cooldown_end_at", 0L);
        bedrock.put("current_mobs", compounds());
        bedrock.put("next_mob_spawns_at", 0L);
        bedrock.put("normal_config", "minecraft:trial_chamber/breeze/normal");
        bedrock.put("ominous_config", "minecraft:trial_chamber/breeze/ominous");
        bedrock.put("registered_players", compounds());
        bedrock.put("required_player_range", 14);
        CompoundTag spawnData = new CompoundTag();
        spawnData.put("TypeId", "minecraft:breeze");
        spawnData.put("Weight", 1);
        bedrock.put("spawn_data", spawnData);
        bedrock.put("total_mobs_spawned", 0);

        BlockEntity blockEntity = BEDROCK.blockEntityResolver().to(bedrock).orElseThrow();
        CompoundTag java = JAVA.blockEntityResolver().from(blockEntity).orElseThrow();
        assertEquals("minecraft:trial_spawner", java.getString("id"));
        assertEquals("minecraft:trial_chamber/breeze/normal", java.getString("normal_config"));
        assertEquals("minecraft:trial_chamber/breeze/ominous", java.getString("ominous_config"));
        assertEquals(14, java.getInt("required_player_range"));

        // Reading the Java spawner back gives the same configuration
        TrialSpawnerBlockEntity readBack = (TrialSpawnerBlockEntity) JAVA.blockEntityResolver().to(java).orElseThrow();
        assertEquals(((TrialSpawnerBlockEntity) blockEntity).getNormalConfig(), readBack.getNormalConfig());
        assertEquals(((TrialSpawnerBlockEntity) blockEntity).getOminousConfig(), readBack.getOminousConfig());
    }

    @Test
    public void testInlineJavaConfigurationIsSkipped() {
        CompoundTag java = new CompoundTag();
        java.put("id", "minecraft:trial_spawner");
        java.put("x", 0);
        java.put("y", 0);
        java.put("z", 0);
        java.put("normal_config", new CompoundTag());
        TrialSpawnerBlockEntity blockEntity = (TrialSpawnerBlockEntity) JAVA.blockEntityResolver().to(java).orElseThrow();
        assertNull(blockEntity.getNormalConfig());
    }

    @Test
    public void testOminousVaultKeepsItsKeyAndLootTable() {
        // An ominous vault as BDS 1.26.61 saves it
        CompoundTag keyItem = new CompoundTag();
        keyItem.put("Count", (byte) 1);
        keyItem.put("Damage", (short) 0);
        keyItem.put("Name", "minecraft:ominous_trial_key");
        keyItem.put("WasPickedUp", (byte) 0);
        CompoundTag config = new CompoundTag();
        config.put("activation_range", 4.0f);
        config.put("deactivation_range", 4.5f);
        config.put("key_item", keyItem);
        config.put("loot_table", "loot_tables/chests/trial_chambers/reward_ominous.json");
        config.put("override_loot_table_to_display", "");
        CompoundTag data = new CompoundTag();
        data.put("items_to_eject", compounds());
        data.put("rewarded_players", compounds());
        data.put("state_updating_resumes_at", 2546209L);
        CompoundTag bedrock = new CompoundTag();
        bedrock.put("id", "Vault");
        bedrock.put("x", 12);
        bedrock.put("y", -25);
        bedrock.put("z", -285);
        bedrock.put("config", config);
        bedrock.put("data", data);

        BlockEntity blockEntity = BEDROCK.blockEntityResolver().to(bedrock).orElseThrow();
        CompoundTag java = JAVA.blockEntityResolver().from(blockEntity).orElseThrow();
        assertEquals("minecraft:vault", java.getString("id"));
        CompoundTag javaConfig = java.getCompound("config");
        assertEquals("minecraft:chests/trial_chambers/reward_ominous", javaConfig.getString("loot_table"));
        assertEquals("minecraft:ominous_trial_key", javaConfig.getCompound("key_item").getString("id"));
        assertEquals(1, javaConfig.getCompound("key_item").getInt("count"));
        assertEquals(4.0, javaConfig.getDouble("activation_range"));
        assertEquals(4.5, javaConfig.getDouble("deactivation_range"));
        assertFalse(javaConfig.contains("override_loot_table_to_display"), "An empty override isn't written");

        // Reading the Java vault back gives the same configuration
        VaultBlockEntity readBack = (VaultBlockEntity) JAVA.blockEntityResolver().to(java).orElseThrow();
        VaultBlockEntity original = (VaultBlockEntity) blockEntity;
        assertEquals(original.getLootTable(), readBack.getLootTable());
        assertEquals(original.getKeyItem().getIdentifier(), readBack.getKeyItem().getIdentifier());
        assertEquals(original.getActivationRange(), readBack.getActivationRange());
    }
}

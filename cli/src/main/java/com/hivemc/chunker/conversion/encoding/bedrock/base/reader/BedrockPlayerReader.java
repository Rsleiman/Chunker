package com.hivemc.chunker.conversion.encoding.bedrock.base.reader;

import com.hivemc.chunker.conversion.encoding.base.Converter;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers.BedrockLivingEntityReader;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerMobEffect;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelPlayer;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerPlayerAbilities;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerRespawnPoint;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.FloatTag;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectAVLTreeMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a Bedrock player record (the local player, or a player_server_ record of a server world).
 * <p>
 * A record holds the player's own data as it was when the player logged off. Fields that Bedrock only uses when the
 * player rejoins (RideID, the shoulder parrots) are not read: the vehicle and the parrots are ordinary entities in the
 * world.
 */
public final class BedrockPlayerReader {
    /**
     * The value Bedrock uses for a spawn position which isn't set.
     */
    public static final int UNSET_POSITION = Integer.MIN_VALUE;
    /**
     * The offset of the armor slots in the inventory map, which lines up with Java (100 = feet, 103 = head).
     */
    public static final byte ARMOR_SLOT_OFFSET = 100;
    /**
     * The slot of the off-hand item in the inventory map, which lines up with Java.
     */
    public static final byte OFFHAND_SLOT = (byte) 150;
    /**
     * The number of slots in the main inventory (hotbar 0-8, then 9-35).
     */
    public static final int INVENTORY_SIZE = 36;
    /**
     * The PlayerUIItems slot of the crafting result, which is a preview rather than an item the player holds.
     */
    private static final int UI_CREATED_OUTPUT_SLOT = 50;

    private BedrockPlayerReader() {
    }

    /**
     * Read a player record.
     *
     * @param resolvers the resolvers used for items and effects.
     * @param converter the converter, holding the dimension registry and the Java UUIDs of known players.
     * @param player    the player NBT.
     * @return the player.
     */
    public static ChunkerLevelPlayer read(BedrockResolvers resolvers, Converter converter, CompoundTag player) {
        DimensionRegistry dimensionRegistry = converter.getDimensionRegistry();

        // Position (Bedrock saves the eye position) and movement
        List<Float> positions = player.getListValues("Pos", FloatTag.class, List.of(0F, 0F, 0F));
        List<Float> motion = player.getListValues("Motion", FloatTag.class, List.of(0F, 0F, 0F));
        List<Float> rotation = player.getListValues("Rotation", FloatTag.class, List.of(0F, 0F));

        // Inventory, armor and off-hand, slotted as in Java
        Byte2ObjectAVLTreeMap<ChunkerItemStack> inventory = readSlots(resolvers, player.getList("Inventory", CompoundTag.class, null));
        ListTag<CompoundTag, Map<String, Tag<?>>> armor = player.getList("Armor", CompoundTag.class, null);
        if (armor != null) {
            // Head, chest, legs, feet, then a body slot which players don't use
            for (int i = 0; i < Math.min(armor.size(), 4); i++) {
                ChunkerItemStack item = BedrockLivingEntityReader.readItem(resolvers, armor.get(i));
                if (item == null) continue;
                inventory.put((byte) (ARMOR_SLOT_OFFSET + 3 - i), item);
            }
        }
        ListTag<CompoundTag, Map<String, Tag<?>>> offhand = player.getList("Offhand", CompoundTag.class, null);
        if (offhand != null && offhand.size() > 0) {
            ChunkerItemStack item = BedrockLivingEntityReader.readItem(resolvers, offhand.get(0));
            if (item != null) {
                inventory.put(OFFHAND_SLOT, item);
            }
        }
        addUIItems(resolvers, converter, player, inventory);

        ChunkerLevelPlayer output = new ChunkerLevelPlayer(
                dimensionRegistry.fromBedrockNBT(player.get("DimensionId"), Dimension.OVERWORLD),
                positions.get(0),
                positions.get(1) - BedrockLevelReader.PLAYER_HEIGHT, // Offset in Bedrock
                positions.get(2),
                motion.get(0),
                motion.get(1),
                motion.get(2),
                rotation.get(0),
                rotation.get(1),
                inventory,
                player.getInt("PlayerGameMode", 0)
        );

        // Identity, the UUID of the profile and of the assigned account
        if (player.contains("UniqueID")) {
            long uniqueId = player.getLong("UniqueID");
            output.setSourceId(String.valueOf(uniqueId));
            output.setProfileUUID(BedrockEntityUUIDs.toUUID(uniqueId));
            output.setAccountUUID(converter.getBedrockPlayerUUIDs().get(uniqueId));
        }

        // Health, air and falling
        CompoundTag health = BedrockLivingEntityReader.getAttribute(player, "minecraft:health");
        if (health != null) {
            output.setHealth(health.getFloat("Current", health.getFloat("Base", 20F)));
        }
        CompoundTag absorption = BedrockLivingEntityReader.getAttribute(player, "minecraft:absorption");
        if (absorption != null) {
            output.setAbsorption(absorption.getFloat("Current", 0F));
        }
        player.getOptionalValue("Air", Short.class).ifPresent(output::setAir);
        player.getOptionalValue("OnGround", Byte.class).ifPresent(value -> output.setOnGround(value == 1));
        player.getOptionalValue("FallDistance", Float.class).ifPresent(output::setFallDistance);

        // Hunger
        CompoundTag hunger = BedrockLivingEntityReader.getAttribute(player, "minecraft:player.hunger");
        if (hunger != null) {
            output.setFoodLevel(Math.round(hunger.getFloat("Current", 20F)));
        }
        CompoundTag saturation = BedrockLivingEntityReader.getAttribute(player, "minecraft:player.saturation");
        if (saturation != null) {
            output.setFoodSaturation(saturation.getFloat("Current", 0F));
        }
        CompoundTag exhaustion = BedrockLivingEntityReader.getAttribute(player, "minecraft:player.exhaustion");
        if (exhaustion != null) {
            output.setFoodExhaustion(exhaustion.getFloat("Current", 0F));
        }

        // Experience (also in the attributes as minecraft:player.level and minecraft:player.experience)
        if (player.contains("PlayerLevel")) {
            output.setXpLevel(player.getInt("PlayerLevel"));
            output.setXpProgress(player.getFloat("PlayerLevelProgress", 0F));
        }

        // Effects and the ender chest
        List<ChunkerMobEffect> effects = BedrockLivingEntityReader.readEffectList(resolvers, player);
        output.setEffects(effects.isEmpty() ? null : effects);
        output.setEnderChest(readSlots(resolvers, player.getList("EnderChestInventory", CompoundTag.class, null)));
        player.getOptionalValue("SelectedInventorySlot", Integer.class).ifPresent(output::setSelectedSlot);

        // Abilities
        CompoundTag abilities = player.getCompound("abilities");
        if (abilities != null) {
            output.setAbilities(new ChunkerPlayerAbilities(
                    abilities.getByte("mayfly", (byte) 0) == 1,
                    abilities.getByte("flying", (byte) 0) == 1,
                    abilities.getByte("instabuild", (byte) 0) == 1,
                    abilities.getByte("invulnerable", (byte) 0) == 1,
                    abilities.getByte("build", (byte) 1) == 1,
                    abilities.getFloat("walkSpeed", 0.1F),
                    abilities.getFloat("flySpeed", 0.05F)
            ));
        }

        output.setRespawnPoint(readRespawnPoint(dimensionRegistry, player));

        // Where the player last died
        if (player.getByte("HasDiedBefore", (byte) 0) == 1) {
            Dimension deathDimension = dimensionRegistry.fromBedrock(player.getInt("DeathDimension", 0), null);
            if (deathDimension != null) {
                output.setLastDeathLocation(new ChunkerGlobalPosition(
                        deathDimension,
                        player.getInt("DeathPositionX", 0),
                        player.getInt("DeathPositionY", 0),
                        player.getInt("DeathPositionZ", 0)
                ));
            }
        }
        return output;
    }

    /**
     * Read the respawn point of a player.
     * <p>
     * A bed or respawn anchor sets SpawnBlockPosition (the block) and SpawnX/Y/Z (where the player stands up), while
     * a position set by /spawnpoint only sets SpawnX/Y/Z. SpawnDimension is the dimension of either.
     *
     * @param dimensionRegistry the dimension registry.
     * @param player            the player NBT.
     * @return the respawn point or null if the player uses the world spawn.
     */
    @Nullable
    static ChunkerRespawnPoint readRespawnPoint(DimensionRegistry dimensionRegistry, CompoundTag player) {
        Dimension dimension = dimensionRegistry.fromBedrock(player.getInt("SpawnDimension", 0), null);
        if (dimension == null) return null;

        int blockX = player.getInt("SpawnBlockPositionX", UNSET_POSITION);
        if (blockX != UNSET_POSITION) {
            return new ChunkerRespawnPoint(
                    dimension,
                    blockX,
                    player.getInt("SpawnBlockPositionY", 0),
                    player.getInt("SpawnBlockPositionZ", 0),
                    0F,
                    false
            );
        }

        int spawnX = player.getInt("SpawnX", UNSET_POSITION);
        if (spawnX != UNSET_POSITION) {
            return new ChunkerRespawnPoint(
                    dimension,
                    spawnX,
                    player.getInt("SpawnY", 0),
                    player.getInt("SpawnZ", 0),
                    0F,
                    true
            );
        }
        return null;
    }

    /**
     * Read a list of slotted items, skipping empty slots.
     *
     * @param resolvers the resolvers.
     * @param items     the items (can be null).
     * @return the items ordered by slot.
     */
    private static Byte2ObjectAVLTreeMap<ChunkerItemStack> readSlots(BedrockResolvers resolvers, @Nullable ListTag<CompoundTag, Map<String, Tag<?>>> items) {
        Byte2ObjectAVLTreeMap<ChunkerItemStack> slots = new Byte2ObjectAVLTreeMap<>();
        if (items == null) return slots;

        byte index = 0;
        for (CompoundTag itemTag : items) {
            byte slot = itemTag.getByte("Slot", index);
            index++;

            ChunkerItemStack item = BedrockLivingEntityReader.readItem(resolvers, itemTag);
            if (item == null) continue;
            slots.put(slot, item);
        }
        return slots;
    }

    /**
     * Put the items left in the player's UI (the cursor, a crafting grid) into free inventory slots, as Bedrock
     * returns them to the inventory when the UI is closed. Items which don't fit are reported and not converted.
     *
     * @param resolvers the resolvers.
     * @param converter the converter, for reporting.
     * @param player    the player NBT.
     * @param inventory the inventory to add to.
     */
    private static void addUIItems(BedrockResolvers resolvers, Converter converter, CompoundTag player, Byte2ObjectAVLTreeMap<ChunkerItemStack> inventory) {
        ListTag<CompoundTag, Map<String, Tag<?>>> uiItems = player.getList("PlayerUIItems", CompoundTag.class, null);
        if (uiItems == null || uiItems.size() == 0) return;

        List<ChunkerItemStack> items = new ArrayList<>();
        for (CompoundTag itemTag : uiItems) {
            if (itemTag.getByte("Slot", (byte) 0) == UI_CREATED_OUTPUT_SLOT) continue;
            ChunkerItemStack item = BedrockLivingEntityReader.readItem(resolvers, itemTag);
            if (item != null) {
                items.add(item);
            }
        }

        int slot = 0;
        for (ChunkerItemStack item : items) {
            while (slot < INVENTORY_SIZE && inventory.containsKey((byte) slot)) {
                slot++;
            }
            if (slot >= INVENTORY_SIZE) {
                converter.logNonFatalException(new Exception("A player's inventory is full, an item left in their UI was not converted: " + item.getIdentifier()));
                continue;
            }
            inventory.put((byte) slot, item);
        }
    }
}

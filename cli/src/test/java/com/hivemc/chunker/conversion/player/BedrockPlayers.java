package com.hivemc.chunker.conversion.player;

import com.hivemc.chunker.conversion.WorldConverter;
import com.hivemc.chunker.conversion.bedrock.resolver.MockConverter;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockDataVersion;
import com.hivemc.chunker.conversion.encoding.bedrock.BedrockEncoders;
import com.hivemc.chunker.conversion.encoding.bedrock.base.reader.BedrockPlayerReader;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel;
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelPlayer;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.hivemc.chunker.conversion.entity.BedrockActors.compounds;
import static com.hivemc.chunker.conversion.entity.BedrockActors.floats;

/**
 * Synthetic Bedrock player records, modelled on the records of BDS 1.26.52 (no real IDs).
 */
public final class BedrockPlayers {
    public static final long PLAYER = -4294967295L;
    public static final UUID JAVA_ACCOUNT = UUID.fromString("907d4c76-76a0-4d98-80ab-92b41cb7c4a2");

    private BedrockPlayers() {
    }

    /**
     * Create a converter and the Bedrock resolvers for reading players.
     *
     * @param playerUUIDs the Java UUIDs of Bedrock players.
     * @return the converter.
     */
    public static WorldConverter converter(Map<Long, UUID> playerUUIDs) {
        WorldConverter converter = new MockConverter(new ChunkerLevel(null, null, Collections.emptyList(), null, Collections.emptyList()));
        converter.setBedrockPlayerUUIDs(playerUUIDs);
        return converter;
    }

    /**
     * Read a player record with the Bedrock 1.26.50 resolvers.
     *
     * @param converter the converter.
     * @param record    the player record.
     * @return the player.
     */
    public static ChunkerLevelPlayer read(WorldConverter converter, CompoundTag record) {
        BedrockResolvers resolvers = BedrockEncoders.getNearestEncoder(BedrockDataVersion.V1_26_50)
                .readerConstructor()
                .construct(null, BedrockDataVersion.V1_26_50.getVersion(), converter)
                .buildResolvers(converter)
                .build();
        return BedrockPlayerReader.read(resolvers, converter, record);
    }

    /**
     * Create a minimal player record, as BDS writes for a player who hasn't done anything.
     *
     * @return the record.
     */
    public static CompoundTag record() {
        CompoundTag player = new CompoundTag();
        player.put("identifier", "minecraft:player");
        player.put("UniqueID", PLAYER);
        player.put("Pos", floats(100.5F, 76.62001F, 9.5F));
        player.put("Motion", floats(0F, 0F, 0F));
        player.put("Rotation", floats(90F, 10F));
        player.put("DimensionId", 0);
        player.put("PlayerGameMode", 5);
        player.put("PlayerLevel", 0);
        player.put("PlayerLevelProgress", 0F);
        player.put("Air", (short) 300);
        player.put("OnGround", (byte) 1);
        player.put("FallDistance", 0F);
        player.put("SelectedInventorySlot", 0);
        player.put("SpawnBlockPositionX", BedrockPlayerReader.UNSET_POSITION);
        player.put("SpawnBlockPositionY", BedrockPlayerReader.UNSET_POSITION);
        player.put("SpawnBlockPositionZ", BedrockPlayerReader.UNSET_POSITION);
        player.put("SpawnDimension", 3);
        player.put("SpawnX", BedrockPlayerReader.UNSET_POSITION);
        player.put("SpawnY", BedrockPlayerReader.UNSET_POSITION);
        player.put("SpawnZ", BedrockPlayerReader.UNSET_POSITION);
        player.put("Attributes", compounds(
                attribute("minecraft:health", 20F, 20F),
                attribute("minecraft:absorption", 0F, 16F),
                attribute("minecraft:player.hunger", 20F, 20F),
                attribute("minecraft:player.saturation", 5F, 20F),
                attribute("minecraft:player.exhaustion", 0F, 20F),
                attribute("minecraft:player.level", 0F, 24791F),
                attribute("minecraft:player.experience", 0F, 1F)
        ));
        player.put("Inventory", emptySlots(36));
        player.put("Armor", compounds(emptyItem(), emptyItem(), emptyItem(), emptyItem(), emptyItem()));
        player.put("Offhand", compounds(emptyItem()));
        player.put("Mainhand", compounds(emptyItem()));
        player.put("EnderChestInventory", emptySlots(27));
        player.put("PlayerUIItems", new ListTag<>(TagType.COMPOUND));

        CompoundTag abilities = new CompoundTag();
        abilities.put("build", (byte) 1);
        abilities.put("flySpeed", 0.05F);
        abilities.put("flying", (byte) 0);
        abilities.put("instabuild", (byte) 0);
        abilities.put("invulnerable", (byte) 0);
        abilities.put("mayfly", (byte) 0);
        abilities.put("walkSpeed", 0.1F);
        player.put("abilities", abilities);
        return player;
    }

    /**
     * Create an entry of the Attributes list.
     *
     * @param name    the name.
     * @param current the current value.
     * @param max     the maximum.
     * @return the attribute.
     */
    public static CompoundTag attribute(String name, float current, float max) {
        CompoundTag attribute = new CompoundTag();
        attribute.put("Name", name);
        attribute.put("Base", current);
        attribute.put("Current", current);
        attribute.put("Max", max);
        return attribute;
    }

    /**
     * Set an entry of the Attributes list of a record.
     *
     * @param record  the record.
     * @param name    the name.
     * @param current the current value.
     */
    public static void setAttribute(CompoundTag record, String name, float current) {
        for (CompoundTag attribute : record.getList("Attributes", CompoundTag.class, null)) {
            if (name.equals(attribute.getString("Name", null))) {
                attribute.put("Current", current);
            }
        }
    }

    /**
     * Create an item stack.
     *
     * @param name  the item name.
     * @param count the count.
     * @return the item.
     */
    public static CompoundTag item(String name, int count) {
        CompoundTag item = new CompoundTag();
        item.put("Name", name);
        item.put("Count", (byte) count);
        item.put("Damage", (short) 0);
        item.put("WasPickedUp", (byte) 0);
        return item;
    }

    /**
     * Create an item stack in a slot.
     *
     * @param slot  the slot.
     * @param name  the item name.
     * @param count the count.
     * @return the item.
     */
    public static CompoundTag item(int slot, String name, int count) {
        CompoundTag item = item(name, count);
        item.put("Slot", (byte) slot);
        return item;
    }

    /**
     * Create an empty slot, as Bedrock writes them.
     *
     * @return the empty item.
     */
    public static CompoundTag emptyItem() {
        return item("", 0);
    }

    /**
     * Put an item into a slotted list of a record (Inventory, EnderChestInventory).
     *
     * @param record the record.
     * @param list   the list name.
     * @param item   the item, with its slot.
     */
    public static void setSlot(CompoundTag record, String list, CompoundTag item) {
        ListTag<CompoundTag, Map<String, Tag<?>>> slots = record.getList(list, CompoundTag.class, null);
        slots.set(item.getByte("Slot", (byte) 0), item);
    }

    private static ListTag<CompoundTag, Map<String, Tag<?>>> emptySlots(int size) {
        ListTag<CompoundTag, Map<String, Tag<?>>> slots = new ListTag<>(TagType.COMPOUND, size);
        for (int i = 0; i < size; i++) {
            CompoundTag empty = emptyItem();
            empty.put("Slot", (byte) i);
            slots.add(empty);
        }
        return slots;
    }

    /**
     * Set a record's unlocked recipes, as Bedrock saves them.
     *
     * @param record the player record.
     * @param ids    the recipe IDs.
     */
    public static void setUnlockedRecipes(CompoundTag record, String... ids) {
        ListTag<StringTag, String> list = new ListTag<>(TagType.STRING, ids.length);
        for (String id : ids) {
            list.add(new StringTag(id));
        }
        CompoundTag recipeUnlocking = new CompoundTag();
        recipeUnlocking.put("unlocked_recipes", list);
        record.put("recipe_unlocking", recipeUnlocking);
    }
}

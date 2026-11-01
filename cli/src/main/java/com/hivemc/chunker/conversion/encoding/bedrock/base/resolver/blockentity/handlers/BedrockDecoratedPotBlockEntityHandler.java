package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.DecoratedPotBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerItemStackIdentifier;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.mapping.identifier.Identifier;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import com.hivemc.chunker.nbt.tags.primitive.StringTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for decorated pots.
 */
public class BedrockDecoratedPotBlockEntityHandler extends BlockEntityHandler<BedrockResolvers, CompoundTag, DecoratedPotBlockEntity> {
    private final boolean enableLootTables;

    public BedrockDecoratedPotBlockEntityHandler(boolean enableLootTables) {
        super("DecoratedPot", DecoratedPotBlockEntity.class, DecoratedPotBlockEntity::new);
        this.enableLootTables = enableLootTables;
    }

    @Override
    public void read(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag input, @NotNull DecoratedPotBlockEntity value) {
        String tagName = input.contains("sherds") ? "sherds" : "shards";
        List<String> sherds = input.getListValues(tagName, StringTag.class, null);
        if (sherds != null && !sherds.isEmpty()) {
            List<ChunkerItemStackIdentifier> items = new ArrayList<>(sherds.size());
            for (String sherd : sherds) {
                items.add(resolvers.readItemIdentifier(new Identifier(sherd)).getIdentifier());
            }

            // We'll set the sherds that are present
            value.setBack(items.get(0));
            if (items.size() > 1) value.setLeft(items.get(1));
            if (items.size() > 2) value.setRight(items.get(2));
            if (items.size() > 3) value.setFront(items.get(3));
        }

        // Read item (an emptied pot keeps an item with a count of 0)
        CompoundTag item = input.getCompound("item");
        if (item != null && item.getByte("Count", (byte) 1) > 0) {
            value.setItem(resolvers.readItem(item));
        }

        // Pots in trial chambers are filled from a loot table when they're broken
        String lootTable = input.getString("LootTable", null);
        if (lootTable != null && !lootTable.isEmpty() && enableLootTables) {
            value.setLootTable(BedrockRandomizableContainerBlockEntityHandler.bedrockLootTableToChunker(lootTable));
        }
    }

    @Override
    public void write(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag output, @NotNull DecoratedPotBlockEntity value) {
        ListTag<StringTag, String> sherds = new ListTag<>(TagType.STRING, 4);
        sherds.add(new StringTag(resolvers.writeItemIdentifier(new ChunkerItemStack(value.getBack())).getIdentifier()));
        sherds.add(new StringTag(resolvers.writeItemIdentifier(new ChunkerItemStack(value.getLeft())).getIdentifier()));
        sherds.add(new StringTag(resolvers.writeItemIdentifier(new ChunkerItemStack(value.getRight())).getIdentifier()));
        sherds.add(new StringTag(resolvers.writeItemIdentifier(new ChunkerItemStack(value.getFront())).getIdentifier()));

        output.put(resolvers.dataVersion().getVersion().isLessThan(1, 20, 0) ? "shards" : "sherds", sherds);

        // Write the item stored in the pot (1.20.50 and above)
        if (resolvers.dataVersion().getVersion().isGreaterThanOrEqual(1, 20, 50)) {
            if (value.getItem() != null && !value.getItem().getIdentifier().isAir()) {
                resolvers.writeItem(value.getItem()).ifPresent(item -> output.put("item", item));
            }
        }
        if (value.getLootTable() != null && enableLootTables) {
            output.put("LootTable", BedrockRandomizableContainerBlockEntityHandler.chunkerLootTableToBedrock(value.getLootTable()));
        }
    }
}

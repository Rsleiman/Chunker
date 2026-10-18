package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BeehiveBlockEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for beehives and bee nests, reading the bees inside. Bedrock stores each occupant's full actor record, which
 * is read like any other actor. Writing the occupants to Bedrock isn't supported (they are only kept when the original
 * NBT is copied).
 */
public class BedrockBeehiveBlockEntityHandler extends BlockEntityHandler<BedrockResolvers, CompoundTag, BeehiveBlockEntity> {
    public BedrockBeehiveBlockEntityHandler() {
        super("Beehive", BeehiveBlockEntity.class, BeehiveBlockEntity::new);
    }

    @Override
    public void read(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag input, @NotNull BeehiveBlockEntity value) {
        ListTag<CompoundTag, Map<String, Tag<?>>> occupants = input.getList("Occupants", CompoundTag.class, null);
        if (occupants == null) return;

        List<BeehiveBlockEntity.Occupant> output = new ArrayList<>(occupants.size());
        for (CompoundTag occupant : occupants) {
            // Bees of a newly generated nest have no saved actor yet, they are adult bees
            CompoundTag saveData = occupant.getCompound("SaveData");
            Optional<Entity> entity;
            if (saveData == null || saveData.size() == 0) {
                GenericEntity bee = new GenericEntity(ChunkerVanillaEntityType.BEE);
                bee.put(ChunkerEntityProperty.BABY, false);
                entity = Optional.of(bee);
            } else {
                entity = resolvers.entityResolver().to(saveData);
            }

            // Bedrock counts the remaining ticks down, Java counts up to the minimum stay
            int ticksLeft = Math.max(0, occupant.getInt("TicksLeftToStay", 0));
            entity.ifPresent(bee -> output.add(new BeehiveBlockEntity.Occupant(bee, 0, ticksLeft)));
        }
        value.setOccupants(output);
    }

    @Override
    public void write(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag output, @NotNull BeehiveBlockEntity value) {
        // Occupants are only written as part of the original NBT
    }
}

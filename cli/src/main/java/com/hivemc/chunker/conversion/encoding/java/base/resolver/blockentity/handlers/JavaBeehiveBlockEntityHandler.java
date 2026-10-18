package com.hivemc.chunker.conversion.encoding.java.base.resolver.blockentity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.blockentity.BlockEntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BeehiveBlockEntity;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for beehives and bee nests with the bees inside, as stored from 1.21.5 onwards
 * (bees: [{entity_data, ticks_in_hive, min_ticks_in_hive}]).
 */
public class JavaBeehiveBlockEntityHandler extends BlockEntityHandler<JavaResolvers, CompoundTag, BeehiveBlockEntity> {
    public JavaBeehiveBlockEntityHandler() {
        super("minecraft:beehive", BeehiveBlockEntity.class, BeehiveBlockEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull BeehiveBlockEntity value) {
        ListTag<CompoundTag, Map<String, Tag<?>>> bees = input.getList("bees", CompoundTag.class, null);
        if (bees == null) return;

        List<BeehiveBlockEntity.Occupant> occupants = new ArrayList<>(bees.size());
        for (CompoundTag bee : bees) {
            CompoundTag entityData = bee.getCompound("entity_data");
            if (entityData == null) continue;
            resolvers.entityResolver().to(entityData).ifPresent(entity -> occupants.add(new BeehiveBlockEntity.Occupant(
                    entity,
                    bee.getInt("ticks_in_hive", 0),
                    bee.getInt("min_ticks_in_hive", 600)
            )));
        }
        value.setOccupants(occupants);
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull BeehiveBlockEntity value) {
        if (value.getOccupants().isEmpty()) return;

        ListTag<CompoundTag, Map<String, Tag<?>>> bees = new ListTag<>(TagType.COMPOUND, value.getOccupants().size());
        for (BeehiveBlockEntity.Occupant occupant : value.getOccupants()) {
            resolvers.entityResolver().from(occupant.entity()).ifPresent(entityData -> {
                CompoundTag bee = new CompoundTag();
                bee.put("entity_data", entityData);
                bee.put("ticks_in_hive", occupant.ticksInHive());
                bee.put("min_ticks_in_hive", occupant.minTicksInHive());
                bees.add(bee);
            });
        }
        output.put("bees", bees);
    }
}

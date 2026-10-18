package com.hivemc.chunker.conversion.encoding.java.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.entity.EntityHandler;
import com.hivemc.chunker.conversion.encoding.java.base.resolver.JavaResolvers;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.nbt.TagType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for mobs, dropped items, experience orbs, vehicles and projectiles stored as a GenericEntity.
 */
public class JavaGenericEntityHandler extends EntityHandler<JavaResolvers, CompoundTag, GenericEntity> {
    public JavaGenericEntityHandler() {
        super(null, GenericEntity.class, GenericEntity::new);
    }

    @Override
    public void read(@NotNull JavaResolvers resolvers, @NotNull CompoundTag input, @NotNull GenericEntity value) {
        value.setEntityType(resolvers.entityResolver().getKey(input).orElseThrow());
        if (GenericEntityTypes.isLiving(value.getEntityType())) {
            JavaLivingEntityData.read(resolvers, input, value);
        } else {
            JavaNonLivingEntityData.read(resolvers, input, value);
        }

        // Riders are nested in their vehicle
        ListTag<CompoundTag, Map<String, Tag<?>>> passengersTag = input.getList("Passengers", CompoundTag.class, null);
        if (passengersTag != null) {
            List<Entity> passengers = new ArrayList<>(passengersTag.size());
            for (CompoundTag passengerTag : passengersTag) {
                resolvers.entityResolver().to(passengerTag).ifPresent(passengers::add);
            }
            if (!passengers.isEmpty()) {
                value.put(ChunkerEntityProperty.PASSENGERS, passengers);
            }
        }

        // Leashes held by an entity (a fence knot is stored as a block position instead)
        CompoundTag leash = input.getCompound("leash");
        UUID holder = leash == null ? null : JavaEntityNBT.fromIntArray(leash.getIntArray("UUID", null));
        if (holder != null) {
            value.put(ChunkerEntityProperty.LEASH_HOLDER, holder);
        }
    }

    @Override
    public void write(@NotNull JavaResolvers resolvers, @NotNull CompoundTag output, @NotNull GenericEntity value) {
        if (GenericEntityTypes.isLiving(value.getEntityType())) {
            JavaLivingEntityData.write(resolvers, output, value);
        } else {
            JavaNonLivingEntityData.write(resolvers, output, value);
        }

        List<Entity> passengers = value.get(ChunkerEntityProperty.PASSENGERS);
        if (passengers != null) {
            ListTag<CompoundTag, Map<String, Tag<?>>> passengersTag = new ListTag<>(TagType.COMPOUND, passengers.size());
            for (Entity passenger : passengers) {
                resolvers.entityResolver().from(passenger).ifPresent(passengersTag::add);
            }
            if (passengersTag.size() > 0) {
                output.put("Passengers", passengersTag);
            }
        }

        UUID holder = value.get(ChunkerEntityProperty.LEASH_HOLDER);
        if (holder != null) {
            CompoundTag leash = new CompoundTag();
            leash.put("UUID", JavaEntityNBT.toIntArray(holder));
            output.put("leash", leash);
        }
    }
}

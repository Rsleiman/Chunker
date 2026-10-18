package com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.handlers;

import com.hivemc.chunker.conversion.encoding.base.resolver.entity.DoNotProcessEntityHandler;
import com.hivemc.chunker.conversion.encoding.base.resolver.entity.DoNotWriteEntityHandler;
import com.hivemc.chunker.conversion.encoding.base.resolver.entity.EntityHandler;
import com.hivemc.chunker.conversion.encoding.base.resolver.entity.UpdateBeforeProcessEntityHandler;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.BedrockResolvers;
import com.hivemc.chunker.conversion.encoding.bedrock.base.resolver.entity.BedrockEntityUUIDs;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.entity.ChunkerEntityProperty;
import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntity;
import com.hivemc.chunker.conversion.intermediate.column.entity.GenericEntityTypes;
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType;
import com.hivemc.chunker.nbt.tags.Tag;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import com.hivemc.chunker.nbt.tags.collection.ListTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler which reads mobs, dropped items, experience orbs, vehicles and projectiles into a GenericEntity.
 * Writing these to Bedrock isn't supported, they are only written when the original NBT is copied (Bedrock to Bedrock).
 * <p>
 * Bedrock stores a rider as a separate actor which its vehicle links to (LinksTag). Riders in the same column as their
 * vehicle are moved into the vehicle's passengers; a rider in another column stays a separate entity.
 */
public class BedrockGenericEntityHandler extends EntityHandler<BedrockResolvers, CompoundTag, GenericEntity>
        implements DoNotWriteEntityHandler<GenericEntity>, DoNotProcessEntityHandler<GenericEntity>, UpdateBeforeProcessEntityHandler<BedrockResolvers, GenericEntity> {
    public BedrockGenericEntityHandler() {
        super(null, GenericEntity.class, GenericEntity::new);
    }

    @Override
    public void read(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag input, @NotNull GenericEntity value) {
        value.setEntityType(resolvers.entityResolver().getKey(input).orElseThrow());
        if (GenericEntityTypes.isLiving(value.getEntityType())) {
            BedrockLivingEntityReader.read(resolvers, input, value);
        } else {
            BedrockNonLivingEntityReader.read(resolvers, input, value);
        }

        // Riders, resolved once the column's entities are read
        ListTag<CompoundTag, Map<String, Tag<?>>> links = input.getList("LinksTag", CompoundTag.class, null);
        if (links != null && links.size() > 0) {
            List<UUID> passengerIds = new ArrayList<>(links.size());
            for (CompoundTag link : links) {
                long riderId = link.getLong("entityID", -1);
                if (riderId != -1) {
                    passengerIds.add(BedrockEntityUUIDs.toUUID(riderId));
                }
            }
            value.put(ChunkerEntityProperty.PASSENGER_IDS, passengerIds);
        }

        // Leashes
        BedrockEntityUUIDs.referenceUUID(input.getLong("LeasherID", -1)).ifPresent(holder -> value.put(ChunkerEntityProperty.LEASH_HOLDER, holder));
    }

    @Override
    public void write(@NotNull BedrockResolvers resolvers, @NotNull CompoundTag output, @NotNull GenericEntity value) {
        // Only the original NBT is written (by the base handler)
    }

    @Override
    public GenericEntity updateBeforeProcess(@NotNull BedrockResolvers resolvers, ChunkerColumn column, GenericEntity entity) {
        List<UUID> passengerIds = entity.remove(ChunkerEntityProperty.PASSENGER_IDS);
        if (passengerIds == null) return entity;

        // Find the riders in this column
        List<Entity> passengers = new ArrayList<>(passengerIds.size());
        for (UUID passengerId : passengerIds) {
            for (Entity other : column.getEntities()) {
                if (other != entity && passengerId.equals(other.get(ChunkerEntityProperty.UUID))) {
                    passengers.add(other);
                    break;
                }
            }
        }
        if (!passengers.isEmpty()) {
            entity.put(ChunkerEntityProperty.PASSENGERS, passengers);
        }
        return entity;
    }

    @Override
    public boolean shouldRemoveBeforeProcess(ChunkerColumn column, GenericEntity entity) {
        // A dropped item without a readable item can't exist in Java
        if (entity.getEntityType() == ChunkerVanillaEntityType.ITEM && entity.get(ChunkerEntityProperty.ITEM) == null) {
            return true;
        }

        // Riders are stored inside their vehicle
        for (Entity other : column.getEntities()) {
            List<Entity> passengers = other.get(ChunkerEntityProperty.PASSENGERS);
            if (passengers != null && passengers.stream().anyMatch(passenger -> passenger == entity)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldRemoveBeforeWrite(ChunkerColumn column, GenericEntity entity) {
        return entity.getOriginalNBT() == null;
    }
}

package com.hivemc.chunker.conversion.intermediate.column.entity;

import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An entity whose type specific data is stored as {@link ChunkerEntityProperty} values, used for mobs, dropped items,
 * experience orbs, vehicles and projectiles. The type is set by the reader from the encoded identifier.
 */
public class GenericEntity extends Entity {
    @Nullable
    private ChunkerEntityType entityType;

    /**
     * Create a new generic entity, the type is set when it is read.
     */
    public GenericEntity() {
    }

    /**
     * Create a new generic entity.
     *
     * @param entityType the type of the entity.
     */
    public GenericEntity(ChunkerEntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public ChunkerEntityType getEntityType() {
        return entityType;
    }

    /**
     * Set the type of the entity.
     *
     * @param entityType the type of the entity.
     */
    public void setEntityType(ChunkerEntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericEntity that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getEntityType(), that.getEntityType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEntityType());
    }

    @Override
    public String toString() {
        return "GenericEntity{" +
                "entityType=" + getEntityType() +
                ", positionX=" + getPositionX() +
                ", positionY=" + getPositionY() +
                ", positionZ=" + getPositionZ() +
                ", properties=" + getProperties() +
                '}';
    }
}

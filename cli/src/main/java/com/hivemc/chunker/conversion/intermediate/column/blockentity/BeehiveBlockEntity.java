package com.hivemc.chunker.conversion.intermediate.column.blockentity;

import com.hivemc.chunker.conversion.intermediate.column.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Beehive Block Entity.
 */
public class BeehiveBlockEntity extends BlockEntity {
    private List<Occupant> occupants = new ArrayList<>(0);

    /**
     * Get the bees inside the hive.
     *
     * @return the occupants.
     */
    public List<Occupant> getOccupants() {
        return occupants;
    }

    /**
     * Set the bees inside the hive.
     *
     * @param occupants the occupants.
     */
    public void setOccupants(List<Occupant> occupants) {
        this.occupants = occupants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeehiveBlockEntity that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getOccupants(), that.getOccupants());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getOccupants());
    }

    /**
     * A bee inside a hive.
     *
     * @param entity         the bee.
     * @param ticksInHive    the ticks it has spent in the hive.
     * @param minTicksInHive the ticks it stays in the hive before leaving.
     */
    public record Occupant(Entity entity, int ticksInHive, int minTicksInHive) {
    }
}

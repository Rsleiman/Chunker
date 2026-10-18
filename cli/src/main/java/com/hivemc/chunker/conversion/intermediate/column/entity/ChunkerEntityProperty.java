package com.hivemc.chunker.conversion.intermediate.column.entity;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.hivemc.chunker.resolver.property.Property;

import java.util.List;
import java.util.UUID;

/**
 * A property type which can be set on entities.
 * Identifiers of data driven registries (variants) use the Java names without a namespace, as do the numeric variant
 * values, the reader for each format converts its own values to these.
 *
 * @param <V> the type that the property holds.
 */
public class ChunkerEntityProperty<V> extends Property<Entity, V> {
    // Every entity
    /**
     * The unique ID of the entity, used by other entities to reference it (owners, riders, leashes).
     */
    public static final ChunkerEntityProperty<UUID> UUID = new ChunkerEntityProperty<>(
            "uuid",
            java.util.UUID.class
    );
    /**
     * The custom name of the entity as a JSON text component.
     */
    public static final ChunkerEntityProperty<JsonElement> CUSTOM_NAME = new ChunkerEntityProperty<>(
            "custom_name",
            JsonElement.class
    );
    /**
     * Whether the custom name is always visible.
     */
    public static final ChunkerEntityProperty<Boolean> CUSTOM_NAME_VISIBLE = new ChunkerEntityProperty<>(
            "custom_name_visible",
            Boolean.class
    );
    /**
     * The remaining air in ticks.
     */
    public static final ChunkerEntityProperty<Integer> AIR = new ChunkerEntityProperty<>(
            "air",
            Integer.class
    );
    /**
     * Whether the entity is on the ground.
     */
    public static final ChunkerEntityProperty<Boolean> ON_GROUND = new ChunkerEntityProperty<>(
            "on_ground",
            Boolean.class
    );
    /**
     * Whether the entity is invulnerable.
     */
    public static final ChunkerEntityProperty<Boolean> INVULNERABLE = new ChunkerEntityProperty<>(
            "invulnerable",
            Boolean.class
    );
    /**
     * The ticks until the entity can use a portal again.
     */
    public static final ChunkerEntityProperty<Integer> PORTAL_COOLDOWN = new ChunkerEntityProperty<>(
            "portal_cooldown",
            Integer.class
    );
    /**
     * The distance the entity has fallen.
     */
    public static final ChunkerEntityProperty<Double> FALL_DISTANCE = new ChunkerEntityProperty<>(
            "fall_distance",
            Double.class
    );
    /**
     * The scoreboard tags of the entity.
     */
    public static final ChunkerEntityProperty<List<String>> TAGS = new ChunkerEntityProperty<>(
            "tags",
            new TypeToken<>() {
            }
    );
    /**
     * The entities riding this entity.
     */
    public static final ChunkerEntityProperty<List<Entity>> PASSENGERS = new ChunkerEntityProperty<>(
            "passengers",
            new TypeToken<>() {
            }
    );

    /**
     * Create a new entity property.
     *
     * @param name the unique name of the property, used for comparison.
     * @param type the class type of the property.
     */
    private ChunkerEntityProperty(String name, Class<? super V> type) {
        super(name, type);
    }

    /**
     * Create a new entity property using a type token.
     *
     * @param name the unique name of the property, used for comparison.
     * @param type the type token used to capture the exact generic parameters of the type.
     */
    private ChunkerEntityProperty(String name, TypeToken<? super V> type) {
        super(name, type.getType());
    }
}

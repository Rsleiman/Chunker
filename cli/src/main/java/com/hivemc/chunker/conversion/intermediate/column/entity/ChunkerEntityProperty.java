package com.hivemc.chunker.conversion.intermediate.column.entity;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor;
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerBlockPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerEquipmentSlot;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGossip;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerMobEffect;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerTradeOffer;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerMemory;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerProfession;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerVillagerType;
import com.hivemc.chunker.resolver.property.Property;

import java.util.List;
import java.util.Map;
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
     * The UUIDs of the entities riding this entity, while a reader resolves them into {@link #PASSENGERS} (Bedrock
     * stores riders as separate actors, which the vehicle links to).
     */
    public static final ChunkerEntityProperty<List<UUID>> PASSENGER_IDS = new ChunkerEntityProperty<>(
            "passenger_ids",
            new TypeToken<>() {
            }
    );
    /**
     * The entity holding this entity's leash.
     */
    public static final ChunkerEntityProperty<UUID> LEASH_HOLDER = new ChunkerEntityProperty<>(
            "leash_holder",
            UUID.class
    );

    // Living entities
    /**
     * The current health (of a mob, a dropped item or an experience orb).
     */
    public static final ChunkerEntityProperty<Float> HEALTH = new ChunkerEntityProperty<>(
            "health",
            Float.class
    );
    /**
     * The base maximum health, only set when it differs per entity (horses, tamed wolves).
     */
    public static final ChunkerEntityProperty<Double> MAX_HEALTH = new ChunkerEntityProperty<>(
            "max_health",
            Double.class
    );
    /**
     * The base movement speed, only set when it differs per entity (horses).
     */
    public static final ChunkerEntityProperty<Double> MOVEMENT_SPEED = new ChunkerEntityProperty<>(
            "movement_speed",
            Double.class
    );
    /**
     * The base jump strength, only set when it differs per entity (horses).
     */
    public static final ChunkerEntityProperty<Double> JUMP_STRENGTH = new ChunkerEntityProperty<>(
            "jump_strength",
            Double.class
    );
    /**
     * The items worn or held.
     */
    public static final ChunkerEntityProperty<Map<ChunkerEquipmentSlot, ChunkerItemStack>> EQUIPMENT = new ChunkerEntityProperty<>(
            "equipment",
            new TypeToken<>() {
            }
    );
    /**
     * The chance of each equipment slot dropping when the entity dies (2 guarantees the drop).
     */
    public static final ChunkerEntityProperty<Map<ChunkerEquipmentSlot, Float>> DROP_CHANCES = new ChunkerEntityProperty<>(
            "drop_chances",
            new TypeToken<>() {
            }
    );
    /**
     * The active status effects.
     */
    public static final ChunkerEntityProperty<List<ChunkerMobEffect>> EFFECTS = new ChunkerEntityProperty<>(
            "effects",
            new TypeToken<>() {
            }
    );
    /**
     * Whether the entity never despawns.
     */
    public static final ChunkerEntityProperty<Boolean> PERSISTENT = new ChunkerEntityProperty<>(
            "persistent",
            Boolean.class
    );
    /**
     * Whether the entity picks up items.
     */
    public static final ChunkerEntityProperty<Boolean> CAN_PICK_UP_LOOT = new ChunkerEntityProperty<>(
            "can_pick_up_loot",
            Boolean.class
    );
    /**
     * Whether the entity is a baby.
     */
    public static final ChunkerEntityProperty<Boolean> BABY = new ChunkerEntityProperty<>(
            "baby",
            Boolean.class
    );
    /**
     * The age in ticks of a breedable entity, negative for babies (counting up to 0) and positive for the breeding
     * cooldown of adults. For dropped items and experience orbs, the ticks they have existed.
     */
    public static final ChunkerEntityProperty<Integer> AGE = new ChunkerEntityProperty<>(
            "age",
            Integer.class
    );
    /**
     * The ticks left in love mode.
     */
    public static final ChunkerEntityProperty<Integer> IN_LOVE = new ChunkerEntityProperty<>(
            "in_love",
            Integer.class
    );
    /**
     * The player which caused love mode.
     */
    public static final ChunkerEntityProperty<UUID> LOVE_CAUSE = new ChunkerEntityProperty<>(
            "love_cause",
            java.util.UUID.class
    );
    /**
     * Whether the entity is tamed.
     */
    public static final ChunkerEntityProperty<Boolean> TAMED = new ChunkerEntityProperty<>(
            "tamed",
            Boolean.class
    );
    /**
     * The owner of a tamed entity, or the entity which shot a projectile.
     */
    public static final ChunkerEntityProperty<UUID> OWNER = new ChunkerEntityProperty<>(
            "owner",
            java.util.UUID.class
    );
    /**
     * Whether a tamed entity is sitting.
     */
    public static final ChunkerEntityProperty<Boolean> SITTING = new ChunkerEntityProperty<>(
            "sitting",
            Boolean.class
    );

    // Type specific data
    /**
     * The variant from a data driven registry (Java name without a namespace, e.g. "black" for a cat).
     * Used by cats, wolves, cows, pigs, chickens, foxes ("red" / "snow") and mooshrooms ("red" / "brown").
     */
    public static final ChunkerEntityProperty<String> VARIANT = new ChunkerEntityProperty<>(
            "variant",
            String.class
    );
    /**
     * The sound variant (Java name without a namespace).
     */
    public static final ChunkerEntityProperty<String> SOUND_VARIANT = new ChunkerEntityProperty<>(
            "sound_variant",
            String.class
    );
    /**
     * The numeric variant with Java's numbering: horses (color | markings &lt;&lt; 8), llamas and tropical fish
     * (shape | pattern &lt;&lt; 8 | base color &lt;&lt; 16 | pattern color &lt;&lt; 24).
     */
    public static final ChunkerEntityProperty<Integer> VARIANT_ID = new ChunkerEntityProperty<>(
            "variant_id",
            Integer.class
    );
    /**
     * The wool color of a sheep.
     */
    public static final ChunkerEntityProperty<ChunkerDyeColor> COLOR = new ChunkerEntityProperty<>(
            "color",
            ChunkerDyeColor.class
    );
    /**
     * The collar color of a tamed wolf or cat.
     */
    public static final ChunkerEntityProperty<ChunkerDyeColor> COLLAR_COLOR = new ChunkerEntityProperty<>(
            "collar_color",
            ChunkerDyeColor.class
    );
    /**
     * Whether a sheep is sheared.
     */
    public static final ChunkerEntityProperty<Boolean> SHEARED = new ChunkerEntityProperty<>(
            "sheared",
            Boolean.class
    );
    /**
     * The temper of a horse (how close it is to being tamed).
     */
    public static final ChunkerEntityProperty<Integer> TEMPER = new ChunkerEntityProperty<>(
            "temper",
            Integer.class
    );
    /**
     * Whether a donkey, mule or llama carries a chest.
     */
    public static final ChunkerEntityProperty<Boolean> CHESTED = new ChunkerEntityProperty<>(
            "chested",
            Boolean.class
    );
    /**
     * The strength of a llama (its inventory size).
     */
    public static final ChunkerEntityProperty<Integer> STRENGTH = new ChunkerEntityProperty<>(
            "strength",
            Integer.class
    );
    /**
     * The inventory of the entity (slot to item): the chest of a donkey, mule or llama, or the items held by a
     * villager, piglin or allay.
     */
    public static final ChunkerEntityProperty<Map<Integer, ChunkerItemStack>> INVENTORY = new ChunkerEntityProperty<>(
            "inventory",
            new TypeToken<>() {
            }
    );
    /**
     * The home of the entity: the hive of a bee or the home beach of a turtle.
     */
    public static final ChunkerEntityProperty<ChunkerBlockPosition> HOME_POSITION = new ChunkerEntityProperty<>(
            "home_position",
            ChunkerBlockPosition.class
    );
    /**
     * Whether a bee carries nectar.
     */
    public static final ChunkerEntityProperty<Boolean> HAS_NECTAR = new ChunkerEntityProperty<>(
            "has_nectar",
            Boolean.class
    );
    /**
     * Whether an iron golem was built by a player.
     */
    public static final ChunkerEntityProperty<Boolean> PLAYER_CREATED = new ChunkerEntityProperty<>(
            "player_created",
            Boolean.class
    );
    /**
     * Whether a zombie can break doors.
     */
    public static final ChunkerEntityProperty<Boolean> CAN_BREAK_DOORS = new ChunkerEntityProperty<>(
            "can_break_doors",
            Boolean.class
    );
    /**
     * Whether a piglin doesn't hunt hoglins.
     */
    public static final ChunkerEntityProperty<Boolean> CANNOT_HUNT = new ChunkerEntityProperty<>(
            "cannot_hunt",
            Boolean.class
    );
    /**
     * The ticks until an allay can duplicate again.
     */
    public static final ChunkerEntityProperty<Long> DUPLICATION_COOLDOWN = new ChunkerEntityProperty<>(
            "duplication_cooldown",
            Long.class
    );
    /**
     * The ticks until a wandering trader or trader llama despawns.
     */
    public static final ChunkerEntityProperty<Integer> DESPAWN_DELAY = new ChunkerEntityProperty<>(
            "despawn_delay",
            Integer.class
    );
    /**
     * The profession of a villager or zombie villager.
     */
    public static final ChunkerEntityProperty<ChunkerVillagerProfession> VILLAGER_PROFESSION = new ChunkerEntityProperty<>(
            "villager_profession",
            ChunkerVillagerProfession.class
    );
    /**
     * The biome type of a villager or zombie villager.
     */
    public static final ChunkerEntityProperty<ChunkerVillagerType> VILLAGER_TYPE = new ChunkerEntityProperty<>(
            "villager_type",
            ChunkerVillagerType.class
    );
    /**
     * The trading level of a villager (1 novice to 5 master).
     */
    public static final ChunkerEntityProperty<Integer> VILLAGER_LEVEL = new ChunkerEntityProperty<>(
            "villager_level",
            Integer.class
    );
    /**
     * The trading experience of a villager.
     */
    public static final ChunkerEntityProperty<Integer> VILLAGER_XP = new ChunkerEntityProperty<>(
            "villager_xp",
            Integer.class
    );
    /**
     * The unlocked trades of a villager or wandering trader.
     */
    public static final ChunkerEntityProperty<List<ChunkerTradeOffer>> TRADE_OFFERS = new ChunkerEntityProperty<>(
            "trade_offers",
            new TypeToken<>() {
            }
    );
    /**
     * The points of interest a villager remembers (bed, bell, workstation).
     */
    public static final ChunkerEntityProperty<Map<ChunkerVillagerMemory, ChunkerGlobalPosition>> VILLAGER_MEMORIES = new ChunkerEntityProperty<>(
            "villager_memories",
            new TypeToken<>() {
            }
    );
    /**
     * The gossip a villager keeps about players, which sets their prices (e.g. the discount for curing it).
     */
    public static final ChunkerEntityProperty<List<ChunkerGossip>> VILLAGER_GOSSIPS = new ChunkerEntityProperty<>(
            "villager_gossips",
            new TypeToken<>() {
            }
    );
    /**
     * The size of a slime, magma cube or sulfur cube (1 is the smallest).
     */
    public static final ChunkerEntityProperty<Integer> SIZE = new ChunkerEntityProperty<>(
            "size",
            Integer.class
    );
    /**
     * The item stack of a dropped item.
     */
    public static final ChunkerEntityProperty<ChunkerItemStack> ITEM = new ChunkerEntityProperty<>(
            "item",
            ChunkerItemStack.class
    );
    /**
     * The experience an experience orb gives.
     */
    public static final ChunkerEntityProperty<Integer> EXPERIENCE_VALUE = new ChunkerEntityProperty<>(
            "experience_value",
            Integer.class
    );
    /**
     * The loot table which fills a container entity when it's first opened (Java identifier).
     */
    public static final ChunkerEntityProperty<String> LOOT_TABLE = new ChunkerEntityProperty<>(
            "loot_table",
            String.class
    );
    /**
     * The seed used with the loot table.
     */
    public static final ChunkerEntityProperty<Long> LOOT_TABLE_SEED = new ChunkerEntityProperty<>(
            "loot_table_seed",
            Long.class
    );
    /**
     * Whether a projectile is stuck in a block.
     */
    public static final ChunkerEntityProperty<Boolean> IN_GROUND = new ChunkerEntityProperty<>(
            "in_ground",
            Boolean.class
    );
    /**
     * Who can pick up an arrow: 0 nobody, 1 anyone, 2 only creative mode players.
     */
    public static final ChunkerEntityProperty<Integer> PICKUP = new ChunkerEntityProperty<>(
            "pickup",
            Integer.class
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

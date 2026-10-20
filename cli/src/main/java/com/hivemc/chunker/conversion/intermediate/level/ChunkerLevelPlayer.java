package com.hivemc.chunker.conversion.intermediate.level;

import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerGlobalPosition;
import com.hivemc.chunker.conversion.intermediate.column.entity.data.ChunkerMobEffect;
import com.hivemc.chunker.conversion.intermediate.world.Dimension;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectAVLTreeMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A player inside the world: the local player, and the players of a server world.
 * <p>
 * The fields after the game type are optional (null when the source doesn't have them).
 */
public class ChunkerLevelPlayer {
    private final Dimension dimension;
    private final double positionX;
    private final double positionY;
    private final double positionZ;
    private final double motionX;
    private final double motionY;
    private final double motionZ;
    private final float yaw;
    private final float pitch;
    private final Byte2ObjectAVLTreeMap<ChunkerItemStack> inventory;
    private final int gameType;
    private @Nullable String sourceId;
    private @Nullable UUID profileUUID;
    private @Nullable UUID accountUUID;
    private @Nullable Float health;
    private @Nullable Float absorption;
    private @Nullable Short air;
    private @Nullable Boolean onGround;
    private @Nullable Float fallDistance;
    private @Nullable Integer foodLevel;
    private @Nullable Float foodSaturation;
    private @Nullable Float foodExhaustion;
    private @Nullable Integer xpLevel;
    private @Nullable Float xpProgress;
    private @Nullable List<ChunkerMobEffect> effects;
    private @Nullable Byte2ObjectAVLTreeMap<ChunkerItemStack> enderChest;
    private @Nullable Integer selectedSlot;
    private @Nullable ChunkerPlayerAbilities abilities;
    private @Nullable ChunkerRespawnPoint respawnPoint;
    private @Nullable ChunkerGlobalPosition lastDeathLocation;

    /**
     * Create a new player.
     *
     * @param dimension the dimension the player is inside.
     * @param positionX the position of the feet in the X axis.
     * @param positionY the position of the feet in the Y axis.
     * @param positionZ the position of the feet in the Z axis.
     * @param motionX   the current motion in the X axis.
     * @param motionY   the current motion in the Y axis.
     * @param motionZ   the current motion in the Z axis.
     * @param yaw       the yaw rotation of the player.
     * @param pitch     the pitch rotation of the player.
     * @param inventory the players inventory (including armor and off-hand).
     * @param gameType  the current gamemode of the player.
     */
    public ChunkerLevelPlayer(Dimension dimension, double positionX, double positionY, double positionZ, double motionX, double motionY, double motionZ, float yaw, float pitch, Byte2ObjectAVLTreeMap<ChunkerItemStack> inventory, int gameType) {
        this.dimension = dimension;
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.inventory = inventory;
        this.gameType = gameType;
    }

    /**
     * Get the dimension the player is inside.
     *
     * @return the dimension.
     */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * Get the X position of the player.
     *
     * @return the X position of the player at feet level.
     */
    public double getPositionX() {
        return positionX;
    }

    /**
     * Get the Y position of the player.
     *
     * @return the Y position of the player at feet level.
     */
    public double getPositionY() {
        return positionY;
    }

    /**
     * Get the Z position of the player.
     *
     * @return the Z position of the player at feet level.
     */
    public double getPositionZ() {
        return positionZ;
    }

    /**
     * Get any applied motion in the X axis.
     *
     * @return any applied motion in the X.
     */
    public double getMotionX() {
        return motionX;
    }

    /**
     * Get any applied motion in the Y axis.
     *
     * @return any applied motion in the Y.
     */
    public double getMotionY() {
        return motionY;
    }

    /**
     * Get any applied motion in the Z axis.
     *
     * @return any applied motion in the Z.
     */
    public double getMotionZ() {
        return motionZ;
    }

    /**
     * Get the yaw rotation of the player.
     *
     * @return the yaw.
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Get the pitch rotation of the player.
     *
     * @return the pitch.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Get the contents of the inventory ordered by slot as a byte.
     * Where 100-103 is the armor and 150 is the off-hand.
     *
     * @return the inventory as a map.
     */
    public Byte2ObjectAVLTreeMap<ChunkerItemStack> getInventory() {
        return inventory;
    }

    /**
     * Get the gamemode of the player as an integer.
     *
     * @return the gamemode of the player.
     */
    public int getGameType() {
        return gameType;
    }

    /**
     * Get the ID of the player in the source world (for Bedrock, the UniqueID of the player record).
     *
     * @return the ID or null if the source has none.
     */
    @Nullable
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Set the ID of the player in the source world.
     *
     * @param sourceId the ID.
     */
    public void setSourceId(@Nullable String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Get the UUID the player's profile is named after, which references to the player use when no account is
     * assigned. It matches no real account.
     *
     * @return the UUID or null if the player has no profile.
     */
    @Nullable
    public UUID getProfileUUID() {
        return profileUUID;
    }

    /**
     * Set the UUID the player's profile is named after.
     *
     * @param profileUUID the UUID.
     */
    public void setProfileUUID(@Nullable UUID profileUUID) {
        this.profileUUID = profileUUID;
    }

    /**
     * Get the UUID of the account the player is assigned to, which loads the player's data when it joins.
     *
     * @return the UUID or null if no account is assigned.
     */
    @Nullable
    public UUID getAccountUUID() {
        return accountUUID;
    }

    /**
     * Set the UUID of the account the player is assigned to.
     *
     * @param accountUUID the UUID or null.
     */
    public void setAccountUUID(@Nullable UUID accountUUID) {
        this.accountUUID = accountUUID;
    }

    /**
     * Get the health of the player.
     *
     * @return the health or null if unknown.
     */
    @Nullable
    public Float getHealth() {
        return health;
    }

    /**
     * Set the health of the player.
     *
     * @param health the health.
     */
    public void setHealth(@Nullable Float health) {
        this.health = health;
    }

    /**
     * Get the absorption (extra health) of the player.
     *
     * @return the absorption or null if unknown.
     */
    @Nullable
    public Float getAbsorption() {
        return absorption;
    }

    /**
     * Set the absorption of the player.
     *
     * @param absorption the absorption.
     */
    public void setAbsorption(@Nullable Float absorption) {
        this.absorption = absorption;
    }

    /**
     * Get the air supply of the player in ticks.
     *
     * @return the air or null if unknown.
     */
    @Nullable
    public Short getAir() {
        return air;
    }

    /**
     * Set the air supply of the player in ticks.
     *
     * @param air the air.
     */
    public void setAir(@Nullable Short air) {
        this.air = air;
    }

    /**
     * Get whether the player is on the ground.
     *
     * @return true if on the ground, null if unknown.
     */
    @Nullable
    public Boolean getOnGround() {
        return onGround;
    }

    /**
     * Set whether the player is on the ground.
     *
     * @param onGround true if on the ground.
     */
    public void setOnGround(@Nullable Boolean onGround) {
        this.onGround = onGround;
    }

    /**
     * Get the distance the player has fallen so far.
     *
     * @return the distance in blocks or null if unknown.
     */
    @Nullable
    public Float getFallDistance() {
        return fallDistance;
    }

    /**
     * Set the distance the player has fallen so far.
     *
     * @param fallDistance the distance in blocks.
     */
    public void setFallDistance(@Nullable Float fallDistance) {
        this.fallDistance = fallDistance;
    }

    /**
     * Get the food level of the player (0 to 20).
     *
     * @return the food level or null if unknown.
     */
    @Nullable
    public Integer getFoodLevel() {
        return foodLevel;
    }

    /**
     * Set the food level of the player.
     *
     * @param foodLevel the food level.
     */
    public void setFoodLevel(@Nullable Integer foodLevel) {
        this.foodLevel = foodLevel;
    }

    /**
     * Get the food saturation of the player.
     *
     * @return the saturation or null if unknown.
     */
    @Nullable
    public Float getFoodSaturation() {
        return foodSaturation;
    }

    /**
     * Set the food saturation of the player.
     *
     * @param foodSaturation the saturation.
     */
    public void setFoodSaturation(@Nullable Float foodSaturation) {
        this.foodSaturation = foodSaturation;
    }

    /**
     * Get the food exhaustion of the player.
     *
     * @return the exhaustion or null if unknown.
     */
    @Nullable
    public Float getFoodExhaustion() {
        return foodExhaustion;
    }

    /**
     * Set the food exhaustion of the player.
     *
     * @param foodExhaustion the exhaustion.
     */
    public void setFoodExhaustion(@Nullable Float foodExhaustion) {
        this.foodExhaustion = foodExhaustion;
    }

    /**
     * Get the experience level of the player.
     *
     * @return the level or null if unknown.
     */
    @Nullable
    public Integer getXpLevel() {
        return xpLevel;
    }

    /**
     * Set the experience level of the player.
     *
     * @param xpLevel the level.
     */
    public void setXpLevel(@Nullable Integer xpLevel) {
        this.xpLevel = xpLevel;
    }

    /**
     * Get the progress towards the next experience level (0 to 1).
     *
     * @return the progress or null if unknown.
     */
    @Nullable
    public Float getXpProgress() {
        return xpProgress;
    }

    /**
     * Set the progress towards the next experience level.
     *
     * @param xpProgress the progress.
     */
    public void setXpProgress(@Nullable Float xpProgress) {
        this.xpProgress = xpProgress;
    }

    /**
     * Get the active effects of the player.
     *
     * @return the effects or null if unknown.
     */
    @Nullable
    public List<ChunkerMobEffect> getEffects() {
        return effects;
    }

    /**
     * Set the active effects of the player.
     *
     * @param effects the effects.
     */
    public void setEffects(@Nullable List<ChunkerMobEffect> effects) {
        this.effects = effects;
    }

    /**
     * Get the contents of the ender chest ordered by slot (0 to 26).
     *
     * @return the ender chest or null if unknown.
     */
    @Nullable
    public Byte2ObjectAVLTreeMap<ChunkerItemStack> getEnderChest() {
        return enderChest;
    }

    /**
     * Set the contents of the ender chest.
     *
     * @param enderChest the ender chest ordered by slot.
     */
    public void setEnderChest(@Nullable Byte2ObjectAVLTreeMap<ChunkerItemStack> enderChest) {
        this.enderChest = enderChest;
    }

    /**
     * Get the selected hotbar slot (0 to 8).
     *
     * @return the slot or null if unknown.
     */
    @Nullable
    public Integer getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Set the selected hotbar slot.
     *
     * @param selectedSlot the slot.
     */
    public void setSelectedSlot(@Nullable Integer selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    /**
     * Get the abilities of the player.
     *
     * @return the abilities or null if unknown.
     */
    @Nullable
    public ChunkerPlayerAbilities getAbilities() {
        return abilities;
    }

    /**
     * Set the abilities of the player.
     *
     * @param abilities the abilities.
     */
    public void setAbilities(@Nullable ChunkerPlayerAbilities abilities) {
        this.abilities = abilities;
    }

    /**
     * Get the point where the player respawns.
     *
     * @return the respawn point or null if the player uses the world spawn.
     */
    @Nullable
    public ChunkerRespawnPoint getRespawnPoint() {
        return respawnPoint;
    }

    /**
     * Set the point where the player respawns.
     *
     * @param respawnPoint the respawn point or null for the world spawn.
     */
    public void setRespawnPoint(@Nullable ChunkerRespawnPoint respawnPoint) {
        this.respawnPoint = respawnPoint;
    }

    /**
     * Get the location where the player last died.
     *
     * @return the location or null if the player never died.
     */
    @Nullable
    public ChunkerGlobalPosition getLastDeathLocation() {
        return lastDeathLocation;
    }

    /**
     * Set the location where the player last died.
     *
     * @param lastDeathLocation the location or null.
     */
    public void setLastDeathLocation(@Nullable ChunkerGlobalPosition lastDeathLocation) {
        this.lastDeathLocation = lastDeathLocation;
    }

    @Override
    public String toString() {
        return "ChunkerLevelPlayer{" +
                "dimension=" + getDimension() +
                ", positionX=" + getPositionX() +
                ", positionY=" + getPositionY() +
                ", positionZ=" + getPositionZ() +
                ", motionX=" + getMotionX() +
                ", motionY=" + getMotionY() +
                ", motionZ=" + getMotionZ() +
                ", yaw=" + getYaw() +
                ", pitch=" + getPitch() +
                ", inventory=" + getInventory() +
                ", gameType=" + getGameType() +
                ", sourceId=" + getSourceId() +
                ", profileUUID=" + getProfileUUID() +
                ", accountUUID=" + getAccountUUID() +
                ", health=" + getHealth() +
                ", absorption=" + getAbsorption() +
                ", air=" + getAir() +
                ", onGround=" + getOnGround() +
                ", fallDistance=" + getFallDistance() +
                ", foodLevel=" + getFoodLevel() +
                ", foodSaturation=" + getFoodSaturation() +
                ", foodExhaustion=" + getFoodExhaustion() +
                ", xpLevel=" + getXpLevel() +
                ", xpProgress=" + getXpProgress() +
                ", effects=" + getEffects() +
                ", enderChest=" + getEnderChest() +
                ", selectedSlot=" + getSelectedSlot() +
                ", abilities=" + getAbilities() +
                ", respawnPoint=" + getRespawnPoint() +
                ", lastDeathLocation=" + getLastDeathLocation() +
                '}';
    }
}

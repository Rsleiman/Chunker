package com.hivemc.chunker.conversion.intermediate.level;

/**
 * The abilities of a player.
 *
 * @param mayFly       whether the player may fly.
 * @param flying       whether the player is flying.
 * @param instabuild   whether the player breaks blocks instantly and has unlimited items (creative).
 * @param invulnerable whether the player can't be damaged.
 * @param mayBuild     whether the player may place and break blocks.
 * @param walkSpeed    the walking speed.
 * @param flySpeed     the flying speed.
 */
public record ChunkerPlayerAbilities(boolean mayFly, boolean flying, boolean instabuild, boolean invulnerable,
                                     boolean mayBuild, float walkSpeed, float flySpeed) {
}

package com.volcanicruins.thirst;

/**
 * Tunable numbers for the thirst system. Times are in ticks (20 ticks = 1 second).
 * The drain rate itself is changeable in-game with /thirst drainrate and saved to
 * config/volcanic_thirst.json; the value here is only the starting default.
 */
public final class ThirstConfig {
	private ThirstConfig() {}

	/** Thirst meter range. 100 = fully hydrated, 0 = dehydrated. */
	public static final int MAX_THIRST = 100;

	/** Default: a full meter lasts this many minutes of normal play (1% every 18s). */
	public static final double DEFAULT_FULL_METER_MINUTES = 30.0;
	/** Sprinting drains thirst this many times faster. */
	public static final float SPRINT_DRAIN_MULTIPLIER = 2.0F;
	/** Below this thirst % the player cannot sprint (like vanilla hunger at 3 drumsticks). */
	public static final int NO_SPRINT_BELOW = 20;

	/** Length of the Hydration effect from drinking or standing near a hydrating biome (60 seconds). */
	public static final int HYDRATION_DURATION_TICKS = 60 * 20;

	// --- Unpurified water (vanilla water bottle) ---
	public static final int UNPURIFIED_THIRST_RESTORE = 10;
	/** Rotten-flesh style: chance to get Hunger when drinking unpurified water. */
	public static final float UNPURIFIED_HUNGER_CHANCE = 0.30F;
	public static final int UNPURIFIED_HUNGER_DURATION_TICKS = 30 * 20;
	/** Chance to get Nausea when drinking unpurified water. */
	public static final float UNPURIFIED_NAUSEA_CHANCE = 0.15F;
	public static final int UNPURIFIED_NAUSEA_DURATION_TICKS = 10 * 20;

	// --- Purified water (brewed with blaze powder) ---
	public static final int PURIFIED_THIRST_RESTORE = 25;
	/** Hydration II (purified) also slowly restores 1% every this many ticks (6s => +10% over the full 60s). */
	public static final int PURIFIED_REGEN_INTERVAL_TICKS = 120;

	// --- Hydrating biome ---
	/** How often to check whether a player is near a hydrating biome. */
	public static final int BIOME_CHECK_INTERVAL_TICKS = 40;
	/** "Near" = the player's own position or any point this many blocks away in 8 directions. */
	public static final int BIOME_CHECK_RADIUS = 12;

	// --- Dehydration ---
	/** At 0% thirst the player takes damage every this many ticks. */
	public static final int DEHYDRATION_DAMAGE_INTERVAL_TICKS = 80;
	public static final float DEHYDRATION_DAMAGE = 1.0F;
}

package com.volcanicruins.thirst;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public final class ThirstHandler {
	private ThirstHandler() {}

	/** Fractional drain progress per player (fractions of 1% not yet subtracted). */
	private static final Map<UUID, Double> DRAIN_PROGRESS = new HashMap<>();

	// ---------------------------------------------------------------------------------------------
	// Thirst value helpers
	// ---------------------------------------------------------------------------------------------

	public static int getThirst(ServerPlayer player) {
		return player.getAttachedOrCreate(ModRegistry.THIRST);
	}

	public static void setThirst(ServerPlayer player, int value) {
		int clamped = Mth.clamp(value, 0, ThirstConfig.MAX_THIRST);
		if (clamped != getThirst(player)) {
			player.setAttached(ModRegistry.THIRST, clamped); // auto-synced to the player's HUD
		}
	}

	public static void addThirst(ServerPlayer player, int amount) {
		setThirst(player, getThirst(player) + amount);
	}

	// ---------------------------------------------------------------------------------------------
	// Per-tick logic
	// ---------------------------------------------------------------------------------------------

	public static void tickLevel(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			tickPlayer(level, player);
		}
	}

	private static void tickPlayer(ServerLevel level, ServerPlayer player) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
			return;
		}

		int tick = player.tickCount;

		// Near a hydrating biome -> Hydration I for 60s (refreshed while you stay near it).
		if (tick % ThirstConfig.BIOME_CHECK_INTERVAL_TICKS == 0 && isNearHydratingBiome(level, player)) {
			giveHydration(player, 0);
		}

		MobEffectInstance hydration = player.getEffect(ModRegistry.HYDRATION);
		int thirst = getThirst(player);

		if (hydration == null) {
			// Normal drain, at the rate for the biome the player is standing in (or the default)
			double minutes = ThirstSettings.minutesFor(currentBiomeId(level, player));
			if (minutes > 0) {
				double ticksPerPercent = minutes * 60.0 * 20.0 / ThirstConfig.MAX_THIRST;
				float speed = player.isSprinting() ? ThirstConfig.SPRINT_DRAIN_MULTIPLIER : 1F;
				// progress counts fractions of 1%; whole numbers get subtracted from thirst
				double progress = DRAIN_PROGRESS.getOrDefault(player.getUUID(), 0.0) + speed / ticksPerPercent;
				int whole = (int) progress;
				thirst -= whole;
				DRAIN_PROGRESS.put(player.getUUID(), progress - whole);
			}
		} else if (hydration.getAmplifier() >= 1 && tick % ThirstConfig.PURIFIED_REGEN_INTERVAL_TICKS == 0) {
			// Hydration II (purified): no drain, plus a slow trickle back up
			thirst++;
		}
		// Hydration I: no drain, no regen

		setThirst(player, thirst);

		// Too thirsty to sprint. The client is also blocked (LocalPlayerMixin); this is the server-side backstop.
		if (getThirst(player) < ThirstConfig.NO_SPRINT_BELOW && player.isSprinting()) {
			player.setSprinting(false);
		}

		if (getThirst(player) <= 0 && tick % ThirstConfig.DEHYDRATION_DAMAGE_INTERVAL_TICKS == 0) {
			player.hurtServer(level, level.damageSources().starve(), ThirstConfig.DEHYDRATION_DAMAGE);
		}
	}

	/** e.g. "minecraft:desert", or null if the biome has no id. */
	public static String currentBiomeId(ServerLevel level, ServerPlayer player) {
		return level.getBiome(player.blockPosition()).unwrapKey()
				.map(key -> key.identifier().toString())
				.orElse(null);
	}

	private static boolean isNearHydratingBiome(ServerLevel level, ServerPlayer player) {
		BlockPos center = player.blockPosition();
		if (level.getBiome(center).is(ModRegistry.HYDRATING_BIOMES)) {
			return true;
		}
		int r = ThirstConfig.BIOME_CHECK_RADIUS;
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0;
			BlockPos pos = center.offset((int) Math.round(Math.cos(angle) * r), 0, (int) Math.round(Math.sin(angle) * r));
			if (level.isLoaded(pos) && level.getBiome(pos).is(ModRegistry.HYDRATING_BIOMES)) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------------------------------
	// Drinking (called from ItemStackMixin when a player finishes drinking anything)
	// ---------------------------------------------------------------------------------------------

	public static void onFinishUsing(ServerPlayer player, ItemStack stack) {
		if (!stack.is(Items.POTION)) {
			return;
		}
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) {
			return;
		}
		Optional<Holder<Potion>> potion = contents.potion();
		if (potion.isEmpty()) {
			return;
		}
		Potion drank = potion.get().value();

		if (drank == ModRegistry.PURIFIED_WATER.value()) {
			addThirst(player, ThirstConfig.PURIFIED_THIRST_RESTORE);
			giveHydration(player, 1);
		} else if (drank == Potions.WATER.value()) {
			addThirst(player, ThirstConfig.UNPURIFIED_THIRST_RESTORE);
			giveHydration(player, 0);

			// Rotten-flesh style gamble
			if (player.getRandom().nextFloat() < ThirstConfig.UNPURIFIED_HUNGER_CHANCE) {
				player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ThirstConfig.UNPURIFIED_HUNGER_DURATION_TICKS, 0));
			}
			if (player.getRandom().nextFloat() < ThirstConfig.UNPURIFIED_NAUSEA_CHANCE) {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ThirstConfig.UNPURIFIED_NAUSEA_DURATION_TICKS, 0));
			}
		}
	}

	/** amplifier 0 = Hydration I, 1 = Hydration II. Vanilla keeps the stronger one if both are applied. */
	private static void giveHydration(ServerPlayer player, int amplifier) {
		player.addEffect(new MobEffectInstance(ModRegistry.HYDRATION, ThirstConfig.HYDRATION_DURATION_TICKS, amplifier));
	}
}

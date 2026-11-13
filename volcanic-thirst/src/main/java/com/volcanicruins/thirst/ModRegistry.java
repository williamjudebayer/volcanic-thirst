package com.volcanicruins.thirst;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.biome.Biome;

public final class ModRegistry {
	private ModRegistry() {}

	/** Hydration I = from unpurified water / hydrating biome. Hydration II = from purified water. */
	public static final Holder<MobEffect> HYDRATION = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, VolcanicThirst.id("hydration"), new HydrationEffect());

	/** The only brewable potion: a water bottle with no effects, drinkable for a bigger thirst restore. */
	public static final Holder<Potion> PURIFIED_WATER = Registry.registerForHolder(
			BuiltInRegistries.POTION, VolcanicThirst.id("purified_water"), new Potion("purified_water"));

	/**
	 * Each player's thirst %, saved with the player and synced to that player's client for the HUD.
	 * Not copied on death, so respawning resets to 100%.
	 */
	public static final AttachmentType<Integer> THIRST = AttachmentRegistry.create(
			VolcanicThirst.id("thirst"),
			builder -> builder
					.persistent(Codec.INT)
					.initializer(() -> ThirstConfig.MAX_THIRST)
					.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/**
	 * Biomes that hydrate nearby players. Defined in
	 * data/volcanic_thirst/tags/worldgen/biome/hydrating_biomes.json (empty until the biome is chosen).
	 */
	public static final TagKey<Biome> HYDRATING_BIOMES =
			TagKey.create(Registries.BIOME, VolcanicThirst.id("hydrating_biomes"));

	/** Forces class loading so the static registrations above run during mod init. */
	public static void init() {}
}

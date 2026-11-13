package com.volcanicruins.thirst;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.volcanicruins.thirst.mixin.PotionBrewingBuilderAccessor;

public class VolcanicThirst implements ModInitializer {
	public static final String MOD_ID = "volcanic_thirst";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModRegistry.init();
		ThirstSettings.load();

		// Brewing: wipe every vanilla mix so purified water is the only thing a brewing stand can make.
		// Fabric fires this right before the brewing registry is built, after vanilla mixes were added.
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			PotionBrewingBuilderAccessor accessor = (PotionBrewingBuilderAccessor) builder;
			accessor.volcanicThirst$getPotionMixes().clear();
			accessor.volcanicThirst$getContainerMixes().clear(); // no splash/lingering conversions either

			// Water bottle + blaze powder (ingredient slot) -> Purified Water
			((FabricBrewingRecipeRegistryBuilder) builder).registerPotionRecipe(
					Potions.WATER, Ingredient.of(Items.BLAZE_POWDER), ModRegistry.PURIFIED_WATER);
		});
		// Magma cream as brewing fuel is done in data: data/minecraft/tags/item/brewing_fuel.json

		ServerTickEvents.END_WORLD_TICK.register(ThirstHandler::tickLevel);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				ThirstCommands.register(dispatcher));

		LOGGER.info("Volcanic Thirst loaded");
	}
}

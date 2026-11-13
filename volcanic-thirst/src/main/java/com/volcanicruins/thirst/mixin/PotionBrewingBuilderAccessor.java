package com.volcanicruins.thirst.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.alchemy.PotionBrewing;

/**
 * Lets us clear vanilla brewing mixes so only purified water can be brewed.
 * PotionBrewing.Mix is private, so the lists are exposed as List<?> (we only ever call clear()).
 */
@Mixin(PotionBrewing.Builder.class)
public interface PotionBrewingBuilderAccessor {
	@Accessor("potionMixes")
	List<?> volcanicThirst$getPotionMixes();

	@Accessor("containerMixes")
	List<?> volcanicThirst$getContainerMixes();
}

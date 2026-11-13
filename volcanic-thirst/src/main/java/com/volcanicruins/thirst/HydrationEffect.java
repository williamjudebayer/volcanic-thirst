package com.volcanicruins.thirst;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker effect. The behavior lives in {@link ThirstHandler}:
 * level I stops thirst from draining, level II also slowly restores thirst.
 */
public class HydrationEffect extends MobEffect {
	public HydrationEffect() {
		super(MobEffectCategory.BENEFICIAL, 0x3FA9F5);
	}
}

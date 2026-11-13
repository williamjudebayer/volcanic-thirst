package com.volcanicruins.thirst.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.volcanicruins.thirst.VolcanicThirst;

public class VolcanicThirstClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.FOOD_BAR, VolcanicThirst.id("thirst_bar"), ThirstHud::render);
	}
}

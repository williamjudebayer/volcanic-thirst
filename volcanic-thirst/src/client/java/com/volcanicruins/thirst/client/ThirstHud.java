package com.volcanicruins.thirst.client;

import java.util.Random;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.volcanicruins.thirst.ModRegistry;
import com.volcanicruins.thirst.ThirstConfig;
import com.volcanicruins.thirst.VolcanicThirst;

/**
 * Hunger-bar style thirst meter: 10 water droplets on the right side, directly above the hunger bar.
 * Each droplet = 10%, half droplet = 5%. Droplets turn cyan while Hydration is active and
 * jiggle when thirst is too low to sprint, like the hunger bar does.
 * Moves up a row when air bubbles are showing, and above mount hearts when riding.
 */
public final class ThirstHud {
	private ThirstHud() {}

	private static final Identifier EMPTY = VolcanicThirst.id("hud/thirst_empty");
	private static final Identifier FULL = VolcanicThirst.id("hud/thirst_full");
	private static final Identifier HALF = VolcanicThirst.id("hud/thirst_half");
	private static final Identifier FULL_HYDRATED = VolcanicThirst.id("hud/thirst_full_hydrated");
	private static final Identifier HALF_HYDRATED = VolcanicThirst.id("hud/thirst_half_hydrated");

	private static final Random JIGGLE = new Random();

	public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
			return; // hidden whenever vanilla hides hunger (creative/spectator, F1)
		}

		int thirst = player.getAttachedOrElse(ModRegistry.THIRST, ThirstConfig.MAX_THIRST);
		boolean hydrated = player.hasEffect(ModRegistry.HYDRATION);

		int right = graphics.guiWidth() / 2 + 91;
		int y = graphics.guiHeight() - 39 - 10; // one row above the hunger bar

		// Riding a mob with health: vanilla swaps the hunger bar for mount hearts (possibly several rows)
		if (player.getVehicle() instanceof LivingEntity mount) {
			int hearts = Math.min((int) (mount.getMaxHealth() + 0.5F) / 2, 30);
			int rows = Math.max(1, Mth.ceil(hearts / 10.0F));
			y = graphics.guiHeight() - 39 - rows * 10;
		}

		// Air bubbles take the row above hunger while underwater / recovering air
		if (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
			y -= 10;
		}

		// Units of 5%: 20 = full bar. Round up so any thirst left still shows half a droplet.
		int units = Mth.ceil(thirst / 5.0F);
		boolean jiggle = thirst < ThirstConfig.NO_SPRINT_BELOW;
		JIGGLE.setSeed(player.tickCount * 312871L);

		for (int i = 0; i < 10; i++) {
			int x = right - i * 8 - 9;
			int iconY = y;
			if (jiggle && player.tickCount % (units * 3 + 1) == 0) {
				iconY += JIGGLE.nextInt(3) - 1;
			}

			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, x, iconY, 9, 9);
			if (i * 2 + 1 < units) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hydrated ? FULL_HYDRATED : FULL, x, iconY, 9, 9);
			} else if (i * 2 + 1 == units) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hydrated ? HALF_HYDRATED : HALF, x, iconY, 9, 9);
			}
		}
	}
}

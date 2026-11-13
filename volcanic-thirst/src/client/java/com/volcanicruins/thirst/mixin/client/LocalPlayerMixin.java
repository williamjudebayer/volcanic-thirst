package com.volcanicruins.thirst.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;

import com.volcanicruins.thirst.ModRegistry;
import com.volcanicruins.thirst.ThirstConfig;

/**
 * Vanilla's sprint check (it's where low hunger blocks sprinting). Returning false here both
 * prevents starting a sprint and stops one in progress, exactly like having 3 or fewer drumsticks.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "isSprintingPossible", at = @At("HEAD"), cancellable = true)
	private void volcanicThirst$tooThirstyToSprint(boolean allowedInWater, CallbackInfoReturnable<Boolean> cir) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (self.isCreative() || self.isSpectator()) {
			return;
		}
		if (self.getAttachedOrElse(ModRegistry.THIRST, ThirstConfig.MAX_THIRST) < ThirstConfig.NO_SPRINT_BELOW) {
			cir.setReturnValue(false);
		}
	}
}

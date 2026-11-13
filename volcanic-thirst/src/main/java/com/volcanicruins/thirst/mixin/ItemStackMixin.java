package com.volcanicruins.thirst.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.volcanicruins.thirst.ThirstHandler;

/** Detects when a player finishes drinking, while the stack still holds its potion contents. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "finishUsingItem", at = @At("HEAD"))
	private void volcanicThirst$onFinishUsing(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
		if (entity instanceof ServerPlayer player) {
			ThirstHandler.onFinishUsing(player, (ItemStack) (Object) this);
		}
	}
}

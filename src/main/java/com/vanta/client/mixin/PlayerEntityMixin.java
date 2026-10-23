package com.vanta.client.mixin;

import com.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
    @Inject(method = "getAttackCooldownProgress(F)F", at = @At("RETURN"), cancellable = true)
    private void vanta$noHitDelay(float baseTime, CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((Object) this == client.player && VantaClient.isModuleEnabled("NO_HIT_DELAY")) {
            cir.setReturnValue(1.0f);
        }
    }
}


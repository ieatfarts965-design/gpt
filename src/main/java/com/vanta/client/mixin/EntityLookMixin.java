package com.vanta.client.mixin;

import com.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityLookMixin {
    @Inject(method = "changeLookDirection(DD)V", at = @At("HEAD"), cancellable = true)
    private void vanta$freeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((Object) this == client.player
                && (VantaClient.isModuleEnabled("FREECAM") || VantaClient.isModuleEnabled("FREE_LOOK"))
                && VantaClient.getFreecamCamera() != null) {
            VantaClient.getFreecamCamera().changeLookDirection(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}


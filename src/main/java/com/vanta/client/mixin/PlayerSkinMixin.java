package com.vanta.client.mixin;

import com.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
abstract class PlayerSkinMixin {
    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("RETURN"), cancellable = true)
    private void vanta$replaceOwnSkin(AbstractClientPlayerEntity player,
                                      CallbackInfoReturnable<Identifier> cir) {
        if (player == MinecraftClient.getInstance().player && VantaClient.isModuleEnabled("SKIN_PROTECT")) {
            cir.setReturnValue(Identifier.of("minecraft", "textures/entity/player/wide/steve.png"));
        }
    }
}


package com.vanta.client.mixin;

import com.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;",
            at = @At("RETURN"), cancellable = true)
    private void vanta$maskTabName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (VantaClient.isModuleEnabled("NAME_PROTECT") && client.getSession() != null
                && entry.getProfile().getName().equalsIgnoreCase(client.getSession().getUsername())) {
            cir.setReturnValue(Text.literal("Vanta"));
        }
    }
}


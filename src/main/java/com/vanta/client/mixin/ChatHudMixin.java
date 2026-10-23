package com.vanta.client.mixin;

import com.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
abstract class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text vanta$maskSimpleChat(Text message) {
        return maskName(message);
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private Text vanta$maskSignedChat(Text message) {
        return maskName(message);
    }

    private Text maskName(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VantaClient.isModuleEnabled("NAME_PROTECT") || client.getSession() == null) return message;
        String ownName = client.getSession().getUsername();
        String original = message.getString();
        if (ownName.isBlank() || !original.contains(ownName)) return message;
        return Text.literal(original.replace(ownName, "Vanta"));
    }
}


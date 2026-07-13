package net.pcal.fwportals.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F"
            )
    )
    private float fwportals$suppressBlockedPortalEffectInTick(LocalPlayer player) {
        return ForeverWorldPortalsClient.getInstance().shouldSuppressPortalEffect(player) ? 0.0F : player.portalEffectIntensity;
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;oPortalEffectIntensity:F"
            )
    )
    private float fwportals$suppressBlockedOldPortalEffectInRender(LocalPlayer player) {
        return ForeverWorldPortalsClient.getInstance().shouldSuppressPortalEffect(player) ? 0.0F : player.oPortalEffectIntensity;
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F"
            )
    )
    private float fwportals$suppressBlockedPortalEffectInRender(LocalPlayer player) {
        return ForeverWorldPortalsClient.getInstance().shouldSuppressPortalEffect(player) ? 0.0F : player.portalEffectIntensity;
    }
}

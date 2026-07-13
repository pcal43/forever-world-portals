package net.pcal.fwportals.client.mixins;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.pcal.fwportals.client.ClientService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {

    @Redirect(
            method = "handlePortalTransitionEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"
            )
    )
    private SoundEngine.PlayResult fwportals$suppressBlockedPortalSound(SoundManager soundManager, SoundInstance soundInstance) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (ClientService.getInstance().shouldSuppressPortalEffect(player)) {
            return null;
        }

        return soundManager.play(soundInstance);
    }
}

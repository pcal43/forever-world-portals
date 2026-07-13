package net.pcal.fwportals.client.mixin;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.client.ForeverWorldPortalTintSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockColors.class)
abstract class BlockColorsMixin {

    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void fwportals$registerPortalTintSource(CallbackInfoReturnable<BlockColors> cir) {
        cir.getReturnValue().register(List.of(ForeverWorldPortalTintSource.INSTANCE), Blocks.NETHER_PORTAL);
    }
}

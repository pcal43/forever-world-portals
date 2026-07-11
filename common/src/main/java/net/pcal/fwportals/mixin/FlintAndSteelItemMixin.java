package net.pcal.fwportals.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.pcal.fwportals.ForeverWorldPortalsService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void maybeActivateForeverWorldPortal(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) {
            return;
        }

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos firePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(firePos).is(Blocks.FIRE)) {
            return;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return;
        }

        ForeverWorldPortalsService.getInstance().tryActivatePortal(level, firePos, context.getItemInHand(), player);
    }
}

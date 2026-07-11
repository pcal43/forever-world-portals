package net.pcal.fwportals.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.pcal.fwportals.ForeverWorldPortalsService;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void keepForeverWorldPortalBlocksWhenDiamondFrameIsIntact(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (cir.getReturnValue().isAir() && ForeverWorldPortalsService.getInstance().isForeverWorldPortal(level, pos)) {
            cir.setReturnValue(state);
        }
    }

    @Inject(method = "entityInside", at = @At("HEAD"))
    private void recordForeverWorldPortalEntry(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        ForeverWorldPortalsService.getInstance().onEntityInsidePortal(level, pos, entity);
    }

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaTeleportForForeverWorldPortals(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalEntryPos,
            CallbackInfoReturnable<@Nullable TeleportTransition> cir
    ) {
        if (ForeverWorldPortalsService.getInstance().isForeverWorldPortal(currentLevel, portalEntryPos)) {
            cir.setReturnValue(ForeverWorldPortalsService.getInstance().getTeleportTransitionForPortal(currentLevel, entity, portalEntryPos));
        }
    }
}

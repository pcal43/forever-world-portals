package net.pcal.fwportals.common.mixins;

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
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.pcal.fwportals.CommonService;
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
        if (cir.getReturnValue().isAir() && CommonService.getInstance().isForeverWorldPortal(level, pos)) {
            cir.setReturnValue(state);
        }
    }

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void recordForeverWorldPortalEntry(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (!CommonService.getInstance().handleEntityInsidePortal(level, pos, entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "getPortalTransitionTime", at = @At("RETURN"), cancellable = true)
    private void useStandardPlayerPortalDelayForForeverWorldPortals(
            ServerLevel level,
            Entity entity,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (entity.portalProcess == null) {
            return;
        }

        BlockPos entryPos = entity.portalProcess.getEntryPosition();
        if (!CommonService.getInstance().isForeverWorldPortal(level, entryPos)) {
            return;
        }

        if (entity instanceof net.minecraft.server.level.ServerPlayer player
                && !CommonService.getInstance().canPlayerUseForeverWorldPortal(player)) {
            cir.setReturnValue(Integer.MAX_VALUE);
            return;
        }

        cir.setReturnValue(Math.max(1, level.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)));
    }

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaTeleportForForeverWorldPortals(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalEntryPos,
            CallbackInfoReturnable<@Nullable TeleportTransition> cir
    ) {
        if (CommonService.getInstance().isForeverWorldPortal(currentLevel, portalEntryPos)) {
            cir.setReturnValue(CommonService.getInstance().getTeleportTransitionForPortal(currentLevel, entity, portalEntryPos));
        }
    }
}

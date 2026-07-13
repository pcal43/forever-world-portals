package net.pcal.fwportals.common.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.pcal.fwportals.ForeverWorldPortalsService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void activateForeverWorldPortalBeforeVanillaFirePlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos firePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(firePos).isAir()) {
            ForeverWorldPortalsService.getInstance().logger().info(
                    ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin skipped: target {} is {} on {}",
                    firePos,
                    level.getBlockState(firePos).getBlock().getName().getString(),
                    level.isClientSide() ? "client" : "server"
            );
            return;
        }

        ItemStack itemStack = context.getItemInHand();
        ForeverWorldPortalsService.getInstance().logger().info(
                ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin checking portal activation at {} from clickedPos={} face={} on {}",
                firePos,
                context.getClickedPos(),
                context.getClickedFace(),
                level.isClientSide() ? "client" : "server"
        );
        if (!ForeverWorldPortalsService.getInstance().canActivatePortalAt(level, firePos, itemStack)) {
            ForeverWorldPortalsService.getInstance().logger().info(
                    ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin found no valid Forever World portal frame at {} on {}",
                    firePos,
                    level.isClientSide() ? "client" : "server"
            );
            return;
        }

        if (level.isClientSide()) {
            ForeverWorldPortalsService.getInstance().logger().info(
                    ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin accepted activation client-side at {}",
                    firePos
            );
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        Player player = context.getPlayer();
        if (player == null) {
            ForeverWorldPortalsService.getInstance().logger().info(
                    ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin found valid frame at {} but no player was present",
                    firePos
            );
            return;
        }

        if (!ForeverWorldPortalsService.getInstance().tryActivatePortal(level, firePos, itemStack, player)) {
            ForeverWorldPortalsService.getInstance().logger().info(
                    ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin failed to activate portal server-side at {}",
                    firePos
            );
            return;
        }

        ForeverWorldPortalsService.getInstance().logger().info(
                ForeverWorldPortalsService.LOG_PREFIX + "FlintAndSteel mixin activated portal server-side at {}",
                firePos
        );
        level.playSound(player, firePos, net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, firePos);
        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraft.advancements.triggers.CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, firePos, itemStack);
        }
        itemStack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}

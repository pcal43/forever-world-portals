package net.pcal.fwportals.client.mixins;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.client.ClientService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Shared portal rendering hook for the vanilla block model renderer.
 *
 * <p>This mixin redirects vanilla {@code ModelBlockRenderer} so Forever World portal blocks can
 * substitute a tinted/masked portal model while leaving ordinary Nether portals unchanged. This is
 * the common cross-loader rendering hook because both Fabric and NeoForge include the vanilla
 * renderer classes.
 *
 * <p>This mixin is necessary because vanilla {@code BlockStateModel.collectParts(...)} does
 * not receive world or position context, while the decision between an ordinary Nether portal and
 * a valid Forever World portal is inherently position-sensitive. Fabric Renderer API renderers may
 * also consume models through {@code BlockStateModel.emitQuads(...)} instead of
 * {@code ModelBlockRenderer}; that alternate path is handled on Fabric through public baked-model
 * wrapping rather than a renderer-implementation mixin.
 */
@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {

    @Redirect(
            method = "tesselateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"
            ),
            require = 0
    )
    private void fwportals$useTintedPortalModelVanilla(
            BlockStateModel model,
            RandomSource random,
            List<BlockStateModelPart> parts,
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel originalModel,
            long seed
    ) {
        ClientService.getInstance().collectPortalModelParts(level, pos, state, model, random, parts);
    }

    @Redirect(
            method = "tesselateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"
            ),
            require = 0
    )
    private void fwportals$useTintedPortalModelNeoForge(
            BlockStateModel model,
            BlockAndTintGetter collectLevel,
            BlockPos collectPos,
            BlockState collectState,
            RandomSource random,
            List<BlockStateModelPart> parts,
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel originalModel,
            long seed
    ) {
        ClientService.getInstance().collectPortalModelParts(collectLevel, collectPos, collectState, model, random, parts);
    }
}

package net.pcal.fwportals.client.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;
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
 * <p>Fabric also has an alternate renderer path via Indigo. That path does not necessarily pass
 * through {@code ModelBlockRenderer}, so Fabric needs an additional loader-specific mixin targeting
 * Indigo's {@code AltModelBlockRendererImpl}. This class handles the vanilla path; the Fabric
 * mixin exists to preserve the same portal appearance when Indigo is the active renderer.
 */
@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {

    @Redirect(
            method = "tesselateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"
            )
    )
    private void fwportals$useTintedPortalModel(
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
        ForeverWorldPortalsClient.getInstance().wrapPortalModelIfNeeded(level, pos, state, model).collectParts(random, parts);
    }
}

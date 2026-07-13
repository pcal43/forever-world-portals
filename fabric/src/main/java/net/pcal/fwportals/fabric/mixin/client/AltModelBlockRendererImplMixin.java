package net.pcal.fwportals.fabric.mixin.client;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(AltModelBlockRendererImpl.class)
abstract class AltModelBlockRendererImplMixin {

    private static final float PORTAL_MODEL_DEPTH = 10.0F / 16.0F;

    @Redirect(
            method = "tesselateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/function/Predicate;)V"
            )
    )
    private void fwportals$emitTintedForeverWorldPortalQuads(
            BlockStateModel model,
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest,
            QuadEmitter originalEmitter,
            float x,
            float y,
            float z,
            BlockAndTintGetter originalLevel,
            BlockPos originalPos,
            BlockState originalState,
            BlockStateModel originalModel,
            long seed
    ) {
        if (!state.is(Blocks.NETHER_PORTAL) || !ForeverWorldPortalsClient.getInstance().isForeverWorldPortal(level, pos)) {
            model.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        emitPortalQuads(emitter, state, ForeverWorldPortalsClient.getInstance().portalMaskMaterial());
    }

    private static void emitPortalQuads(QuadEmitter emitter, BlockState state, Material.Baked material) {
        Direction.Axis axis = state.getValue(NetherPortalBlock.AXIS);
        if (axis == Direction.Axis.X) {
            emitPortalFace(emitter, Direction.NORTH, material);
            emitPortalFace(emitter, Direction.SOUTH, material);
            return;
        }

        emitPortalFace(emitter, Direction.WEST, material);
        emitPortalFace(emitter, Direction.EAST, material);
    }

    private static void emitPortalFace(QuadEmitter emitter, Direction face, Material.Baked material) {
        emitter.square(face, 0.0F, 0.0F, 1.0F, 1.0F, PORTAL_MODEL_DEPTH);
        emitter.color(-1, -1, -1, -1);
        emitter.uvUnitSquare();
        emitter.materialBake(material, MutableQuadView.BAKE_NORMALIZED);
        emitter.diffuseShade(false);
        emitter.ambientOcclusion(TriState.FALSE);
        emitter.emissive(true);
        emitter.tintIndex(0);
        emitter.emit();
    }
}

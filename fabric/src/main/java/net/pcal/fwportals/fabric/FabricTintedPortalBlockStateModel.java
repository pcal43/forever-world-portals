package net.pcal.fwportals.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.client.ClientService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fabric Renderer API wrapper that applies the shared Forever World portal quad transformation.
 *
 * <p>Ordinary Nether portals delegate directly to the wrapped model. Valid Forever World portals
 * reuse the shared masked/tinted quad transformation from the client module and emit those quads
 * through the public Renderer API path.
 */
final class FabricTintedPortalBlockStateModel extends WrapperBlockStateModel {

    FabricTintedPortalBlockStateModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest
    ) {
        if (!state.is(Blocks.NETHER_PORTAL) || !ClientService.getInstance().isForeverWorldPortal(level, pos)) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        List<BlockStateModelPart> parts = new ArrayList<>();
        ClientService.getInstance().wrapPortalModel(wrapped).collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            emitPartQuads(part, emitter, cullTest);
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (!state.is(Blocks.NETHER_PORTAL) || !ClientService.getInstance().isForeverWorldPortal(level, pos)) {
            return super.createGeometryKey(level, pos, state, random);
        }
        return ClientService.getInstance().wrapPortalModel(wrapped);
    }

    private static void emitPartQuads(
            BlockStateModelPart part,
            QuadEmitter emitter,
            Predicate<Direction> cullTest
    ) {
        TriState ambientOcclusion = part.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
        for (Direction direction : Direction.values()) {
            if (cullTest.test(direction)) {
                continue;
            }
            emitQuadList(part.getQuads(direction), direction, ambientOcclusion, emitter);
        }
        emitQuadList(part.getQuads(null), null, ambientOcclusion, emitter);
    }

    private static void emitQuadList(
            List<BakedQuad> quads,
            Direction cullFace,
            TriState ambientOcclusion,
            QuadEmitter emitter
    ) {
        for (BakedQuad quad : quads) {
            emitter.cullFace(cullFace);
            emitter.fromBakedQuad(quad);
            emitter.ambientOcclusion(ambientOcclusion);
            emitter.shadeMode(ShadeMode.VANILLA);
            emitter.emit();
        }
    }
}

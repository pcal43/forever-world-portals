package net.pcal.fwportals.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.world.level.block.Blocks;

/**
 * Registers a public Fabric baked-model wrapper for Nether portal block states.
 *
 * <p>Fabric renderers can consume {@code BlockStateModel.emitQuads(...)} directly, bypassing the
 * vanilla {@code ModelBlockRenderer} hook used by common code. Wrapping the baked Nether portal
 * model at load time keeps that alternate path on public Fabric APIs and avoids targeting any
 * renderer implementation classes.
 */
final class FabricPortalModelLoadingPlugin implements ModelLoadingPlugin {

    @Override
    public void initialize(Context context) {
        context.modifyBlockModelAfterBake().register((model, bakeContext) -> {
            if (!bakeContext.state().is(Blocks.NETHER_PORTAL)) {
                return model;
            }
            return new FabricTintedPortalBlockStateModel(model);
        });
    }
}

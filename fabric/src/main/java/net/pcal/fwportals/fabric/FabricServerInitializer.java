package net.pcal.fwportals.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.pcal.fwportals.CommonService;

public class FabricServerInitializer implements ModInitializer {

    private static final Identifier ATTUNEMENTS_RELOAD_ID = Identifier.fromNamespaceAndPath("fwportals", "attunements");

    @Override
    public void onInitialize() {
        CommonService.initializeCommon();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                ATTUNEMENTS_RELOAD_ID,
                lookup -> new FabricReloadListenerAdapter(
                        ATTUNEMENTS_RELOAD_ID,
                        CommonService.getInstance().attunementRegistry().createReloadListener(lookup)
                )
        );
        ServerLifecycleEvents.SERVER_STARTING.register(CommonService.getInstance()::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(CommonService.getInstance()::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(CommonService.getInstance()::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(CommonService.getInstance()::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(CommonService.getInstance()::onServerStopped);
    }

    private record FabricReloadListenerAdapter(
            Identifier fabricId,
            PreparableReloadListener delegate
    ) implements IdentifiableResourceReloadListener {

        @Override
        public Identifier getFabricId() {
            return fabricId;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> reload(
                PreparableReloadListener.SharedState currentReload,
                java.util.concurrent.Executor taskExecutor,
                PreparableReloadListener.PreparationBarrier preparationBarrier,
                java.util.concurrent.Executor reloadExecutor
        ) {
            return delegate.reload(currentReload, taskExecutor, preparationBarrier, reloadExecutor);
        }
    }
}

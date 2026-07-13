package net.pcal.fwportals.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.pcal.fwportals.ForeverWorldPortals;
import net.pcal.fwportals.ForeverWorldPortalsService;

public class FabricServerInitializer implements ModInitializer {

    private static final Identifier ATTUNEMENTS_RELOAD_ID = Identifier.fromNamespaceAndPath("fwportals", "attunements");

    @Override
    public void onInitialize() {
        ForeverWorldPortals.initialize();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                ATTUNEMENTS_RELOAD_ID,
                lookup -> new FabricReloadListenerAdapter(
                        ATTUNEMENTS_RELOAD_ID,
                        ForeverWorldPortalsService.getInstance().attunementRegistry().createReloadListener(lookup)
                )
        );
        ServerLifecycleEvents.SERVER_STARTING.register(ForeverWorldPortalsService.getInstance()::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(ForeverWorldPortalsService.getInstance()::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(ForeverWorldPortalsService.getInstance()::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(ForeverWorldPortalsService.getInstance()::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(ForeverWorldPortalsService.getInstance()::onServerStopped);
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

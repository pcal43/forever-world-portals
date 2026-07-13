package net.pcal.fwportals.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;

public final class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ForeverWorldPortalsClient.getInstance().initialize();
        ModelLoadingPlugin.register(new FabricPortalModelLoadingPlugin());
    }
}

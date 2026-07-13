package net.pcal.fwportals.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;

public final class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ForeverWorldPortalsClient.getInstance().initialize();
    }
}

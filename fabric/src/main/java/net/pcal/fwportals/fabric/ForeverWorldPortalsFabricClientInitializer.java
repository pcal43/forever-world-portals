package net.pcal.fwportals.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;

public final class ForeverWorldPortalsFabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ForeverWorldPortalsClient.getInstance().initialize();
    }
}

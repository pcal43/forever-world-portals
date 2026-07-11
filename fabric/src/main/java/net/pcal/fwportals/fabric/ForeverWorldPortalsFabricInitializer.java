package net.pcal.fwportals.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fwportals.ForeverWorldPortals;
import net.pcal.fwportals.ForeverWorldPortalsService;

public class ForeverWorldPortalsFabricInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        ForeverWorldPortals.initialize();
        ServerLifecycleEvents.SERVER_STARTING.register(ForeverWorldPortalsService.getInstance()::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(ForeverWorldPortalsService.getInstance()::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ForeverWorldPortalsService.getInstance()::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(ForeverWorldPortalsService.getInstance()::onServerStopped);
    }
}

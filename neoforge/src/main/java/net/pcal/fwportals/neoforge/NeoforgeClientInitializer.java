package net.pcal.fwportals.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.pcal.fwportals.client.ClientService;

@EventBusSubscriber(modid = NeoforgeServerInitializer.MOD_ID, value = Dist.CLIENT)
public final class NeoforgeClientInitializer {

    private NeoforgeClientInitializer() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientService.getInstance().initialize();
    }
}

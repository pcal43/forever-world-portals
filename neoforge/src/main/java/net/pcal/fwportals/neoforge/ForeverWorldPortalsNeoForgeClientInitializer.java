package net.pcal.fwportals.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.pcal.fwportals.client.ForeverWorldPortalsClient;

@EventBusSubscriber(modid = ForeverWorldPortalsNeoForgeInitializer.MOD_ID, value = Dist.CLIENT)
public final class ForeverWorldPortalsNeoForgeClientInitializer {

    private ForeverWorldPortalsNeoForgeClientInitializer() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ForeverWorldPortalsClient.getInstance().initialize();
    }
}

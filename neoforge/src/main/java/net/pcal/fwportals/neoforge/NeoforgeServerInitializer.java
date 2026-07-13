package net.pcal.fwportals.neoforge;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.pcal.fwportals.CommonService;

@Mod(NeoforgeServerInitializer.MOD_ID)
public class NeoforgeServerInitializer {

    public static final String MOD_ID = "fwportals";
    private static final Identifier ATTUNEMENTS_RELOAD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "attunements");

    public NeoforgeServerInitializer(IEventBus modEventBus) {
        CommonService.initializeCommon();
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) ->
                event.addListener(
                        ATTUNEMENTS_RELOAD_ID,
                        CommonService.getInstance().attunementRegistry().createReloadListener(event.getRegistryAccess())
                ));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) ->
                CommonService.getInstance().onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                CommonService.getInstance().onServerStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) ->
                CommonService.getInstance().onServerTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) ->
                CommonService.getInstance().onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                CommonService.getInstance().onServerStopped(event.getServer()));
    }
}

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
import net.pcal.fwportals.ForeverWorldPortals;
import net.pcal.fwportals.ForeverWorldPortalsService;

@Mod(ForeverWorldPortalsNeoForgeInitializer.MOD_ID)
public class ForeverWorldPortalsNeoForgeInitializer {

    public static final String MOD_ID = "fwportals";
    private static final Identifier ATTUNEMENTS_RELOAD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "attunements");

    public ForeverWorldPortalsNeoForgeInitializer(IEventBus modEventBus) {
        ForeverWorldPortals.initialize();
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) ->
                event.addListener(
                        ATTUNEMENTS_RELOAD_ID,
                        ForeverWorldPortalsService.getInstance().attunementRegistry().createReloadListener(event.getRegistryAccess())
                ));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) ->
                ForeverWorldPortalsService.getInstance().onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                ForeverWorldPortalsService.getInstance().onServerStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) ->
                ForeverWorldPortalsService.getInstance().onServerTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) ->
                ForeverWorldPortalsService.getInstance().onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                ForeverWorldPortalsService.getInstance().onServerStopped(event.getServer()));
    }
}

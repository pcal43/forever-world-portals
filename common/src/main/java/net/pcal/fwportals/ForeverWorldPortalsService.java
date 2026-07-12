package net.pcal.fwportals;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.pcal.fwportals.attunement.AttunementRegistry;
import net.pcal.fwportals.portal.ForeverWorldPortalFrame;
import net.pcal.fwportals.portal.PortalActivationService;
import net.pcal.fwportals.portal.PortalAttunementService;
import net.pcal.fwportals.portal.PortalTravelService;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class ForeverWorldPortalsService {

    private static final ForeverWorldPortalsService INSTANCE = new ForeverWorldPortalsService();

    public static final String LOGGER_NAME = "fwportals";
    public static final String LOG_PREFIX = "[fwportals] ";

    private ForeverWorldPortalsConfig config;
    private Logger logger;
    private boolean initialized;
    private MinecraftServer currentServer;
    private PortalActivationService portalActivationService;
    private PortalAttunementService portalAttunementService;
    private PortalTravelService portalTravelService;
    private AttunementRegistry attunementRegistry;

    public static ForeverWorldPortalsService getInstance() {
        return INSTANCE;
    }

    void init(ForeverWorldPortalsConfig config, Logger logger) {
        if (this.initialized) {
            throw new IllegalStateException("Forever World Portals has already been initialized");
        }
        this.config = requireNonNull(config);
        this.logger = requireNonNull(logger);
        this.attunementRegistry = new AttunementRegistry(logger);
        this.portalAttunementService = new PortalAttunementService(logger, attunementRegistry);
        this.portalActivationService = new PortalActivationService(config, logger, portalAttunementService);
        this.portalTravelService = new PortalTravelService(config, logger, attunementRegistry);
        this.initialized = true;
    }

    public Logger logger() {
        if (!initialized) {
            throw new IllegalStateException("Forever World Portals has not been initialized");
        }
        return logger;
    }

    public ForeverWorldPortalsConfig config() {
        if (!initialized) {
            throw new IllegalStateException("Forever World Portals has not been initialized");
        }
        return config;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public MinecraftServer currentServer() {
        return currentServer;
    }

    public AttunementRegistry attunementRegistry() {
        if (!initialized || attunementRegistry == null) {
            throw new IllegalStateException("Forever World Portals has not been initialized");
        }
        return attunementRegistry;
    }

    public boolean tryActivatePortal(Level level, BlockPos firePos, ItemStack activationStack, Player player) {
        return portalActivationService().tryActivatePortal(level, firePos, activationStack, player);
    }

    public boolean canActivatePortalAt(BlockGetter level, BlockPos firePos, ItemStack activationStack) {
        return portalActivationService().canActivatePortalAt(level, firePos, activationStack);
    }

    public Optional<ForeverWorldPortalFrame> findForeverWorldPortal(BlockGetter level, BlockPos pos) {
        return portalActivationService().findForeverWorldPortal(level, pos);
    }

    public boolean isForeverWorldPortal(BlockGetter level, BlockPos pos) {
        return portalActivationService().isForeverWorldPortal(level, pos);
    }

    public boolean handleEntityInsidePortal(Level level, BlockPos pos, Entity entity) {
        return portalActivationService().handleEntityInsidePortal(level, pos, entity);
    }

    public boolean canPlayerUseForeverWorldPortal(net.minecraft.server.level.ServerPlayer player) {
        return portalActivationService().canPlayerUseForeverWorldPortal(player);
    }

    public @Nullable net.minecraft.world.level.portal.TeleportTransition getTeleportTransitionForPortal(ServerLevel level, Entity entity, BlockPos portalEntryPos) {
        return portalTravelService().getTeleportTransition(level, entity, portalEntryPos);
    }

    public void onServerStarting(MinecraftServer server) {
        currentServer = requireNonNull(server);
        logger().info(LOG_PREFIX + "Server starting");
    }

    public void onServerStarted(MinecraftServer server) {
        currentServer = requireNonNull(server);
        logger().info(LOG_PREFIX + "Server started");
    }

    public void onServerTick(MinecraftServer server) {
        requireNonNull(server);
        if (portalAttunementService != null) {
            portalAttunementService.onServerTick(server);
        }
    }

    public void onServerStopping(MinecraftServer server) {
        requireNonNull(server);
        logger().info(LOG_PREFIX + "Server stopping");
    }

    public void onServerStopped(MinecraftServer server) {
        requireNonNull(server);
        currentServer = null;
        if (portalActivationService != null) {
            portalActivationService.clearRuntimeState();
        }
        if (portalAttunementService != null) {
            portalAttunementService.clearRuntimeState();
        }
        if (portalTravelService != null) {
            portalTravelService.clearRuntimeState();
        }
        logger().info(LOG_PREFIX + "Server stopped");
    }

    private PortalActivationService portalActivationService() {
        if (!initialized || portalActivationService == null) {
            throw new IllegalStateException("Forever World Portals has not been initialized");
        }
        return portalActivationService;
    }

    private PortalTravelService portalTravelService() {
        if (!initialized || portalTravelService == null) {
            throw new IllegalStateException("Forever World Portals has not been initialized");
        }
        return portalTravelService;
    }

    private ForeverWorldPortalsService() {
    }
}

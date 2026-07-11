package net.pcal.fwportals;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

import static java.util.Objects.requireNonNull;

public final class ForeverWorldPortalsService {

    private static final ForeverWorldPortalsService INSTANCE = new ForeverWorldPortalsService();

    public static final String LOGGER_NAME = "fwportals";
    public static final String LOG_PREFIX = "[fwportals] ";

    private ForeverWorldPortalsConfig config;
    private Logger logger;
    private boolean initialized;
    private MinecraftServer currentServer;

    public static ForeverWorldPortalsService getInstance() {
        return INSTANCE;
    }

    void init(ForeverWorldPortalsConfig config, Logger logger) {
        if (this.initialized) {
            throw new IllegalStateException("Forever World Portals has already been initialized");
        }
        this.config = requireNonNull(config);
        this.logger = requireNonNull(logger);
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

    public void onServerStarting(MinecraftServer server) {
        currentServer = requireNonNull(server);
        logger().info(LOG_PREFIX + "Server starting");
    }

    public void onServerStarted(MinecraftServer server) {
        currentServer = requireNonNull(server);
        logger().info(LOG_PREFIX + "Server started");
    }

    public void onServerStopping(MinecraftServer server) {
        requireNonNull(server);
        logger().info(LOG_PREFIX + "Server stopping");
    }

    public void onServerStopped(MinecraftServer server) {
        requireNonNull(server);
        currentServer = null;
        logger().info(LOG_PREFIX + "Server stopped");
    }

    private ForeverWorldPortalsService() {
    }
}

package net.pcal.fwportals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.nio.file.Paths;

import static net.pcal.fwportals.ForeverWorldPortalsService.LOGGER_NAME;
import static net.pcal.fwportals.ForeverWorldPortalsService.LOG_PREFIX;

public final class ForeverWorldPortals {

    public static void initialize() {
        try {
            initializeCommon();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void initializeCommon() throws IOException {
        Logger logger = LogManager.getLogger(LOGGER_NAME);
        ForeverWorldPortalsConfigLoader configLoader = ForeverWorldPortalsConfigLoader.createDefault();
        java.nio.file.Path configFilePath = Paths.get("config").resolve(ForeverWorldPortalsConfigLoader.CONFIG_FILENAME);

        logger.info(LOG_PREFIX + "Loading configuration from " + configFilePath);
        ForeverWorldPortalsConfig config = configLoader.load(configFilePath, logger);

        if (config.logLevel() != Level.INFO) {
            Configurator.setLevel(LOGGER_NAME, config.logLevel());
            logger.info(LOG_PREFIX + "LogLevel set to " + config.logLevel());
        }

        ForeverWorldPortalsService.getInstance().init(config, logger);
        logger.info(LOG_PREFIX + "Initialized Forever World Portals");
    }

    private ForeverWorldPortals() {
    }
}

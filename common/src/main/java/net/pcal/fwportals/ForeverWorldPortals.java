package net.pcal.fwportals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static net.pcal.fwportals.ForeverWorldPortalsService.LOGGER_NAME;
import static net.pcal.fwportals.ForeverWorldPortalsService.LOG_PREFIX;

public final class ForeverWorldPortals {

    private static final String DEFAULT_CONFIG_RESOURCE_NAME = "fwportals-default-config.properties";
    private static final String CONFIG_FILENAME = "fwportals.properties";

    public static void initialize() {
        try {
            initializeCommon();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void initializeCommon() throws IOException {
        Logger logger = LogManager.getLogger(LOGGER_NAME);

        Path configDirPath = Paths.get("config");
        Path configFilePath = configDirPath.resolve(CONFIG_FILENAME);

        if (!configFilePath.toFile().exists()) {
            logger.info(LOG_PREFIX + "Writing default configuration to " + configFilePath);
            try (InputStream in = ForeverWorldPortals.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME)) {
                if (in == null) {
                    throw new IllegalStateException("Unable to load " + DEFAULT_CONFIG_RESOURCE_NAME);
                }
                java.nio.file.Files.createDirectories(configDirPath);
                java.nio.file.Files.copy(in, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        ForeverWorldPortalsConfig defaultConfig;
        try (InputStream in = ForeverWorldPortals.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME)) {
            if (in == null) {
                throw new IllegalStateException("Unable to load " + DEFAULT_CONFIG_RESOURCE_NAME);
            }
            defaultConfig = ForeverWorldPortalsConfigParser.parse(in, ForeverWorldPortalsConfig.defaults(), logger);
        }

        logger.info(LOG_PREFIX + "Loading configuration from " + configFilePath);
        ForeverWorldPortalsConfig config;
        try (InputStream in = new FileInputStream(configFilePath.toFile())) {
            config = ForeverWorldPortalsConfigParser.parse(in, defaultConfig, logger);
        }

        if (config.logLevel() != Level.INFO) {
            Configurator.setLevel(LOGGER_NAME, config.logLevel());
            logger.info(LOG_PREFIX + "LogLevel set to " + config.logLevel());
        }

        ForeverWorldPortalsService.getInstance().init(config, logger);
        logger.info(LOG_PREFIX + "Initialized Forever World Portals placeholder");
    }

    private ForeverWorldPortals() {
    }
}

package net.pcal.fwportals;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

final class ForeverWorldPortalsConfigLoader {

    static final String DEFAULT_CONFIG_RESOURCE_NAME = "fwportals-default.properties";
    static final String CONFIG_FILENAME = "fwportals.properties";

    private final String resourceName;
    private final Supplier<InputStream> resourceSupplier;

    ForeverWorldPortalsConfigLoader(String resourceName, Supplier<InputStream> resourceSupplier) {
        this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
        this.resourceSupplier = Objects.requireNonNull(resourceSupplier, "resourceSupplier");
    }

    static ForeverWorldPortalsConfigLoader createDefault() {
        return new ForeverWorldPortalsConfigLoader(
                DEFAULT_CONFIG_RESOURCE_NAME,
                () -> ForeverWorldPortalsConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME)
        );
    }

    ForeverWorldPortalsConfig load(Path configFilePath, Logger logger) throws IOException {
        Properties bundledDefaults = loadBundledDefaultProperties();
        ForeverWorldPortalsConfig defaultConfig = ForeverWorldPortalsConfigParser.parseBundledDefaults(
                bundledDefaults,
                resourceName
        );

        if (!Files.exists(configFilePath)) {
            ensureUserConfigExists(configFilePath, logger);
            return defaultConfig;
        }

        Properties userOverrides = loadUserProperties(configFilePath);
        return ForeverWorldPortalsConfigParser.parse(userOverrides, defaultConfig, logger);
    }

    Properties loadBundledDefaultProperties() throws IOException {
        try (InputStream in = openResource()) {
            return loadProperties(in);
        }
    }

    void ensureUserConfigExists(Path configFilePath, Logger logger) throws IOException {
        if (Files.exists(configFilePath)) {
            return;
        }

        if (logger != null) {
            logger.info(ForeverWorldPortalsService.LOG_PREFIX + "Writing default configuration to " + configFilePath);
        }

        Path parent = configFilePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (InputStream in = openResource()) {
            Files.copy(in, configFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    Properties loadUserProperties(Path configFilePath) throws IOException {
        try (InputStream in = Files.newInputStream(configFilePath)) {
            return loadProperties(in);
        }
    }

    private InputStream openResource() {
        InputStream in = resourceSupplier.get();
        if (in == null) {
            throw new IllegalStateException("Unable to load bundled configuration resource '" + resourceName + "'");
        }
        return in;
    }

    private static Properties loadProperties(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        return properties;
    }
}

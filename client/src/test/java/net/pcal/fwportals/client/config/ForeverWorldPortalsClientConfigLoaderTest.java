package net.pcal.fwportals.client.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeverWorldPortalsClientConfigLoaderTest {

    private static final Logger LOGGER = LogManager.getLogger("fwportals-test");
    private static final String DEFAULT_TEMPLATE = """
            requireEmptyInventory=true
            logLevel=INFO
            frameBlock=minecraft:diamond_block
            activationItem=minecraft:flint_and_steel
            destinationPortalMode=RUINED
            client.portalColor=#4CAF50
            destinationSpiralSpacingBlocks=10000
            maximumSpiralSearchPositions=512
            maximumBiomeSearches=64
            maximumPortalPlacementAttemptsPerBiome=64
            minimumGeneratedTerrainDistanceBlocks=10000
            """;

    @Test
    void parsesValidClientPortalColor() {
        assertEquals(0x4CAF50, ClientConfigLoader.parseColor("#4CAF50"));
    }

    @Test
    void rejectsMalformedClientPortalColor() {
        assertThrows(IllegalArgumentException.class, () -> ClientConfigLoader.parseColor("4CAF50"));
        assertThrows(IllegalArgumentException.class, () -> ClientConfigLoader.parseColor("#4caf5"));
        assertThrows(IllegalArgumentException.class, () -> ClientConfigLoader.parseColor("#GGGGGG"));
    }

    @Test
    void invalidUserClientPortalColorFallsBackToBundledDefault(@TempDir Path tempDir) throws IOException {
        ForeverWorldPortalsClientConfigLoaderHelper helper = new ForeverWorldPortalsClientConfigLoaderHelper(DEFAULT_TEMPLATE);
        Path configPath = tempDir.resolve("config").resolve("fwportals.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, "client.portalColor=bad\n", StandardCharsets.UTF_8);

        ClientConfig config = helper.load(configPath, LOGGER);

        assertEquals(0x4CAF50, config.portalColorRgb());
    }

    @Test
    void missingBundledClientPortalColorFailsClearly() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ForeverWorldPortalsClientConfigLoaderHelper("requireEmptyInventory=true\n").loadBundledDefaultColor()
        );
        assertTrue(exception.getMessage().contains("client.portalColor"));
    }

    private static final class ForeverWorldPortalsClientConfigLoaderHelper {

        private final String defaultsText;

        private ForeverWorldPortalsClientConfigLoaderHelper(String defaultsText) {
            this.defaultsText = defaultsText;
        }

        private ClientConfig load(Path configPath, Logger logger) throws IOException {
            java.util.Properties bundledDefaults = loadBundledDefaults();
            int defaultColor = loadBundledDefaultColor();
            java.util.Properties userProperties = Files.exists(configPath)
                    ? loadProperties(Files.newInputStream(configPath))
                    : new java.util.Properties();
            return new ClientConfig(
                    ClientConfigLoader.parseUserColor(
                            userProperties.getProperty(ClientConfigLoader.PORTAL_COLOR_KEY),
                            defaultColor,
                            logger
                    )
            );
        }

        private int loadBundledDefaultColor() throws IOException {
            return ClientConfigLoader.parseBundledDefaultColor(
                    loadBundledDefaults(),
                    ClientConfigLoader.PORTAL_COLOR_KEY,
                    "test-defaults.properties"
            );
        }

        private java.util.Properties loadBundledDefaults() throws IOException {
            return loadProperties(new ByteArrayInputStream(defaultsText.getBytes(StandardCharsets.UTF_8)));
        }

        private static java.util.Properties loadProperties(java.io.InputStream in) throws IOException {
            java.util.Properties properties = new java.util.Properties();
            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return properties;
        }
    }
}

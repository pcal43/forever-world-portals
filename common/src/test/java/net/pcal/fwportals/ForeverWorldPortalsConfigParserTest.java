package net.pcal.fwportals;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForeverWorldPortalsConfigParserTest {

    @Test
    void fallsBackToDefaultsForMalformedValues() throws IOException {
        ForeverWorldPortalsConfig config = ForeverWorldPortalsConfigParser.parse(
                new ByteArrayInputStream("""
                        enabled=maybe
                        logLevel=LOUD
                        """.getBytes(StandardCharsets.UTF_8)),
                ForeverWorldPortalsConfig.defaults(),
                null
        );

        assertEquals(true, config.enabled());
        assertEquals(Level.INFO, config.logLevel());
    }
}

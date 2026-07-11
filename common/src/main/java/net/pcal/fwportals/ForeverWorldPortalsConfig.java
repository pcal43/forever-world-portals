package net.pcal.fwportals;

import org.apache.logging.log4j.Level;

public record ForeverWorldPortalsConfig(
        boolean enabled,
        Level logLevel
) {

    static ForeverWorldPortalsConfig defaults() {
        return new ForeverWorldPortalsConfig(true, Level.INFO);
    }
}

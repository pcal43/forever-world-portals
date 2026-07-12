package net.pcal.fwportals.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ForeverWorldPortalsClient {

    private static final ForeverWorldPortalsClient INSTANCE = new ForeverWorldPortalsClient();
    private static final Logger LOGGER = LogManager.getLogger("fwportals");

    private ForeverWorldPortalsClient() {
    }

    public static ForeverWorldPortalsClient getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        LOGGER.info("[fwportals] Initialized Forever World Portals client scaffolding");
    }
}

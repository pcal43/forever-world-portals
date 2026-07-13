package net.pcal.fwportals.common;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class TestBootstrap {

    private static boolean bootstrapped;

    public static void ensureBootstrapped() {
        if (!bootstrapped) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            bootstrapped = true;
        }
    }

    private TestBootstrap() {
    }
}

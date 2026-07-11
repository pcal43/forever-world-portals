package net.pcal.fwportals;

public enum ReturnPortalMode {
    GENERATE(true),
    REQUIRE_PLAYER_BUILD(false),
    NONE(false);

    private final boolean implemented;

    ReturnPortalMode(boolean implemented) {
        this.implemented = implemented;
    }

    public boolean isImplemented() {
        return implemented;
    }
}

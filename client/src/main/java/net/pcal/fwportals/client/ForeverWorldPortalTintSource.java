package net.pcal.fwportals.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ForeverWorldPortalTintSource implements BlockTintSource {

    public static final ForeverWorldPortalTintSource INSTANCE = new ForeverWorldPortalTintSource();

    private ForeverWorldPortalTintSource() {
    }

    @Override
    public int color(BlockState state) {
        return opaqueArgb(ForeverWorldPortalsClient.getInstance().portalColorRgb());
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return opaqueArgb(ForeverWorldPortalsClient.getInstance().portalColorRgb());
    }

    private static int opaqueArgb(int rgb) {
        return 0xFF000000 | rgb;
    }
}

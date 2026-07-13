package net.pcal.fwportals.client.effects;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.client.ClientService;

public final class TintSource implements BlockTintSource {

    public static final TintSource INSTANCE = new TintSource();

    private TintSource() {
    }

    @Override
    public int color(BlockState state) {
        return opaqueArgb(ClientService.getInstance().portalColorRgb());
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return opaqueArgb(ClientService.getInstance().portalColorRgb());
    }

    private static int opaqueArgb(int rgb) {
        return 0xFF000000 | rgb;
    }
}

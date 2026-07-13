package net.pcal.fwportals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.ForeverWorldPortalsService;
import net.pcal.fwportals.common.portal.PortalFrameDetector;
import net.pcal.fwportals.common.portal.PortalInventoryAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ForeverWorldPortalsClient {

    private static final ForeverWorldPortalsClient INSTANCE = new ForeverWorldPortalsClient();
    private static final Logger LOGGER = LogManager.getLogger("fwportals");
    static final Identifier PORTAL_MASK_TEXTURE_ID = Identifier.parse("fwportals:block/forever_world_portal_mask");
    static final SpriteId PORTAL_MASK_SPRITE_ID = new SpriteId(TextureAtlas.LOCATION_BLOCKS, PORTAL_MASK_TEXTURE_ID);

    private final PortalFrameDetector detector = new PortalFrameDetector();
    private ForeverWorldPortalsClientConfig config;

    public static ForeverWorldPortalsClient getInstance() {
        return INSTANCE;
    }

    private ForeverWorldPortalsClient() {
    }

    public void initialize() {
        ensureConfigLoaded();
        LOGGER.info("[fwportals] Initialized Forever World Portals client rendering");
    }

    public int portalColorRgb() {
        return ensureConfigLoaded().portalColorRgb();
    }

    public Material.Baked portalMaskMaterial() {
        return new Material.Baked(portalMaskSprite(), false);
    }

    public boolean shouldSuppressPortalEffect(LocalPlayer player) {
        if (player == null || PortalInventoryAccess.isEmpty(player.getInventory()) || player.getActivePortalLocalTransition() == null) {
            return false;
        }

        BlockAndTintGetter level = (BlockAndTintGetter) player.level();
        double epsilon = 1.0E-5;
        int minX = Mth.floor(player.getBoundingBox().minX + epsilon);
        int minY = Mth.floor(player.getBoundingBox().minY + epsilon);
        int minZ = Mth.floor(player.getBoundingBox().minZ + epsilon);
        int maxX = Mth.floor(player.getBoundingBox().maxX - epsilon);
        int maxY = Mth.floor(player.getBoundingBox().maxY - epsilon);
        int maxZ = Mth.floor(player.getBoundingBox().maxZ - epsilon);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(Blocks.NETHER_PORTAL) && isForeverWorldPortal(level, cursor)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public BlockStateModel wrapPortalModelIfNeeded(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel model
    ) {
        if (!state.is(Blocks.NETHER_PORTAL) || !isForeverWorldPortal(level, pos)) {
            return model;
        }
        return new TintedPortalBlockStateModel(model, portalMaskSprite());
    }

    public boolean isForeverWorldPortal(BlockAndTintGetter level, BlockPos pos) {
        BlockState frameState = ForeverWorldPortalsService.getInstance().isInitialized()
                ? ForeverWorldPortalsService.getInstance().config().frameBlock().defaultBlockState()
                : Blocks.DIAMOND_BLOCK.defaultBlockState();
        return detector.findPortalFrame(level, pos, frameState).isPresent();
    }

    private TextureAtlasSprite portalMaskSprite() {
        return Minecraft.getInstance().getAtlasManager().get(PORTAL_MASK_SPRITE_ID);
    }

    private ForeverWorldPortalsClientConfig ensureConfigLoaded() {
        if (config == null) {
            config = ForeverWorldPortalsClientConfigLoader.load(LOGGER);
        }
        return config;
    }
}

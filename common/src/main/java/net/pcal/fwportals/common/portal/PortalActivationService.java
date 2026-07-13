package net.pcal.fwportals.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.CommonService;
import net.pcal.fwportals.common.config.CommonConfig;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PortalActivationService {

    private static final long ENTRY_LOG_COOLDOWN_TICKS = 40L;
    private static final long INVENTORY_WARNING_COOLDOWN_TICKS = 40L;
    private final CommonConfig config;
    private final Logger logger;
    private final PortalAttunementService portalAttunementService;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final PortalIdentity portalIdentity = new PortalIdentity();
    private final Map<UUID, PortalEntryRecord> recentEntries = new HashMap<>();
    private final Map<UUID, Long> recentInventoryWarnings = new HashMap<>();

    public PortalActivationService(CommonConfig config, Logger logger, PortalAttunementService portalAttunementService) {
        this.config = config;
        this.logger = logger;
        this.portalAttunementService = portalAttunementService;
    }

    public boolean tryActivatePortal(Level level, BlockPos firePos, Player player) {
        if (level.isClientSide()) {
            return false;
        }
        if (!isPortalDimension(level)) {
            return false;
        }

        BlockState frameState = config.frameBlock().defaultBlockState();
        Optional<PortalFrame> maybeFrame = detector.findEmptyFrame(level, firePos, Direction.Axis.X, frameState);
        if (maybeFrame.isEmpty()) {
            if (isAdjacentToFrame(level, firePos, frameState)) {
                logger.debug(CommonService.LOG_PREFIX + "Activation failed at {}", firePos);
            }
            return false;
        }

        PortalFrame frame = maybeFrame.get();
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, frame.axis());
        for (BlockPos portalPos : frame.interiorBlocks()) {
            level.setBlock(portalPos, portalState, 18);
        }
        logger.info(
                CommonService.LOG_PREFIX + "Activated Forever World portal with frame base {} and portal anchor {} axis={} width={} height={}",
                frame.frameBasePos(),
                portalIdentity.computeAnchorBlock(frame),
                frame.axis(),
                frame.width(),
                frame.height()
        );
        return true;
    }

    public boolean canActivatePortalAt(BlockGetter level, BlockPos firePos) {
        BlockState frameState = config.frameBlock().defaultBlockState();
        return detector.findEmptyFrame(level, firePos, Direction.Axis.X, frameState).isPresent();
    }

    public Optional<PortalFrame> findForeverWorldPortal(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.NETHER_PORTAL)) {
            return Optional.empty();
        }
        return detector.findPortalFrame(level, pos, config.frameBlock().defaultBlockState());
    }

    public boolean isForeverWorldPortal(BlockGetter level, BlockPos pos) {
        return findForeverWorldPortal(level, pos).isPresent();
    }

    public boolean handleEntityInsidePortal(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
            return true;
        }
        Optional<PortalFrame> maybeFrame = findForeverWorldPortal(level, pos);
        if (maybeFrame.isEmpty()) {
            return true;
        }

        PortalFrame frame = maybeFrame.get();
        if (entity instanceof ServerPlayer player && shouldRejectPlayerEntry(player, level.getGameTime())) {
            return false;
        }

        if (entity instanceof ItemEntity itemEntity && level instanceof ServerLevel serverLevel) {
            portalAttunementService.onItemInsidePortal(serverLevel, frame, itemEntity);
        }

        BlockPos anchorBlock = portalIdentity.computeAnchorBlock(frame);
        PortalEntryRecord previous = recentEntries.get(entity.getUUID());
        long gameTime = level.getGameTime();
        if (previous != null
                && previous.dimension().equals(level.dimension())
                && previous.anchor().equals(anchorBlock)
                && gameTime - previous.lastSeenGameTime() <= ENTRY_LOG_COOLDOWN_TICKS) {
            recentEntries.put(entity.getUUID(), new PortalEntryRecord(previous.dimension(), previous.anchor(), gameTime));
            return true;
        }

        recentEntries.put(entity.getUUID(), new PortalEntryRecord(level.dimension(), anchorBlock, gameTime));
        logger.info(
                CommonService.LOG_PREFIX + "Entity entered Forever World portal: entity={} type={} anchorBlock={}",
                entity.getName().getString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                anchorBlock
        );
        return true;
    }

    public boolean shouldSuppressTeleport(ServerLevel level, Entity entity, BlockPos portalEntryPos) {
        if (!level.getBlockState(portalEntryPos).is(Blocks.NETHER_PORTAL)) {
            return false;
        }
        boolean foreverWorldPortal = isForeverWorldPortal(level, portalEntryPos);
        if (foreverWorldPortal) {
            logger.debug(
                    CommonService.LOG_PREFIX + "Suppressing vanilla Nether teleport for entity={} at {}",
                    entity.getName().getString(),
                    portalEntryPos
            );
        }
        return foreverWorldPortal;
    }

    public void clearRuntimeState() {
        recentEntries.clear();
        recentInventoryWarnings.clear();
    }

    public boolean canPlayerUseForeverWorldPortal(ServerPlayer player) {
        return !config.requireEmptyInventory() || PortalInventoryAccess.isEmpty(player.getInventory());
    }

    private boolean isAdjacentToFrame(LevelAccessor level, BlockPos pos, BlockState frameState) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(frameState.getBlock())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPortalDimension(Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER;
    }

    private boolean shouldRejectPlayerEntry(ServerPlayer player, long gameTime) {
        if (canPlayerUseForeverWorldPortal(player)) {
            return false;
        }
        Long previousWarning = recentInventoryWarnings.get(player.getUUID());
        if (previousWarning == null || gameTime - previousWarning > INVENTORY_WARNING_COOLDOWN_TICKS) {
            player.connection.send(new ClientboundSetActionBarTextPacket(PortalFeedbackText.inventoryBlockedMessage()));
            recentInventoryWarnings.put(player.getUUID(), gameTime);
        }
        return true;
    }

    private record PortalEntryRecord(ResourceKey<Level> dimension, BlockPos anchor, long lastSeenGameTime) {
    }
}

package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.pcal.fwportals.ForeverWorldPortalsConfig;
import net.pcal.fwportals.ForeverWorldPortalsService;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PortalActivationService {

    private static final long ENTRY_LOG_COOLDOWN_TICKS = 40L;

    private final ForeverWorldPortalsConfig config;
    private final Logger logger;
    private final PortalFrameDetector detector = new PortalFrameDetector();
    private final Map<UUID, PortalEntryRecord> recentEntries = new HashMap<>();

    public PortalActivationService(ForeverWorldPortalsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public boolean tryActivatePortal(Level level, BlockPos firePos, ItemStack activationStack, Player player) {
        if (!config.enabled() || level.isClientSide()) {
            return false;
        }
        if (!activationStack.is(config.activationItem())) {
            return false;
        }
        if (!isPortalDimension(level)) {
            return false;
        }

        BlockState frameState = config.frameBlock().defaultBlockState();
        Optional<ForeverWorldPortalFrame> maybeFrame = detector.findEmptyFrame(level, firePos, Direction.Axis.X, frameState);
        if (maybeFrame.isEmpty()) {
            if (isAdjacentToFrame(level, firePos, frameState)) {
                logger.debug(ForeverWorldPortalsService.LOG_PREFIX + "Activation failed at {}", firePos);
            }
            return false;
        }

        ForeverWorldPortalFrame frame = maybeFrame.get();
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, frame.axis());
        for (BlockPos portalPos : frame.interiorBlocks()) {
            level.setBlock(portalPos, portalState, 18);
        }
        logger.info(
                ForeverWorldPortalsService.LOG_PREFIX + "Activated Forever World portal at {} axis={} width={} height={}",
                frame.anchorPos(),
                frame.axis(),
                frame.width(),
                frame.height()
        );
        return true;
    }

    public Optional<ForeverWorldPortalFrame> findForeverWorldPortal(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.NETHER_PORTAL)) {
            return Optional.empty();
        }
        return detector.findPortalFrame(level, pos, config.frameBlock().defaultBlockState());
    }

    public boolean isForeverWorldPortal(BlockGetter level, BlockPos pos) {
        return findForeverWorldPortal(level, pos).isPresent();
    }

    public void onEntityInsidePortal(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
            return;
        }
        Optional<ForeverWorldPortalFrame> maybeFrame = findForeverWorldPortal(level, pos);
        if (maybeFrame.isEmpty()) {
            return;
        }

        ForeverWorldPortalFrame frame = maybeFrame.get();
        PortalEntryRecord previous = recentEntries.get(entity.getUUID());
        long gameTime = level.getGameTime();
        if (previous != null
                && previous.dimension().equals(level.dimension())
                && previous.anchor().equals(frame.anchorPos())
                && gameTime - previous.lastSeenGameTime() <= ENTRY_LOG_COOLDOWN_TICKS) {
            recentEntries.put(entity.getUUID(), new PortalEntryRecord(previous.dimension(), previous.anchor(), gameTime));
            return;
        }

        recentEntries.put(entity.getUUID(), new PortalEntryRecord(level.dimension(), frame.anchorPos(), gameTime));
        logger.info(
                ForeverWorldPortalsService.LOG_PREFIX + "Entity entered Forever World portal: entity={} type={} anchor={}",
                entity.getName().getString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                frame.anchorPos()
        );
    }

    public boolean shouldSuppressTeleport(ServerLevel level, Entity entity, BlockPos portalEntryPos) {
        if (!level.getBlockState(portalEntryPos).is(Blocks.NETHER_PORTAL)) {
            return false;
        }
        boolean foreverWorldPortal = isForeverWorldPortal(level, portalEntryPos);
        if (foreverWorldPortal) {
            logger.debug(
                    ForeverWorldPortalsService.LOG_PREFIX + "Suppressing vanilla Nether teleport for entity={} at {}",
                    entity.getName().getString(),
                    portalEntryPos
            );
        }
        return foreverWorldPortal;
    }

    public void clearRuntimeState() {
        recentEntries.clear();
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

    private record PortalEntryRecord(ResourceKey<Level> dimension, BlockPos anchor, long lastSeenGameTime) {
    }
}

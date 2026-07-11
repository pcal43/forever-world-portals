package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public record SourcePortalRecord(
        UUID portalId,
        ResourceKey<Level> dimension,
        BlockPos originBlock,
        ResourceKey<Level> destinationDimension,
        BlockPos destinationPosition
) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.store("portalId", UUIDUtil.STRING_CODEC, portalId);
        tag.store("dimension", DIMENSION_CODEC, dimension);
        tag.store("originBlock", BlockPos.CODEC, originBlock);
        tag.store("destinationDimension", DIMENSION_CODEC, destinationDimension);
        tag.store("destinationPosition", BlockPos.CODEC, destinationPosition);
        return tag;
    }

    public static Optional<SourcePortalRecord> fromTag(CompoundTag tag, Consumer<String> warn) {
        Optional<UUID> portalId = PortalPersistenceHelper.required(tag, "portalId", UUIDUtil.STRING_CODEC, "source portal", warn);
        Optional<ResourceKey<Level>> dimension = PortalPersistenceHelper.required(tag, "dimension", DIMENSION_CODEC, "source portal", warn);
        Optional<BlockPos> originBlock = PortalPersistenceHelper.required(tag, "originBlock", BlockPos.CODEC, "source portal", warn);
        Optional<ResourceKey<Level>> destinationDimension = PortalPersistenceHelper.required(
                tag,
                "destinationDimension",
                DIMENSION_CODEC,
                "source portal",
                warn
        );
        Optional<BlockPos> destinationPosition = PortalPersistenceHelper.required(
                tag,
                "destinationPosition",
                BlockPos.CODEC,
                "source portal",
                warn
        );

        if (portalId.isEmpty()
                || dimension.isEmpty()
                || originBlock.isEmpty()
                || destinationDimension.isEmpty()
                || destinationPosition.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SourcePortalRecord(
                portalId.get(),
                dimension.get(),
                originBlock.get(),
                destinationDimension.get(),
                destinationPosition.get()
        ));
    }
}

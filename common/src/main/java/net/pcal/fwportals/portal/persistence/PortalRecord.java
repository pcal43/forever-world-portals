package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

public record PortalRecord(
        ResourceKey<Level> dimension,
        BlockPos anchor,
        ResourceKey<Level> destinationDimension,
        BlockPos destinationAnchor
) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.store("sourceDimension", DIMENSION_CODEC, dimension);
        tag.store("sourceAnchor", BlockPos.CODEC, anchor);
        tag.store("destinationDimension", DIMENSION_CODEC, destinationDimension);
        tag.store("destinationAnchor", BlockPos.CODEC, destinationAnchor);
        return tag;
    }

    public static Optional<PortalRecord> fromTag(CompoundTag tag, Consumer<String> warn) {
        Optional<ResourceKey<Level>> dimension = PortalPersistenceHelper.required(
                tag,
                "sourceDimension",
                DIMENSION_CODEC,
                "portal",
                warn
        );
        Optional<BlockPos> anchor = PortalPersistenceHelper.required(tag, "sourceAnchor", BlockPos.CODEC, "portal", warn);
        Optional<ResourceKey<Level>> destinationDimension = PortalPersistenceHelper.required(
                tag,
                "destinationDimension",
                DIMENSION_CODEC,
                "portal",
                warn
        );
        Optional<BlockPos> destinationAnchor = PortalPersistenceHelper.required(
                tag,
                "destinationAnchor",
                BlockPos.CODEC,
                "portal",
                warn
        );

        if (dimension.isEmpty()
                || anchor.isEmpty()
                || destinationDimension.isEmpty()
                || destinationAnchor.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PortalRecord(
                dimension.get(),
                anchor.get(),
                destinationDimension.get(),
                destinationAnchor.get()
        ));
    }
}

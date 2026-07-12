package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

public record SourcePortalRecord(
        ResourceKey<Level> sourceDimension,
        BlockPos sourceAnchor,
        ResourceKey<Level> destinationDimension,
        BlockPos destinationAnchor
) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.store("sourceDimension", DIMENSION_CODEC, sourceDimension);
        tag.store("sourceAnchor", BlockPos.CODEC, sourceAnchor);
        tag.store("destinationDimension", DIMENSION_CODEC, destinationDimension);
        tag.store("destinationAnchor", BlockPos.CODEC, destinationAnchor);
        return tag;
    }

    public static Optional<SourcePortalRecord> fromTag(CompoundTag tag, Consumer<String> warn) {
        Optional<ResourceKey<Level>> sourceDimension = PortalPersistenceHelper.required(
                tag,
                "sourceDimension",
                DIMENSION_CODEC,
                "source portal",
                warn
        );
        Optional<BlockPos> sourceAnchor = PortalPersistenceHelper.required(tag, "sourceAnchor", BlockPos.CODEC, "source portal", warn);
        Optional<ResourceKey<Level>> destinationDimension = PortalPersistenceHelper.required(
                tag,
                "destinationDimension",
                DIMENSION_CODEC,
                "source portal",
                warn
        );
        Optional<BlockPos> destinationAnchor = PortalPersistenceHelper.required(
                tag,
                "destinationAnchor",
                BlockPos.CODEC,
                "source portal",
                warn
        );

        if (sourceDimension.isEmpty()
                || sourceAnchor.isEmpty()
                || destinationDimension.isEmpty()
                || destinationAnchor.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SourcePortalRecord(
                sourceDimension.get(),
                sourceAnchor.get(),
                destinationDimension.get(),
                destinationAnchor.get()
        ));
    }
}

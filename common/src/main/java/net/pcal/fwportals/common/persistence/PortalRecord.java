package net.pcal.fwportals.common.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Consumer;

public record PortalRecord(
        ResourceKey<Level> dimension,
        BlockPos anchor,
        Optional<ResourceKey<Level>> destinationDimension,
        Optional<BlockPos> destinationAnchor,
        Optional<Identifier> attunementItemId
) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);
    private static final Codec<Identifier> IDENTIFIER_CODEC = Identifier.CODEC;

    public PortalRecord {
        anchor = anchor.immutable();
        destinationAnchor = destinationAnchor.map(BlockPos::immutable);
        destinationDimension = Optional.ofNullable(destinationDimension.orElse(null));
        destinationAnchor = Optional.ofNullable(destinationAnchor.orElse(null));
        attunementItemId = Optional.ofNullable(attunementItemId.orElse(null));
        if (destinationDimension.isPresent() != destinationAnchor.isPresent()) {
            throw new IllegalArgumentException("Portal destination dimension and anchor must either both be present or both be absent");
        }
    }

    public static PortalRecord resolved(
            ResourceKey<Level> dimension,
            BlockPos anchor,
            ResourceKey<Level> destinationDimension,
            BlockPos destinationAnchor
    ) {
        return new PortalRecord(
                dimension,
                anchor,
                Optional.of(destinationDimension),
                Optional.of(destinationAnchor),
                Optional.empty()
        );
    }

    public static PortalRecord pending(
            ResourceKey<Level> dimension,
            BlockPos anchor,
            Identifier attunementItemId
    ) {
        return new PortalRecord(
                dimension,
                anchor,
                Optional.empty(),
                Optional.empty(),
                Optional.of(attunementItemId)
        );
    }

    public boolean isResolved() {
        return destinationDimension.isPresent();
    }

    public PortalRecord withAttunementItem(Identifier itemId) {
        return new PortalRecord(dimension, anchor, Optional.empty(), Optional.empty(), Optional.of(itemId));
    }

    public PortalRecord resolvedTo(ResourceKey<Level> targetDimension, BlockPos targetAnchor) {
        return resolved(dimension, anchor, targetDimension, targetAnchor);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.store("sourceDimension", DIMENSION_CODEC, dimension);
        tag.store("sourceAnchor", BlockPos.CODEC, anchor);
        destinationDimension.ifPresent(value -> tag.store("destinationDimension", DIMENSION_CODEC, value));
        destinationAnchor.ifPresent(value -> tag.store("destinationAnchor", BlockPos.CODEC, value));
        attunementItemId.ifPresent(value -> tag.store("attunementItem", IDENTIFIER_CODEC, value));
        return tag;
    }

    public static Optional<PortalRecord> fromTag(CompoundTag tag, Consumer<String> warn) {
        Optional<ResourceKey<Level>> dimension = required(
                tag,
                "sourceDimension",
                DIMENSION_CODEC,
                "portal",
                warn
        );
        Optional<BlockPos> anchor = required(tag, "sourceAnchor", BlockPos.CODEC, "portal", warn);
        Optional<ResourceKey<Level>> destinationDimension = optional(tag, "destinationDimension", DIMENSION_CODEC);
        Optional<BlockPos> destinationAnchor = optional(tag, "destinationAnchor", BlockPos.CODEC);
        Optional<Identifier> attunementItemId = optional(tag, "attunementItem", IDENTIFIER_CODEC);

        if (dimension.isEmpty() || anchor.isEmpty()) {
            return Optional.empty();
        }
        if (destinationDimension.isPresent() != destinationAnchor.isPresent()) {
            warn.accept("Skipping malformed portal record: destinationDimension and destinationAnchor must either both be present or both be absent");
            return Optional.empty();
        }

        return Optional.of(new PortalRecord(
                dimension.get(),
                anchor.get(),
                destinationDimension,
                destinationAnchor,
                attunementItemId
        ));
    }


    private static <T> Optional<T> required(CompoundTag tag, String field, Codec<T> codec, String context, Consumer<String> warn) {
        Optional<T> value = tag.read(field, codec);
        if (value.isEmpty()) {
            warn.accept("Skipping malformed " + context + " record: invalid or missing '" + field + "'");
        }
        return value;
    }

    private static <T> Optional<T> optional(CompoundTag tag, String field, Codec<T> codec) {
        return tag.read(field, codec);
    }
}

package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public record OriginPortalRecord(
        int dataVersion,
        UUID id,
        ResourceKey<Level> dimension,
        BlockPos canonicalFrameAnchor,
        Direction.Axis axis,
        int frameWidth,
        int frameHeight,
        BlockPos representativePortalPosition,
        UUID linkedPortalId,
        long creationGameTime
) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.store("id", UUIDUtil.STRING_CODEC, id);
        tag.store("dimension", DIMENSION_CODEC, dimension);
        tag.store("canonicalFrameAnchor", BlockPos.CODEC, canonicalFrameAnchor);
        tag.store("axis", Direction.Axis.CODEC, axis);
        tag.store("frameWidth", Codec.INT, frameWidth);
        tag.store("frameHeight", Codec.INT, frameHeight);
        tag.store("representativePortalPosition", BlockPos.CODEC, representativePortalPosition);
        tag.store("linkedPortalId", UUIDUtil.STRING_CODEC, linkedPortalId);
        tag.store("creationGameTime", Codec.LONG, creationGameTime);
        tag.store("dataVersion", Codec.INT, dataVersion);
        return tag;
    }

    public static Optional<OriginPortalRecord> fromTag(CompoundTag tag, Consumer<String> warn) {
        Optional<UUID> id = PortalPersistenceHelper.required(tag, "id", UUIDUtil.STRING_CODEC, "origin portal", warn);
        Optional<ResourceKey<Level>> dimension = PortalPersistenceHelper.required(tag, "dimension", DIMENSION_CODEC, "origin portal", warn);
        Optional<BlockPos> canonicalFrameAnchor = PortalPersistenceHelper.required(tag, "canonicalFrameAnchor", BlockPos.CODEC, "origin portal", warn);
        Optional<Direction.Axis> axis = PortalPersistenceHelper.required(tag, "axis", Direction.Axis.CODEC, "origin portal", warn);
        Optional<Integer> frameWidth = PortalPersistenceHelper.required(tag, "frameWidth", Codec.INT, "origin portal", warn);
        Optional<Integer> frameHeight = PortalPersistenceHelper.required(tag, "frameHeight", Codec.INT, "origin portal", warn);
        Optional<BlockPos> representativePortalPosition = PortalPersistenceHelper.required(
                tag,
                "representativePortalPosition",
                BlockPos.CODEC,
                "origin portal",
                warn
        );
        Optional<UUID> linkedPortalId = PortalPersistenceHelper.required(tag, "linkedPortalId", UUIDUtil.STRING_CODEC, "origin portal", warn);
        Optional<Long> creationGameTime = PortalPersistenceHelper.required(tag, "creationGameTime", Codec.LONG, "origin portal", warn);
        Optional<Integer> dataVersion = PortalPersistenceHelper.required(tag, "dataVersion", Codec.INT, "origin portal", warn);

        if (id.isEmpty()
                || dimension.isEmpty()
                || canonicalFrameAnchor.isEmpty()
                || axis.isEmpty()
                || frameWidth.isEmpty()
                || frameHeight.isEmpty()
                || representativePortalPosition.isEmpty()
                || linkedPortalId.isEmpty()
                || creationGameTime.isEmpty()
                || dataVersion.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new OriginPortalRecord(
                dataVersion.get(),
                id.get(),
                dimension.get(),
                canonicalFrameAnchor.get(),
                axis.get(),
                frameWidth.get(),
                frameHeight.get(),
                representativePortalPosition.get(),
                linkedPortalId.get(),
                creationGameTime.get()
        ));
    }
}

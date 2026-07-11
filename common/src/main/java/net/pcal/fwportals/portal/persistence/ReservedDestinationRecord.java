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

public record ReservedDestinationRecord(
        int dataVersion,
        UUID id,
        ResourceKey<Level> dimension,
        BlockPos reservedDestinationPosition,
        UUID linkedOriginId,
        boolean physicalPortalExists,
        long creationGameTime
) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.store("id", UUIDUtil.STRING_CODEC, id);
        tag.store("dimension", DIMENSION_CODEC, dimension);
        tag.store("reservedDestinationPosition", BlockPos.CODEC, reservedDestinationPosition);
        tag.store("linkedOriginId", UUIDUtil.STRING_CODEC, linkedOriginId);
        tag.store("physicalPortalExists", Codec.BOOL, physicalPortalExists);
        tag.store("creationGameTime", Codec.LONG, creationGameTime);
        tag.store("dataVersion", Codec.INT, dataVersion);
        return tag;
    }

    public static Optional<ReservedDestinationRecord> fromTag(CompoundTag tag, Consumer<String> warn) {
        Optional<UUID> id = PortalPersistenceHelper.required(tag, "id", UUIDUtil.STRING_CODEC, "reserved destination", warn);
        Optional<ResourceKey<Level>> dimension = PortalPersistenceHelper.required(tag, "dimension", DIMENSION_CODEC, "reserved destination", warn);
        Optional<BlockPos> reservedDestinationPosition = PortalPersistenceHelper.required(
                tag,
                "reservedDestinationPosition",
                BlockPos.CODEC,
                "reserved destination",
                warn
        );
        Optional<UUID> linkedOriginId = PortalPersistenceHelper.required(tag, "linkedOriginId", UUIDUtil.STRING_CODEC, "reserved destination", warn);
        Optional<Boolean> physicalPortalExists = PortalPersistenceHelper.required(tag, "physicalPortalExists", Codec.BOOL, "reserved destination", warn);
        Optional<Long> creationGameTime = PortalPersistenceHelper.required(tag, "creationGameTime", Codec.LONG, "reserved destination", warn);
        Optional<Integer> dataVersion = PortalPersistenceHelper.required(tag, "dataVersion", Codec.INT, "reserved destination", warn);

        if (id.isEmpty()
                || dimension.isEmpty()
                || reservedDestinationPosition.isEmpty()
                || linkedOriginId.isEmpty()
                || physicalPortalExists.isEmpty()
                || creationGameTime.isEmpty()
                || dataVersion.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ReservedDestinationRecord(
                dataVersion.get(),
                id.get(),
                dimension.get(),
                reservedDestinationPosition.get(),
                linkedOriginId.get(),
                physicalPortalExists.get(),
                creationGameTime.get()
        ));
    }
}

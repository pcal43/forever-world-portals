package net.pcal.fwportals.portal.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record PortalReservation(
        ResourceKey<Level> dimension,
        BlockPos reservedDestinationPosition,
        Vec3 landingPosition
) {
}

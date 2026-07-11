package net.pcal.fwportals.portal;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalDestinationSelectorTest {

    @Test
    void computesHorizontalDistanceSquared() {
        assertEquals(25L, PortalDestinationSelector.horizontalDistanceSquared(new BlockPos(0, 0, 0), new BlockPos(3, 99, 4)));
    }
}

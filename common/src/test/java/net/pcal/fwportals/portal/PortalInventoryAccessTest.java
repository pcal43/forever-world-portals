package net.pcal.fwportals.portal;

import net.minecraft.world.item.ItemStack;
import net.pcal.fwportals.TestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalInventoryAccessTest {

    @Test
    void emptyInventoryIsAccepted() {
        TestBootstrap.ensureBootstrapped();

        assertTrue(PortalInventoryAccess.areAllEmpty(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
    }
}

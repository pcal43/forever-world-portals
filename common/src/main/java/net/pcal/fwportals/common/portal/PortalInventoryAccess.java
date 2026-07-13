package net.pcal.fwportals.common.portal;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PortalInventoryAccess {

    private PortalInventoryAccess() {
    }

    public static boolean isEmpty(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static boolean areAllEmpty(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

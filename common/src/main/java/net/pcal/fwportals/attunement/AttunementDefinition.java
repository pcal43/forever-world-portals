package net.pcal.fwportals.attunement;

import net.minecraft.world.item.Item;

public record AttunementDefinition(
        String id,
        Item item,
        DestinationTarget target
) {

    public String displayName() {
        return id.replace('_', ' ');
    }
}

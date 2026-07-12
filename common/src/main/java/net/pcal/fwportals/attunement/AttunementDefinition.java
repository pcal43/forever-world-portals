package net.pcal.fwportals.attunement;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public record AttunementDefinition(
        String id,
        @Nullable Item item,
        DestinationTarget target
) {

    public String displayName() {
        return id.replace('_', ' ');
    }
}

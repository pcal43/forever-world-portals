package net.pcal.fwportals.attunement;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AttunementLookup {

    private static final AttunementLookup EMPTY = new AttunementLookup(Map.of());

    private final Map<Item, AttunementDefinition> byItem;

    public static AttunementLookup empty() {
        return EMPTY;
    }

    public static AttunementLookup of(Map<Item, AttunementDefinition> byItem) {
        return new AttunementLookup(byItem);
    }

    private AttunementLookup(Map<Item, AttunementDefinition> byItem) {
        this.byItem = Map.copyOf(new LinkedHashMap<>(byItem));
    }

    public Optional<AttunementDefinition> resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return resolve(stack.getItem());
    }

    public Optional<AttunementDefinition> resolve(Item item) {
        return Optional.ofNullable(byItem.get(item));
    }

    public int size() {
        return byItem.size();
    }
}

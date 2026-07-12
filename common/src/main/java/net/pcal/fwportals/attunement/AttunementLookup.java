package net.pcal.fwportals.attunement;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AttunementLookup {

    private static final AttunementLookup EMPTY = new AttunementLookup(null, Map.of());

    private final @Nullable AttunementDefinition defaultAttunement;
    private final Map<Item, AttunementDefinition> byItem;

    public static AttunementLookup empty() {
        return EMPTY;
    }

    public static AttunementLookup of(AttunementDefinition defaultAttunement, Map<Item, AttunementDefinition> byItem) {
        return new AttunementLookup(defaultAttunement, byItem);
    }

    private AttunementLookup(@Nullable AttunementDefinition defaultAttunement, Map<Item, AttunementDefinition> byItem) {
        this.defaultAttunement = defaultAttunement;
        this.byItem = Map.copyOf(new LinkedHashMap<>(byItem));
    }

    public AttunementDefinition defaultAttunement() {
        if (defaultAttunement == null) {
            throw new IllegalStateException("Attunement lookup has no default attunement.");
        }
        return defaultAttunement;
    }

    public BiomeDestinationTarget defaultTarget() {
        DestinationTarget target = defaultAttunement().target();
        if (!(target instanceof BiomeDestinationTarget biomeTarget)) {
            throw new IllegalStateException("Unsupported default destination target type " + target.getClass().getName());
        }
        return biomeTarget;
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
        return byItem.size() + (defaultAttunement == null ? 0 : 1);
    }
}

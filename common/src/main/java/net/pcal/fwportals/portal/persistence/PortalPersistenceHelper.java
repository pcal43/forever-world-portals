package net.pcal.fwportals.portal.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Consumer;

final class PortalPersistenceHelper {

    static <T> Optional<T> required(CompoundTag tag, String field, Codec<T> codec, String context, Consumer<String> warn) {
        Optional<T> value = tag.read(field, codec);
        if (value.isEmpty()) {
            warn.accept("Skipping malformed " + context + " record: invalid or missing '" + field + "'");
        }
        return value;
    }

    static <T> Optional<T> optional(CompoundTag tag, String field, Codec<T> codec) {
        return tag.read(field, codec);
    }

    private PortalPersistenceHelper() {
    }
}

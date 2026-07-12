package net.pcal.fwportals.attunement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AttunementLoader {

    private static final String ATTUNEMENT_DIRECTORY = "forever_world_portals";
    private static final String ATTUNEMENT_FILE_PATH = "forever_world_portals/attunements.json";
    private static final String DEFAULT_ATTUNEMENT_ID = "default";

    static AttunementLookup load(ResourceManager resourceManager, HolderLookup.Provider registryLookup) {
        return load(readSources(resourceManager), registryLookup);
    }

    static AttunementLookup load(List<ResourceSource> resourceSources, HolderLookup.Provider registryLookup) {
        Map<String, RawDefinition> merged = new LinkedHashMap<>();
        for (ResourceSource resourceSource : resourceSources.stream()
                .sorted(Comparator.comparingInt(ResourceSource::packPriority).thenComparing(source -> source.resourceId().toString()))
                .toList()) {
            JsonObject root = parseRootObject(resourceSource);
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String attunementId = entry.getKey().trim();
                if (attunementId.isBlank()) {
                    throw invalid(resourceSource, "<blank>", "logical attunement ID must not be blank");
                }
                if (!entry.getValue().isJsonObject()) {
                    throw invalid(resourceSource, attunementId, "definition must be a JSON object");
                }
                merged.put(attunementId, parseRawDefinition(attunementId, resourceSource, entry.getValue().getAsJsonObject()));
            }
        }

        HolderLookup.RegistryLookup<Item> items = registryLookup.lookupOrThrow(Registries.ITEM);
        HolderLookup.RegistryLookup<Biome> biomes = registryLookup.lookupOrThrow(Registries.BIOME);
        RawDefinition defaultRawDefinition = merged.get(DEFAULT_ATTUNEMENT_ID);
        if (defaultRawDefinition == null) {
            throw new IllegalStateException("Missing required 'default' attunement.");
        }

        AttunementDefinition defaultAttunement = new AttunementDefinition(
                defaultRawDefinition.id(),
                null,
                resolveBiomeTarget(defaultRawDefinition, biomes),
                defaultRawDefinition.colorRgb(),
                defaultRawDefinition.particleId()
        );
        Map<Item, AttunementDefinition> byItem = new LinkedHashMap<>();
        Map<Item, String> idsByItem = new LinkedHashMap<>();

        for (RawDefinition rawDefinition : merged.values()) {
            if (rawDefinition.id().equals(DEFAULT_ATTUNEMENT_ID)) {
                continue;
            }
            Item item = resolveItem(rawDefinition, items);
            BiomeDestinationTarget target = resolveBiomeTarget(rawDefinition, biomes);
            String previousId = idsByItem.putIfAbsent(item, rawDefinition.id());
            if (previousId != null) {
                throw invalid(
                        rawDefinition.source(),
                        rawDefinition.id(),
                        "item '" + rawDefinition.itemId() + "' is already used by attunement '" + previousId + "'"
                );
            }

            byItem.put(item, new AttunementDefinition(
                    rawDefinition.id(),
                    item,
                    target,
                    rawDefinition.colorRgb(),
                    rawDefinition.particleId()
            ));
        }

        return AttunementLookup.of(defaultAttunement, byItem);
    }

    private static List<ResourceSource> readSources(ResourceManager resourceManager) {
        Map<String, Integer> packPriority = new LinkedHashMap<>();
        int index = 0;
        for (var iterator = resourceManager.listPacks().iterator(); iterator.hasNext(); ) {
            packPriority.putIfAbsent(iterator.next().packId(), index++);
        }

        List<ResourceSource> sources = new ArrayList<>();
        resourceManager.listResourceStacks(ATTUNEMENT_DIRECTORY, id -> id.getPath().equals(ATTUNEMENT_FILE_PATH))
                .forEach((resourceId, stack) -> {
                    for (Resource resource : stack) {
                        sources.add(readSource(resourceId, resource, packPriority.getOrDefault(resource.sourcePackId(), Integer.MAX_VALUE)));
                    }
                });
        return sources;
    }

    private static ResourceSource readSource(Identifier resourceId, Resource resource, int packPriority) {
        try (Reader reader = resource.openAsReader()) {
            return new ResourceSource(resourceId, resource.sourcePackId(), packPriority, readerToString(reader));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read attunement resource " + describeSource(resourceId, resource.sourcePackId()) + ": " + ex.getMessage(), ex);
        }
    }

    private static String readerToString(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[2048];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }

    private static JsonObject parseRootObject(ResourceSource resourceSource) {
        try {
            JsonElement root = JsonParser.parseString(resourceSource.contents());
            if (!root.isJsonObject()) {
                throw invalid(resourceSource, "<file>", "root must be a JSON object");
            }
            return root.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new IllegalStateException(
                    "Failed to parse attunement resource " + describeSource(resourceSource.resourceId(), resourceSource.sourcePackId()) + ": " + ex.getMessage(),
                    ex
            );
        }
    }

    private static RawDefinition parseRawDefinition(String id, ResourceSource source, JsonObject object) {
        @Nullable Identifier itemId = parseOptionalIdentifier(source, id, object, "item");
        if (id.equals(DEFAULT_ATTUNEMENT_ID)) {
            if (itemId != null) {
                throw new IllegalStateException("The 'default' attunement must not specify an item.");
            }
        } else if (itemId == null) {
            throw new IllegalStateException("Attunement '" + id + "' must specify an item.");
        }
        Identifier dimensionId = parseIdentifier(source, id, object, "dimension");
        JsonArray biomeArray = requiredArray(source, id, object, "biomes");
        if (biomeArray.isEmpty()) {
            throw invalid(source, id, "field 'biomes' must not be empty");
        }
        int colorRgb = parseColorRgb(source, id, object, "color");
        @Nullable Identifier particleId = parseOptionalIdentifier(source, id, object, "particle");
        if (particleId != null && !BuiltInRegistries.PARTICLE_TYPE.containsKey(particleId)) {
            throw invalid(source, id, "unknown particle '" + particleId + "'");
        }

        List<Identifier> biomeIds = new ArrayList<>();
        for (JsonElement element : biomeArray) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw invalid(source, id, "field 'biomes' must contain only biome ID strings");
            }
            biomeIds.add(parseIdentifier(source, id, element.getAsString(), "biomes"));
        }

        return new RawDefinition(id, itemId, dimensionId, biomeIds, colorRgb, particleId, source);
    }

    private static Item resolveItem(RawDefinition rawDefinition, HolderLookup.RegistryLookup<Item> items) {
        if (rawDefinition.itemId() == null) {
            throw invalid(rawDefinition.source(), rawDefinition.id(), "missing field 'item'");
        }
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, rawDefinition.itemId());
        return items.get(itemKey)
                .orElseThrow(() -> invalid(rawDefinition.source(), rawDefinition.id(), "unknown item '" + rawDefinition.itemId() + "'"))
                .value();
    }

    private static BiomeDestinationTarget resolveBiomeTarget(
            RawDefinition rawDefinition,
            HolderLookup.RegistryLookup<Biome> biomes
    ) {
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, rawDefinition.dimensionId());
        if (!dimensionKey.equals(Level.OVERWORLD)) {
            throw invalid(rawDefinition.source(), rawDefinition.id(), "unsupported dimension '" + rawDefinition.dimensionId() + "'; only minecraft:overworld is supported");
        }

        List<ResourceKey<Biome>> biomeKeys = new ArrayList<>();
        for (Identifier biomeId : rawDefinition.biomeIds()) {
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
            if (biomes.get(biomeKey).isEmpty()) {
                throw invalid(rawDefinition.source(), rawDefinition.id(), "unknown biome '" + biomeId + "'");
            }
            biomeKeys.add(biomeKey);
        }

        return new BiomeDestinationTarget(dimensionKey, Set.copyOf(biomeKeys));
    }

    private static Identifier parseIdentifier(ResourceSource source, String attunementId, JsonObject object, String field) {
        Identifier identifier = parseOptionalIdentifier(source, attunementId, object, field);
        if (identifier == null) {
            throw invalid(source, attunementId, "missing field '" + field + "'");
        }
        return identifier;
    }

    private static @Nullable Identifier parseOptionalIdentifier(ResourceSource source, String attunementId, JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw invalid(source, attunementId, "field '" + field + "' must be a string");
        }
        return parseIdentifier(source, attunementId, element.getAsString(), field);
    }

    private static Identifier parseIdentifier(ResourceSource source, String attunementId, String value, String field) {
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ex) {
            throw invalid(source, attunementId, "invalid identifier '" + value + "' for field '" + field + "'");
        }
    }

    private static JsonArray requiredArray(ResourceSource source, String attunementId, JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null) {
            throw invalid(source, attunementId, "missing field '" + field + "'");
        }
        if (!element.isJsonArray()) {
            throw invalid(source, attunementId, "field '" + field + "' must be an array");
        }
        return element.getAsJsonArray();
    }

    private static int parseColorRgb(ResourceSource source, String attunementId, JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null) {
            throw invalid(source, attunementId, "missing field '" + field + "'");
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw invalid(source, attunementId, "field '" + field + "' must be a string");
        }

        String value = element.getAsString().trim();
        if (!value.matches("^#[0-9a-fA-F]{6}$")) {
            throw invalid(source, attunementId, "field '" + field + "' must be a hex RGB string like '#A1B2C3'");
        }
        return Integer.parseInt(value.substring(1), 16);
    }

    private static IllegalStateException invalid(ResourceSource source, String attunementId, String message) {
        return new IllegalStateException(
                "Invalid attunement '" + attunementId + "' in " + describeSource(source.resourceId(), source.sourcePackId()) + ": " + message
        );
    }

    private static IllegalStateException invalid(ResourceSource source, String attunementId, String message, Throwable cause) {
        return new IllegalStateException(
                "Invalid attunement '" + attunementId + "' in " + describeSource(source.resourceId(), source.sourcePackId()) + ": " + message,
                cause
        );
    }

    private static String describeSource(Identifier resourceId, String sourcePackId) {
        return resourceId + " from pack '" + sourcePackId + "'";
    }

    record ResourceSource(Identifier resourceId, String sourcePackId, int packPriority, String contents) {
    }

    private record RawDefinition(
            String id,
            @Nullable Identifier itemId,
            Identifier dimensionId,
            List<Identifier> biomeIds,
            int colorRgb,
            @Nullable Identifier particleId,
            ResourceSource source
    ) {
    }

    private AttunementLoader() {
    }
}

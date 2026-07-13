package net.pcal.fwportals.client;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class TintedPortalBlockStateModelPart implements BlockStateModelPart {

    private final BlockStateModelPart delegate;
    private final TextureAtlasSprite sprite;
    private final Map<Direction, List<BakedQuad>> quadCache = new EnumMap<>(Direction.class);
    private List<BakedQuad> nullDirectionQuads;

    TintedPortalBlockStateModelPart(BlockStateModelPart delegate, TextureAtlasSprite sprite) {
        this.delegate = delegate;
        this.sprite = sprite;
    }

    @Override
    public List<BakedQuad> getQuads(Direction direction) {
        if (direction == null) {
            if (nullDirectionQuads == null) {
                nullDirectionQuads = transformQuads(delegate.getQuads(null));
            }
            return nullDirectionQuads;
        }
        return quadCache.computeIfAbsent(direction, key -> transformQuads(delegate.getQuads(key)));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return delegate.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return delegate.materialFlags();
    }

    private List<BakedQuad> transformQuads(List<BakedQuad> quads) {
        return quads.stream().map(this::transformQuad).toList();
    }

    private BakedQuad transformQuad(BakedQuad quad) {
        BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
        TextureAtlasSprite originalSprite = materialInfo.sprite();
        BakedQuad.MaterialInfo tintedMaterialInfo = new BakedQuad.MaterialInfo(
                sprite,
                materialInfo.layer(),
                materialInfo.itemRenderType(),
                0,
                materialInfo.shade(),
                materialInfo.lightEmission()
        );
        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                remapPackedUv(quad.packedUV0(), originalSprite, sprite),
                remapPackedUv(quad.packedUV1(), originalSprite, sprite),
                remapPackedUv(quad.packedUV2(), originalSprite, sprite),
                remapPackedUv(quad.packedUV3(), originalSprite, sprite),
                quad.direction(),
                tintedMaterialInfo
        );
    }

    private static long remapPackedUv(long packedUv, TextureAtlasSprite originalSprite, TextureAtlasSprite targetSprite) {
        float originalU = UVPair.unpackU(packedUv);
        float originalV = UVPair.unpackV(packedUv);
        float localU = normalize(originalU, originalSprite.getU0(), originalSprite.getU1());
        float localV = normalize(originalV, originalSprite.getV0(), originalSprite.getV1());
        float remappedU = lerp(targetSprite.getU0(), targetSprite.getU1(), localU);
        float remappedV = lerp(targetSprite.getV0(), targetSprite.getV1(), localV);
        return UVPair.pack(remappedU, remappedV);
    }

    private static float normalize(float value, float min, float max) {
        float span = max - min;
        if (span == 0.0F) {
            return 0.0F;
        }
        return (value - min) / span;
    }

    private static float lerp(float min, float max, float amount) {
        return min + (max - min) * amount;
    }
}

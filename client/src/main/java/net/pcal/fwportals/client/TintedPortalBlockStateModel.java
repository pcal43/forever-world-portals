package net.pcal.fwportals.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

final class TintedPortalBlockStateModel implements BlockStateModel {

    private final BlockStateModel delegate;
    private final TextureAtlasSprite sprite;

    TintedPortalBlockStateModel(BlockStateModel delegate, TextureAtlasSprite sprite) {
        this.delegate = delegate;
        this.sprite = sprite;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        List<BlockStateModelPart> originalParts = new ArrayList<>();
        delegate.collectParts(random, originalParts);
        for (BlockStateModelPart originalPart : originalParts) {
            parts.add(new TintedPortalBlockStateModelPart(originalPart, sprite));
        }
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return delegate.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return delegate.materialFlags();
    }
}

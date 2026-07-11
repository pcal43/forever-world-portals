package net.pcal.fwportals;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.Level;

public record ForeverWorldPortalsConfig(
        boolean enabled,
        Level logLevel,
        Identifier frameBlockId,
        Block frameBlock,
        Identifier activationItemId,
        Item activationItem,
        int minimumPortalSeparationBlocks,
        int destinationSearchAttempts
) {

    static ForeverWorldPortalsConfig defaults() {
        Block frameBlock = net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK;
        Item activationItem = net.minecraft.world.item.Items.FLINT_AND_STEEL;
        return new ForeverWorldPortalsConfig(
                true,
                Level.INFO,
                Identifier.parse("minecraft:diamond_block"),
                frameBlock,
                Identifier.parse("minecraft:flint_and_steel"),
                activationItem,
                25000,
                64
        );
    }
}

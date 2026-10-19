package xfacthd.framedblocks.common.block.slopepanelcorner;

import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.common.data.BlockType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

// TODO 1.21.6: remove
public class FramedSolidPrismSlopePanelCornerWallBlock extends FramedPrismSlopePanelCornerWallBlock
{
    public FramedSolidPrismSlopePanelCornerWallBlock(BlockType type)
    {
        super(type);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FramedProperties.SOLID);
    }
}

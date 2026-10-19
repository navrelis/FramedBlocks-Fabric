package xfacthd.framedblocks.common.blockentity.special;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.common.FBContent;

public class FramedBannerBlockEntity extends FramedBlockEntity
{
    public FramedBannerBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_BANNER.value(), pos, state);
    }
}

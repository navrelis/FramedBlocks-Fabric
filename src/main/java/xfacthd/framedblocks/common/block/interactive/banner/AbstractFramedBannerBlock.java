package xfacthd.framedblocks.common.block.interactive.banner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.common.block.FramedBlock;
import xfacthd.framedblocks.common.blockentity.special.FramedBannerBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;

abstract class AbstractFramedBannerBlock extends FramedBlock
{
    protected AbstractFramedBannerBlock(BlockType blockType)
    {
        super(blockType, props -> props
                .forceSolidOn()
                .noCollission()
                .strength(1F)
        );
    }

    protected abstract Direction getAttachDir(BlockState state);

    @Override
    @SuppressWarnings("deprecation")
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        Direction attachDir = getAttachDir(state);
        return level.getBlockState(pos.relative(attachDir)).isSolid();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState adjState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos adjPos
    )
    {
        if (side == getAttachDir(state) && !state.canSurvive(level, pos))
        {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, side, adjState, level, pos, adjPos);
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState state)
    {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedBannerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public BlockState getItemModelSource()
    {
        return null;
    }

    @Override
    public boolean shouldRenderAsBlockInJadeTooltip()
    {
        return false;
    }
}

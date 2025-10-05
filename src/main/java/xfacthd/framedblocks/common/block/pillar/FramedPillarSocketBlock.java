package xfacthd.framedblocks.common.block.pillar;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.PlacementStateBuilder;
import xfacthd.framedblocks.common.block.FramedBlock;
import xfacthd.framedblocks.common.data.BlockType;

public class FramedPillarSocketBlock extends FramedBlock
{
    public FramedPillarSocketBlock(BlockType type)
    {
        super(type);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING, FramedProperties.SOLID, BlockStateProperties.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return PlacementStateBuilder.of(this, context)
                .withTargetFacing()
                .withWater()
                .build();
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN);
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN);
    }
}

package xfacthd.framedblocks.common.block.cube;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.PlacementStateBuilder;
import xfacthd.framedblocks.common.block.FramedBlock;
import xfacthd.framedblocks.common.data.BlockType;

/**
 * Reimplements the logic of {@link net.minecraft.world.level.block.RedstoneLampBlock} on top of {@link FramedBlock}
 * instead of extending the vanilla block: the logic is only a few lines while extending it would require copying all
 * the camo handling of the other {@code IFramedBlock}s that have to extend a vanilla block
 */
public class FramedRedstoneLampBlock extends FramedBlock
{
    public FramedRedstoneLampBlock()
    {
        super(BlockType.FRAMED_REDSTONE_LAMP, props -> props.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FramedProperties.SOLID, BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return PlacementStateBuilder.of(this, ctx)
                .withCustom((state, modCtx) -> state.setValue(
                        BlockStateProperties.LIT, modCtx.getLevel().hasNeighborSignal(modCtx.getClickedPos())
                ))
                .build();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (!level.isClientSide())
        {
            boolean lit = state.getValue(BlockStateProperties.LIT);
            if (lit != level.hasNeighborSignal(pos))
            {
                if (lit)
                {
                    level.scheduleTick(pos, this, 4);
                }
                else
                {
                    level.setBlock(pos, state.cycle(BlockStateProperties.LIT), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (state.getValue(BlockStateProperties.LIT) && !level.hasNeighborSignal(pos))
        {
            level.setBlock(pos, state.cycle(BlockStateProperties.LIT), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos)
    {
        return Math.max(state.getLightEmission(), super.getLightEmission(state, level, pos));
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState();
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return state;
    }
}

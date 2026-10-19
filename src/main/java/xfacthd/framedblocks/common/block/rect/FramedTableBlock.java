package xfacthd.framedblocks.common.block.rect;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.PlacementStateBuilder;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.block.FramedBlock;
import xfacthd.framedblocks.common.data.BlockType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A table with a full top slab and four legs. The horizontal properties are set when the neighbour on that side is
 * another table, the legs on the corners next to a connected side are removed
 */
public class FramedTableBlock extends FramedBlock
{
    private static final Map<Direction, BooleanProperty> CONNECTIONS = Util.make(new EnumMap<>(Direction.class), map ->
    {
        map.put(Direction.NORTH, BlockStateProperties.NORTH);
        map.put(Direction.EAST, BlockStateProperties.EAST);
        map.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        map.put(Direction.WEST, BlockStateProperties.WEST);
    });

    public FramedTableBlock()
    {
        super(BlockType.FRAMED_TABLE);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.WEST, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(
                BlockStateProperties.NORTH,
                BlockStateProperties.EAST,
                BlockStateProperties.SOUTH,
                BlockStateProperties.WEST,
                BlockStateProperties.WATERLOGGED
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return PlacementStateBuilder.of(this, ctx)
                .withWater()
                .withCustom((state, placeCtx) -> updateAllConnections(state, placeCtx.getLevel(), placeCtx.getClickedPos()))
                .build();
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
        if (!Utils.isY(side))
        {
            state = state.setValue(CONNECTIONS.get(side), adjState.is(this));
        }
        return super.updateShape(state, side, adjState, level, pos, adjPos);
    }

    private BlockState updateAllConnections(BlockState state, LevelReader level, BlockPos pos)
    {
        for (Direction side : Direction.Plane.HORIZONTAL)
        {
            BooleanProperty prop = CONNECTIONS.get(side);
            state = state.setValue(prop, level.getBlockState(pos.relative(side)).is(this));
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        // The table is rotationally symmetric, rotating the connections without level access would only desync them
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        return remap(state, rot::rotate);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return remap(state, mirror::mirror);
    }

    private static BlockState remap(BlockState state, UnaryOperator<Direction> mapper)
    {
        BlockState newState = state;
        for (Direction side : Direction.Plane.HORIZONTAL)
        {
            BooleanProperty from = CONNECTIONS.get(side);
            BooleanProperty to = CONNECTIONS.get(mapper.apply(side));
            newState = newState.setValue(to, state.getValue(from));
        }
        return newState;
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState();
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState();
    }
}

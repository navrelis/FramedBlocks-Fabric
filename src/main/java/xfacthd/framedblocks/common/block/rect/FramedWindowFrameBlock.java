package xfacthd.framedblocks.common.block.rect;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.PlacementStateBuilder;
import xfacthd.framedblocks.common.block.FramedBlock;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.shapes.rect.WindowFrameShapes;

import java.util.function.UnaryOperator;

/**
 * A thin frame which sits flush against the block face opposite of its {@link #FACING}. Neighbouring frames with the same
 * facing in the plane of the frame are connected into one large frame with a single continuous opening
 */
public class FramedWindowFrameBlock extends FramedBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    // Connections in the frame's local space when looking at the front of the frame, see WindowFrameShapes
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    private static final BooleanProperty[] CONNECTIONS = { UP, DOWN, LEFT, RIGHT };

    public FramedWindowFrameBlock()
    {
        super(BlockType.FRAMED_WINDOW_FRAME);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(LEFT, false)
                .setValue(RIGHT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, UP, DOWN, LEFT, RIGHT, BlockStateProperties.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return PlacementStateBuilder.of(this, ctx)
                .withTargetFacing(true)
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
        BooleanProperty prop = getConnectionProperty(state.getValue(FACING), side);
        if (prop != null)
        {
            state = state.setValue(prop, connectsTo(state, adjState));
        }
        return super.updateShape(state, side, adjState, level, pos, adjPos);
    }

    private BlockState updateAllConnections(BlockState state, LevelReader level, BlockPos pos)
    {
        Direction facing = state.getValue(FACING);
        for (BooleanProperty prop : CONNECTIONS)
        {
            Direction side = getConnectionDirection(facing, prop);
            state = state.setValue(prop, connectsTo(state, level.getBlockState(pos.relative(side))));
        }
        return state;
    }

    private boolean connectsTo(BlockState state, BlockState adjState)
    {
        return adjState.is(this) && adjState.getValue(FACING) == state.getValue(FACING);
    }

    @Override
    public boolean doesBlockOccludeBeaconBeam(BlockState state, LevelReader level, BlockPos pos)
    {
        // The frame has an open center and therefore never fully covers the beam, the bounding box based
        // check of the super implementation would report a vertical frame as covering it
        return false;
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        // Connections are derived from the neighbours and cannot be recomputed without level access here
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
        Direction oldFacing = state.getValue(FACING);
        Direction newFacing = mapper.apply(oldFacing);

        BlockState newState = state.setValue(FACING, newFacing);
        for (BooleanProperty prop : CONNECTIONS)
        {
            Direction side = mapper.apply(getConnectionDirection(oldFacing, prop));
            BooleanProperty newProp = getConnectionProperty(newFacing, side);
            if (newProp != null)
            {
                newState = newState.setValue(newProp, state.getValue(prop));
            }
        }
        return newState;
    }

    /**
     * @return the connection property of a frame with the given facing which covers the given world direction or null
     * if the direction is on the facing axis
     */
    @Nullable
    public static BooleanProperty getConnectionProperty(Direction facing, Direction side)
    {
        if (side.getAxis() == facing.getAxis())
        {
            return null;
        }
        Direction up = WindowFrameShapes.upDir(facing);
        Direction right = WindowFrameShapes.rightDir(facing);
        if (side == up) { return UP; }
        if (side == up.getOpposite()) { return DOWN; }
        if (side == right) { return RIGHT; }
        return LEFT;
    }

    /**
     * @return the world direction a connection property of a frame with the given facing points to
     */
    public static Direction getConnectionDirection(Direction facing, BooleanProperty prop)
    {
        Direction up = WindowFrameShapes.upDir(facing);
        Direction right = WindowFrameShapes.rightDir(facing);
        if (prop == UP) { return up; }
        if (prop == DOWN) { return up.getOpposite(); }
        if (prop == RIGHT) { return right; }
        return right.getOpposite();
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState().setValue(FACING, Direction.SOUTH);
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState().setValue(FACING, Direction.SOUTH);
    }
}

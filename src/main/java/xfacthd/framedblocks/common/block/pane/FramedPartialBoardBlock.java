package xfacthd.framedblocks.common.block.pane;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.PlacementStateBuilder;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.block.FramedBlock;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CompoundDirection;

import java.util.Objects;

/**
 * Shared block class of the Half Board, Corner Board and Inner Corner Board
 */
public class FramedPartialBoardBlock extends FramedBlock
{
    private final boolean half;

    public FramedPartialBoardBlock(BlockType type)
    {
        super(type);
        this.half = type == BlockType.FRAMED_HALF_BOARD;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(PropertyHolder.FACING_DIR, BlockStateProperties.WATERLOGGED, FramedProperties.SOLID);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return getStateForPlacement(this, ctx, half);
    }

    @Nullable
    public static BlockState getStateForPlacement(Block block, BlockPlaceContext ctx, boolean cross)
    {
        return PlacementStateBuilder.of(block, ctx)
                .withCustom((state, modCtx) ->
                {
                    Direction face = modCtx.getClickedFace().getOpposite();
                    Direction orientation;
                    if (cross)
                    {
                        orientation = Utils.getDirByCross(face, modCtx.getClickLocation());
                    }
                    else
                    {
                        orientation = Utils.getDirByCorner(face, modCtx.getClickLocation());
                    }
                    return state.setValue(PropertyHolder.FACING_DIR, CompoundDirection.of(face, orientation));
                })
                .withWater()
                .build();
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        return rotateState(state, face, rot);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        return rotateState(state, Direction.UP, rot);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return mirrorState(state, mirror, !half);
    }

    @Override
    public BlockState getItemModelSource()
    {
        if (getBlockType() == BlockType.FRAMED_INNER_CORNER_BOARD)
        {
            return defaultBlockState().setValue(PropertyHolder.FACING_DIR, CompoundDirection.DOWN_SOUTH);
        }
        return defaultBlockState();
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return defaultBlockState().setValue(PropertyHolder.FACING_DIR, CompoundDirection.DOWN_WEST);
    }



    /**
     * Rotates the state's {@link PropertyHolder#FACING_DIR} 90 degrees clockwise (as seen when looking at the given face
     * from the outside) around the axis of the given face, {@code rot} controls the amount of rotation steps
     */
    public static BlockState rotateState(BlockState state, Direction face, Rotation rot)
    {
        int steps = switch (rot)
        {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
        if (steps == 0)
        {
            return state;
        }
        if (!Utils.isPositive(face))
        {
            // Clockwise as seen from the negative side is counterclockwise as seen from the positive side
            steps = 4 - steps;
        }

        Direction.Axis axis = face.getAxis();
        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        Direction dir = cmpDir.direction();
        Direction orientation = cmpDir.orientation();
        for (int i = 0; i < steps; i++)
        {
            dir = rotateClockwiseAroundAxis(dir, axis);
            orientation = rotateClockwiseAroundAxis(orientation, axis);
        }
        return state.setValue(PropertyHolder.FACING_DIR, CompoundDirection.of(dir, orientation));
    }

    public static BlockState mirrorState(BlockState state, Mirror mirror, boolean corner)
    {
        if (mirror == Mirror.NONE)
        {
            return state;
        }

        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        // Mirroring flips the handedness of corners, so the second corner direction becomes the new orientation
        Direction orientation = corner ? getCornerDirTwo(cmpDir) : cmpDir.orientation();
        return state.setValue(PropertyHolder.FACING_DIR, CompoundDirection.of(
                mirror.mirror(cmpDir.direction()),
                mirror.mirror(orientation)
        ));
    }

    /**
     * Rotates the given direction clockwise around the given axis as seen from the positive end of that axis
     */
    private static Direction rotateClockwiseAroundAxis(Direction dir, Direction.Axis axis)
    {
        if (dir.getAxis() == axis)
        {
            return dir;
        }
        int x = dir.getStepX();
        int y = dir.getStepY();
        int z = dir.getStepZ();
        Direction result = switch (axis)
        {
            case X -> Utils.dirByNormal(x, z, -y);
            case Y -> Utils.dirByNormal(-z, y, x);
            case Z -> Utils.dirByNormal(y, -x, z);
        };
        return Objects.requireNonNull(result);
    }

    /**
     * {@return the second edge direction of the corner of a corner-type board}
     */
    public static Direction getCornerDirTwo(CompoundDirection cmpDir)
    {
        Direction face = cmpDir.direction();
        Direction dirOne = cmpDir.orientation();
        if (Utils.isY(face))
        {
            return dirOne.getCounterClockWise();
        }
        else if (Utils.isPositive(face))
        {
            return dirOne.getClockWise(face.getAxis());
        }
        else
        {
            return dirOne.getCounterClockWise(face.getAxis());
        }
    }
}

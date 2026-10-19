package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.common.block.rect.FramedWindowFrameBlock;

import java.util.ArrayList;
import java.util.List;

public final class WindowFrameShapes
{
    public static final float DEPTH = 4F;
    public static final float BORDER = 2F;

    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        return RectShapes.generate(states, WindowFrameShapes::boxes);
    }

    public static List<PxBox> boxes(BlockState state)
    {
        return boxes(
                state.getValue(FramedWindowFrameBlock.FACING),
                state.getValue(FramedWindowFrameBlock.UP),
                state.getValue(FramedWindowFrameBlock.DOWN),
                state.getValue(FramedWindowFrameBlock.LEFT),
                state.getValue(FramedWindowFrameBlock.RIGHT)
        );
    }

    /**
     * Builds the boxes of the frame in its local coordinate system: {@code r} runs to the right when looking at the
     * front of the frame, {@code u} runs upwards and {@code d} runs from the wall the frame sits on towards the front.
     * A side that is connected has no border bar. Each corner square exists if at least one of its two adjacent sides
     * is not connected, which is achieved by merging corner squares into the adjacent bars wherever possible
     */
    public static List<PxBox> boxes(Direction facing, boolean up, boolean down, boolean left, boolean right)
    {
        Direction rightDir = rightDir(facing);
        Direction upDir = upDir(facing);
        float inner0 = BORDER;
        float inner1 = 16F - BORDER;

        List<PxBox> boxes = new ArrayList<>(4);
        // Top and bottom strips span the full width and include both corners
        if (!up)
        {
            boxes.add(PxBox.local(rightDir, upDir, facing, 0, inner1, 0, 16, 16, DEPTH));
        }
        if (!down)
        {
            boxes.add(PxBox.local(rightDir, upDir, facing, 0, 0, 0, 16, inner0, DEPTH));
        }
        // Left and right bars span the open height and include the corners which are not part of a top/bottom strip
        float barMin = down ? 0F : inner0;
        float barMax = up ? 16F : inner1;
        if (!left)
        {
            boxes.add(PxBox.local(rightDir, upDir, facing, 0, barMin, 0, inner0, barMax, DEPTH));
        }
        if (!right)
        {
            boxes.add(PxBox.local(rightDir, upDir, facing, inner1, barMin, 0, 16, barMax, DEPTH));
        }
        return List.copyOf(boxes);
    }

    /**
     * @return the world direction of the frame's local "up" when looking at its front
     */
    public static Direction upDir(Direction facing)
    {
        return facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
    }

    /**
     * @return the world direction of the frame's local "right" when looking at its front
     */
    public static Direction rightDir(Direction facing)
    {
        return switch (facing)
        {
            case UP -> Direction.EAST;
            case DOWN -> Direction.WEST;
            default -> facing.getCounterClockWise();
        };
    }



    private WindowFrameShapes() { }
}

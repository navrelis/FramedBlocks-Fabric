package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.shapes.ShapeProvider;

import java.util.List;

public final class WallShelfShapes
{
    private static final float[] BRACKET_DEPTHS = { 6F, 4F, 2F };

    public static ShapeProvider generate(ImmutableList<BlockState> states)
    {
        return RectShapes.generate(states, state -> boxes(
                state.getValue(FramedProperties.FACING_HOR),
                state.getValue(FramedProperties.TOP)
        ));
    }

    /**
     * @param facing The direction pointing away from the wall
     * @param top Whether the board is at the top (y 14 - 16) or in the lower half (y 6 - 8) of the block, the
     *            brackets are always placed below the board
     */
    public static List<PxBox> boxes(Direction facing, boolean top)
    {
        Direction right = facing.getClockWise();
        float boardMin = top ? 14F : 6F;

        PxBox[] boxes = new PxBox[1 + BRACKET_DEPTHS.length * 2];
        int idx = 0;
        boxes[idx++] = PxBox.local(right, Direction.UP, facing, 0, boardMin, 0, 16, boardMin + 2F, 8);
        for (int i = 0; i < BRACKET_DEPTHS.length; i++)
        {
            float minY = boardMin - 2F * (i + 1);
            boxes[idx++] = PxBox.local(right, Direction.UP, facing, 2, minY, 0, 4, minY + 2F, BRACKET_DEPTHS[i]);
            boxes[idx++] = PxBox.local(right, Direction.UP, facing, 12, minY, 0, 14, minY + 2F, BRACKET_DEPTHS[i]);
        }
        return List.of(boxes);
    }



    private WallShelfShapes() { }
}

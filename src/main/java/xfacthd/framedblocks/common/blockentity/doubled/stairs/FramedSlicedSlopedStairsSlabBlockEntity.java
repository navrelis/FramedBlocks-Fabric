package xfacthd.framedblocks.common.blockentity.doubled.stairs;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;
import xfacthd.framedblocks.api.util.Triangle;

public class FramedSlicedSlopedStairsSlabBlockEntity extends FramedDoubleBlockEntity
{
    static final Triangle[] TRIANGLES = Util.make(new Triangle[4], arr ->
    {
        arr[Direction.NORTH.get2DDataValue()] = new Triangle(new Vec3(1, .5, 1), new Vec3(0, .5, 1), new Vec3(1, .5, 0));
        arr[Direction.SOUTH.get2DDataValue()] = new Triangle(new Vec3(0, .5, 0), new Vec3(1, .5, 0), new Vec3(0, .5, 1));
        arr[Direction.WEST.get2DDataValue()] = new Triangle(new Vec3(1, .5, 0), new Vec3(0, .5, 0), new Vec3(1, .5, 1));
        arr[Direction.EAST.get2DDataValue()] = new Triangle(new Vec3(0, .5, 1), new Vec3(1, .5, 1), new Vec3(0, .5, 0));
    });

    public FramedSlicedSlopedStairsSlabBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_SLICED_SLOPED_DOUBLE_STAIRS_SLAB.value(), pos, state);
    }

    @Override
    protected boolean hitSecondary(BlockHitResult hit, Vec3 lookVec, Vec3 eyePos)
    {
        Direction facing = getBlockState().getValue(FramedProperties.FACING_HOR);
        Direction dirTwo = getBlockState().getValue(FramedProperties.TOP) ? Direction.UP : Direction.DOWN;
        Direction side = hit.getDirection();
        Vec3 hitVec = hit.getLocation();

        if (side == dirTwo)
        {
            return false;
        }
        if (side == facing || side == facing.getCounterClockWise())
        {
            return Utils.fractionInDir(hitVec, dirTwo) < .5;
        }

        if (side == facing.getOpposite() || side == facing.getClockWise())
        {
            if (Utils.fractionInDir(hitVec, dirTwo) > .5) // Crosshair is definitely on slab's half-width faces
            {
                return false;
            }
        }
        else if (side == dirTwo.getOpposite())
        {
            double par = Utils.fractionInDir(hitVec, facing);
            double perp = Utils.fractionInDir(hitVec, facing.getClockWise());
            if (perp < par) // Crosshair is definitely on half slope's triangle face
            {
                return true;
            }
        }

        Triangle tri = TRIANGLES[facing.get2DDataValue()];
        Vec3 origin = eyePos.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return !tri.intersects(origin, lookVec.normalize());
    }
}

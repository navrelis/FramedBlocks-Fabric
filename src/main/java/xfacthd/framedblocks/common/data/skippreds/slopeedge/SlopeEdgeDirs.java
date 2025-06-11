package xfacthd.framedblocks.common.data.skippreds.slopeedge;

import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.property.CornerType;
import xfacthd.framedblocks.common.data.property.SlopeType;
import xfacthd.framedblocks.common.data.skippreds.CornerDir;
import xfacthd.framedblocks.common.data.skippreds.HalfDir;
import xfacthd.framedblocks.common.data.skippreds.QuarterTriangleDir;
import xfacthd.framedblocks.common.data.skippreds.TriangleDir;

public final class SlopeEdgeDirs
{
    public static final class SlopeEdge
    {
        public static QuarterTriangleDir getTriDir(Direction dir, SlopeType type, boolean alt, Direction side)
        {
            if (type == SlopeType.HORIZONTAL)
            {
                if (Utils.isY(side))
                {
                    return QuarterTriangleDir.fromDirections(dir, dir.getCounterClockWise(), alt);
                }
            }
            else
            {
                if (side.getAxis() == dir.getClockWise().getAxis())
                {
                    Direction dirTwo = type == SlopeType.TOP ? Direction.UP : Direction.DOWN;
                    return QuarterTriangleDir.fromDirections(dir, dirTwo, alt);
                }
            }
            return QuarterTriangleDir.NULL;
        }

        public static HalfDir getHalfDir(Direction dir, SlopeType type, boolean alt, Direction side)
        {
            if (alt)
            {
                return HalfDir.NULL;
            }
            if (type == SlopeType.HORIZONTAL)
            {
                if (side == dir)
                {
                    return HalfDir.fromDirections(side, dir.getCounterClockWise());
                }
                else if (side == dir.getCounterClockWise())
                {
                    return HalfDir.fromDirections(side, dir);
                }
            }
            else
            {
                Direction dirTwo = type == SlopeType.TOP ? Direction.UP : Direction.DOWN;
                if (side == dir)
                {
                    return HalfDir.fromDirections(side, dirTwo);
                }
                else if (side == dirTwo)
                {
                    return HalfDir.fromDirections(side, dir);
                }
            }
            return HalfDir.NULL;
        }

        private SlopeEdge() { }
    }

    public static final class ElevatedSlopeEdge
    {
        public static TriangleDir getTriDir(Direction dir, SlopeType type, Direction side)
        {
            if (type == SlopeType.HORIZONTAL)
            {
                if (Utils.isY(side))
                {
                    return TriangleDir.fromDirections(dir, dir.getCounterClockWise());
                }
            }
            else
            {
                if (side.getAxis() == dir.getClockWise().getAxis())
                {
                    Direction dirTwo = type == SlopeType.TOP ? Direction.UP : Direction.DOWN;
                    return TriangleDir.fromDirections(dir, dirTwo);
                }
            }
            return TriangleDir.NULL;
        }

        public static HalfDir getHalfDir(Direction dir, SlopeType type, Direction side)
        {
            if (type == SlopeType.HORIZONTAL)
            {
                if (side == dir.getOpposite())
                {
                    return HalfDir.fromDirections(side, dir.getCounterClockWise());
                }
                else if (side == dir.getClockWise())
                {
                    return HalfDir.fromDirections(side, dir);
                }
            }
            else
            {
                Direction dirTwo = type == SlopeType.TOP ? Direction.UP : Direction.DOWN;
                if (side == dir.getOpposite())
                {
                    return HalfDir.fromDirections(side, dirTwo);
                }
                else if (side == dirTwo.getOpposite())
                {
                    return HalfDir.fromDirections(side, dir);
                }
            }
            return HalfDir.NULL;
        }

        private ElevatedSlopeEdge() { }
    }

    public static final class CornerSlopeEdge
    {
        public static QuarterTriangleDir getTriDir(Direction dir, CornerType type, boolean alt, Direction side)
        {
            if (type.isHorizontal())
            {
                Direction backOne = type.isTop() ? Direction.UP : Direction.DOWN;
                Direction backTwo = type.isRight() ? dir.getClockWise() : dir.getCounterClockWise();
                if (side == backOne)
                {
                    return QuarterTriangleDir.fromDirections(dir, backTwo, alt);
                }
                if (side == backTwo)
                {
                    return QuarterTriangleDir.fromDirections(dir, backOne, alt);
                }
            }
            else
            {
                Direction bottom = type.isTop() ? Direction.UP : Direction.DOWN;
                if (side == dir)
                {
                    return QuarterTriangleDir.fromDirections(dir.getCounterClockWise(), bottom, alt);
                }
                else if (side == dir.getCounterClockWise())
                {
                    return QuarterTriangleDir.fromDirections(dir, bottom, alt);
                }
            }
            return QuarterTriangleDir.NULL;
        }

        public static CornerDir getCornerDir(Direction dir, CornerType type, boolean alt, Direction side)
        {
            if (!alt)
            {
                if (type.isHorizontal())
                {
                    if (side == dir)
                    {
                        return CornerDir.fromDirections(
                                dir,
                                type.isTop() ? Direction.UP : Direction.DOWN,
                                type.isRight() ? dir.getClockWise() : dir.getCounterClockWise()
                        );
                    }
                }
                else
                {
                    Direction bottom = type.isTop() ? Direction.UP : Direction.DOWN;
                    if (side == bottom)
                    {
                        return CornerDir.fromDirections(bottom, dir, dir.getCounterClockWise());
                    }
                }
            }
            return CornerDir.NULL;
        }

        private CornerSlopeEdge() { }
    }

    public static final class InnerCornerSlopeEdge
    {
        public static QuarterTriangleDir getTriDir(Direction dir, CornerType type, boolean alt, Direction side)
        {
            if (type.isHorizontal())
            {
                Direction frontOne =  type.isTop() ? Direction.DOWN : Direction.UP;
                Direction frontTwo =  type.isRight() ? dir.getCounterClockWise() : dir.getClockWise();
                if (side == frontOne)
                {
                    return QuarterTriangleDir.fromDirections(dir, frontTwo.getOpposite(), alt);
                }
                if (side == frontTwo)
                {
                    return QuarterTriangleDir.fromDirections(dir, frontOne.getOpposite(), alt);
                }
            }
            else
            {
                Direction bottom = type.isTop() ? Direction.UP : Direction.DOWN;
                if (side == dir.getOpposite())
                {
                    return QuarterTriangleDir.fromDirections(bottom, dir.getCounterClockWise(), alt);
                }
                if (side == dir.getClockWise())
                {
                    return QuarterTriangleDir.fromDirections(bottom, dir, alt);
                }
            }
            return QuarterTriangleDir.NULL;
        }

        public static HalfDir getHalfDir(Direction dir, CornerType type, boolean alt, Direction side)
        {
            if (!alt)
            {
                if (type.isHorizontal())
                {
                    Direction backOne =  type.isTop() ? Direction.UP : Direction.DOWN;
                    Direction backTwo =  type.isRight() ? dir.getClockWise() : dir.getCounterClockWise();
                    if (side == backOne || side == backTwo)
                    {
                        return HalfDir.fromDirections(side, dir);
                    }
                }
                else
                {
                    Direction bottom = type.isTop() ? Direction.UP : Direction.DOWN;
                    if (side == dir || side == dir.getCounterClockWise())
                    {
                        return HalfDir.fromDirections(side, bottom);
                    }
                }
            }
            return HalfDir.NULL;
        }

        public static TriangleDir getStairDir(Direction dir, CornerType type, boolean alt, Direction side)
        {
            if (!alt)
            {
                if (type.isHorizontal())
                {
                    if (side == dir)
                    {
                        Direction backOne =  type.isTop() ? Direction.UP : Direction.DOWN;
                        Direction backTwo =  type.isRight() ? dir.getClockWise() : dir.getCounterClockWise();
                        return TriangleDir.fromDirections(backOne, backTwo);
                    }
                }
                else
                {
                    Direction bottom = type.isTop() ? Direction.UP : Direction.DOWN;
                    if (side == bottom)
                    {
                        return TriangleDir.fromDirections(dir, dir.getCounterClockWise());
                    }
                }
            }
            return TriangleDir.NULL;
        }

        private InnerCornerSlopeEdge() { }
    }

    public static final class ElevatedCornerSlopeEdge
    {
        public static TriangleDir getTriDir(Direction dir, CornerType type, Direction side)
        {
            if (type.isHorizontal())
            {
                Direction yBack = type.isTop() ? Direction.UP : Direction.DOWN;
                Direction xBack = type.isRight() ? dir.getClockWise() : dir.getCounterClockWise();
                if (side == xBack)
                {
                    return TriangleDir.fromDirections(dir, yBack);
                }
                if (side == yBack)
                {
                    return TriangleDir.fromDirections(dir, xBack);
                }
            }
            else
            {
                if (side == dir)
                {
                    Direction bottom = type == CornerType.TOP ? Direction.UP : Direction.DOWN;
                    return TriangleDir.fromDirections(dir.getCounterClockWise(), bottom);
                }
                if (side == dir.getCounterClockWise())
                {
                    Direction bottom = type == CornerType.TOP ? Direction.UP : Direction.DOWN;
                    return TriangleDir.fromDirections(dir, bottom);
                }
            }
            return TriangleDir.NULL;
        }

        public static HalfDir getHalfDir(Direction dir, CornerType type, Direction side)
        {
            if (type.isHorizontal())
            {
                Direction yFront = type.isTop() ? Direction.DOWN : Direction.UP;
                Direction xFront = type.isRight() ? dir.getCounterClockWise() : dir.getClockWise();
                if (side == xFront || side == yFront)
                {
                    return HalfDir.fromDirections(side, dir);
                }
            }
            else
            {
                if (side == dir.getOpposite() || side == dir.getClockWise())
                {
                    Direction bottom = type == CornerType.TOP ? Direction.UP : Direction.DOWN;
                    return HalfDir.fromDirections(side, bottom);
                }
            }
            return HalfDir.NULL;
        }

        public static CornerDir getCornerDir(Direction dir, CornerType type, Direction side)
        {
            if (type.isHorizontal())
            {
                if (side == dir.getOpposite())
                {
                    return CornerDir.fromDirections(
                            side,
                            type.isTop() ? Direction.UP : Direction.DOWN,
                            type.isRight() ? dir.getClockWise() : dir.getCounterClockWise()
                    );
                }
            }
            else
            {
                Direction top = type == CornerType.TOP ? Direction.DOWN : Direction.UP;
                if (side == top)
                {
                    return CornerDir.fromDirections(side, dir, dir.getCounterClockWise());
                }
            }
            return CornerDir.NULL;
        }

        private ElevatedCornerSlopeEdge() { }
    }

    public static final class ElevatedInnerCornerSlopeEdge
    {
        public static TriangleDir getTriDir(Direction dir, CornerType type, Direction side)
        {
            if (type.isHorizontal())
            {
                Direction yBack = type.isTop() ? Direction.UP : Direction.DOWN;
                Direction xBack = type.isRight() ? dir.getClockWise() : dir.getCounterClockWise();
                if (side == xBack.getOpposite())
                {
                    return TriangleDir.fromDirections(dir, yBack);
                }
                if (side == yBack.getOpposite())
                {
                    return TriangleDir.fromDirections(dir, xBack);
                }
            }
            else
            {
                if (side == dir.getOpposite())
                {
                    Direction bottom = type == CornerType.TOP ? Direction.UP : Direction.DOWN;
                    return TriangleDir.fromDirections(bottom, dir.getCounterClockWise());
                }
                if (side == dir.getClockWise())
                {
                    Direction bottom = type == CornerType.TOP ? Direction.UP : Direction.DOWN;
                    return TriangleDir.fromDirections(bottom, dir);
                }
            }
            return TriangleDir.NULL;
        }

        public static TriangleDir getStairDir(Direction dir, CornerType type, Direction side)
        {
            if (type.isHorizontal())
            {
                if (side == dir.getOpposite())
                {
                    return TriangleDir.fromDirections(
                            type.isTop() ? Direction.UP : Direction.DOWN,
                            type.isRight() ? dir.getClockWise() : dir.getCounterClockWise()
                    );
                }
            }
            else
            {
                Direction top = type == CornerType.TOP ? Direction.DOWN : Direction.UP;
                if (side == top)
                {
                    return TriangleDir.fromDirections(dir, dir.getCounterClockWise());
                }
            }
            return TriangleDir.NULL;
        }

        private ElevatedInnerCornerSlopeEdge() { }
    }

    public static final class ThreewayCornerSlopeEdge
    {
        public static QuarterTriangleDir getTriDir(Direction dir, boolean top, boolean right, boolean alt, Direction side)
        {
            Direction bottom = top ? Direction.UP : Direction.DOWN;
            Direction dirTwo = right ? dir.getClockWise() : dir.getCounterClockWise();
            if (side == dir)
            {
                return QuarterTriangleDir.fromDirections(bottom, dirTwo, alt);
            }
            if (side == dirTwo)
            {
                return QuarterTriangleDir.fromDirections(bottom, dir, alt);
            }
            if (side == bottom)
            {
                return QuarterTriangleDir.fromDirections(dir, dirTwo, alt);
            }
            return QuarterTriangleDir.NULL;
        }

        private ThreewayCornerSlopeEdge() { }
    }

    public static final class InnerThreewayCornerSlopeEdge
    {
        public static QuarterTriangleDir getTriDir(Direction dir, boolean top, boolean right, boolean alt, Direction side)
        {
            Direction bottom = top ? Direction.UP : Direction.DOWN;
            Direction dirTwo = right ? dir.getClockWise() : dir.getCounterClockWise();
            if (side == dir.getOpposite())
            {
                return QuarterTriangleDir.fromDirections(bottom, dirTwo, alt);
            }
            if (side == dirTwo.getOpposite())
            {
                return QuarterTriangleDir.fromDirections(bottom, dir, alt);
            }
            if (side == bottom.getOpposite())
            {
                return QuarterTriangleDir.fromDirections(dir, dirTwo, alt);
            }
            return QuarterTriangleDir.NULL;
        }

        public static TriangleDir getStairDir(Direction dir, boolean top, boolean right, boolean alt, Direction side)
        {
            if (!alt)
            {
                Direction bottom = top ? Direction.UP : Direction.DOWN;
                Direction dirTwo = right ? dir.getClockWise() : dir.getCounterClockWise();
                if (side == dir)
                {
                    return TriangleDir.fromDirections(bottom, dirTwo);
                }
                if (side == dirTwo)
                {
                    return TriangleDir.fromDirections(bottom, dir);
                }
                if (side == bottom)
                {
                    return TriangleDir.fromDirections(dir, dirTwo);
                }
            }
            return TriangleDir.NULL;
        }

        private InnerThreewayCornerSlopeEdge() { }
    }
}

package xfacthd.framedblocks.common.data.skippreds.pane;

import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.block.pane.FramedPartialBoardBlock;
import xfacthd.framedblocks.common.data.property.CompoundDirection;
import xfacthd.framedblocks.common.data.property.SlopeType;
import xfacthd.framedblocks.common.data.skippreds.CornerDir;
import xfacthd.framedblocks.common.data.skippreds.HalfDir;
import xfacthd.framedblocks.common.data.skippreds.TriangleDir;

@SuppressWarnings("JavaExistingMethodCanBeUsed")
public final class PaneDirs
{
    public static final class FloorBoard
    {
        public static HalfDir getHalfDir(boolean top, Direction side)
        {
            if (!Utils.isY(side))
            {
                return HalfDir.fromDirections(side, top ? Direction.UP : Direction.DOWN);
            }
            return HalfDir.NULL;
        }

        private FloorBoard() { }
    }

    public static final class WallBoard
    {
        public static HalfDir getHalfDir(Direction dir, Direction side)
        {
            if (side.getAxis() != dir.getAxis())
            {
                return HalfDir.fromDirections(side, dir);
            }
            return HalfDir.NULL;
        }

        private WallBoard() { }
    }

    public static final class HalfBoard
    {
        public static HalfDir getEdgeDir(CompoundDirection cmpDir, Direction side)
        {
            if (side == cmpDir.orientation())
            {
                return HalfDir.fromDirections(side, cmpDir.direction());
            }
            return HalfDir.NULL;
        }

        public static CornerDir getHalfEdgeDir(CompoundDirection cmpDir, Direction side)
        {
            if (side.getAxis() != cmpDir.direction().getAxis() && side.getAxis() != cmpDir.orientation().getAxis())
            {
                return CornerDir.fromDirections(side, cmpDir.direction(), cmpDir.orientation());
            }
            return CornerDir.NULL;
        }

        public static HalfDir getHalfDir(CompoundDirection cmpDir, Direction side)
        {
            if (side == cmpDir.direction())
            {
                return HalfDir.fromDirections(side, cmpDir.orientation());
            }
            return HalfDir.NULL;
        }

        private HalfBoard() { }
    }

    public static final class CornerBoard
    {
        public static CornerDir getHalfEdgeDir(CompoundDirection cmpDir, Direction side)
        {
            Direction dirOne = cmpDir.orientation();
            Direction dirTwo = FramedPartialBoardBlock.getCornerDirTwo(cmpDir);
            if (side == dirOne)
            {
                return CornerDir.fromDirections(side, cmpDir.direction(), dirTwo);
            }
            if (side == dirTwo)
            {
                return CornerDir.fromDirections(side, cmpDir.direction(), dirOne);
            }
            return CornerDir.NULL;
        }

        public static CornerDir getCornerDir(CompoundDirection cmpDir, Direction side)
        {
            if (side == cmpDir.direction())
            {
                Direction dirOne = cmpDir.orientation();
                Direction dirTwo = FramedPartialBoardBlock.getCornerDirTwo(cmpDir);
                return CornerDir.fromDirections(side, dirOne, dirTwo);
            }
            return CornerDir.NULL;
        }

        private CornerBoard() { }
    }

    public static final class InnerCornerBoard
    {
        public static HalfDir getEdgeDir(CompoundDirection cmpDir, Direction side)
        {
            if (side == cmpDir.orientation() || side == FramedPartialBoardBlock.getCornerDirTwo(cmpDir))
            {
                return HalfDir.fromDirections(side, cmpDir.direction());
            }
            return HalfDir.NULL;
        }

        public static CornerDir getHalfEdgeDir(CompoundDirection cmpDir, Direction side)
        {
            Direction dirOne = cmpDir.orientation();
            Direction dirTwo = FramedPartialBoardBlock.getCornerDirTwo(cmpDir);
            if (side == dirOne.getOpposite())
            {
                return CornerDir.fromDirections(side, cmpDir.direction(), dirTwo);
            }
            if (side == dirTwo.getOpposite())
            {
                return CornerDir.fromDirections(side, cmpDir.direction(), dirOne);
            }
            return CornerDir.NULL;
        }

        public static TriangleDir getStairDir(CompoundDirection cmpDir, Direction side)
        {
            if (side == cmpDir.direction())
            {
                Direction dirOne = cmpDir.orientation();
                Direction dirTwo = FramedPartialBoardBlock.getCornerDirTwo(cmpDir);
                return TriangleDir.fromDirections(dirOne, dirTwo);
            }
            return TriangleDir.NULL;
        }

        private InnerCornerBoard() { }
    }

    public static final class CornerStrip
    {
        public static HalfDir getHalfDir(Direction dir, SlopeType type, Direction side)
        {
            Direction dirTwo = switch (type)
            {
                case TOP -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
                case HORIZONTAL -> dir.getCounterClockWise();
            };
            if (side == dir)
            {
                return HalfDir.fromDirections(side, dirTwo);
            }
            else if (side == dirTwo)
            {
                return HalfDir.fromDirections(side, dir);
            }
            return HalfDir.NULL;
        }

        public static CornerDir getCornerDir(Direction dir, SlopeType type, Direction side)
        {
            Direction dirTwo = switch (type)
            {
                case TOP -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
                case HORIZONTAL -> dir.getCounterClockWise();
            };
            if (side.getAxis() != dir.getAxis() && side.getAxis() != dirTwo.getAxis())
            {
                return CornerDir.fromDirections(side, dir, dirTwo);
            }
            return CornerDir.NULL;
        }

        private CornerStrip() { }
    }

    private PaneDirs() { }
}

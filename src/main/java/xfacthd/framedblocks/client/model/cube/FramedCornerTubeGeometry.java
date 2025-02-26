package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CornerTubeOrientation;

public class FramedCornerTubeGeometry extends Geometry
{
    private final CornerTubeOrientation orientation;
    private final float minWidth;
    private final float maxWidth;

    public FramedCornerTubeGeometry(GeometryFactory.Context ctx)
    {
        this.orientation = ctx.state().getValue(PropertyHolder.CORNER_TYPE_ORIENTATION);
        float thickness = ctx.state().getValue(PropertyHolder.THICK) ? 3F : 2F;
        this.minWidth = thickness / 16F;
        this.maxWidth = (16F - thickness) / 16F;
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();

        if (Utils.isY(quadDir))
        {
            if (orientation.isSideOpen(quadDir))
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.cutTopBottom(Direction.NORTH, minWidth))
                        .export(quadMap.get(quadDir));
                QuadModifier.of(quad)
                        .apply(Modifiers.cutTopBottom(Direction.SOUTH, minWidth))
                        .export(quadMap.get(quadDir));
                QuadModifier.of(quad)
                        .apply(Modifiers.cutTopBottom(Direction.EAST, minWidth))
                        .apply(Modifiers.cutTopBottom(Direction.Axis.Z, maxWidth))
                        .export(quadMap.get(quadDir));
                QuadModifier.of(quad)
                        .apply(Modifiers.cutTopBottom(Direction.WEST, minWidth))
                        .apply(Modifiers.cutTopBottom(Direction.Axis.Z, maxWidth))
                        .export(quadMap.get(quadDir));

                QuadModifier mod = QuadModifier.of(quad);
                for (Direction side : Direction.values())
                {
                    if (side.getAxis() != quadDir.getAxis() && !orientation.isSideOpen(side))
                    {
                        mod.apply(Modifiers.cutTopBottom(side, maxWidth));
                    }
                }
                mod.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
            }
            else if (orientation.isSideOpen(quadDir.getOpposite()))
            {
                QuadModifier mod = QuadModifier.of(quad);
                for (Direction side : Direction.values())
                {
                    if (side.getAxis() == quadDir.getAxis()) continue;

                    if (orientation.isSideOpen(side.getOpposite()))
                    {
                        mod.apply(Modifiers.cutTopBottom(side, minWidth));
                    }
                    else if (!orientation.isSideOpen(side))
                    {
                        mod.apply(Modifiers.cutTopBottom(side, maxWidth));
                    }
                }
                mod.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
            }
            else
            {
                QuadModifier modOne = QuadModifier.of(quad);
                QuadModifier modTwo = QuadModifier.of(quad);
                for (Direction side : Direction.values())
                {
                    if (side.getAxis() == quadDir.getAxis()) continue;

                    if (side != orientation.getPrimaryDir())
                    {
                        modOne.apply(Modifiers.cutTopBottom(side, maxWidth));
                    }
                    if (side != orientation.getSecondaryDir())
                    {
                        float len = side == orientation.getSecondaryDir().getOpposite() ? minWidth : maxWidth;
                        modTwo.apply(Modifiers.cutTopBottom(side, len));
                    }
                }
                modOne.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
                modTwo.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
            }
        }
        else
        {
            if (orientation.isSideOpen(quadDir))
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideUpDown(false, minWidth))
                        .export(quadMap.get(quadDir));
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideUpDown(true, minWidth))
                        .export(quadMap.get(quadDir));
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideLeftRight(quadDir.getClockWise(), minWidth))
                        .apply(Modifiers.cutSideUpDown(maxWidth))
                        .export(quadMap.get(quadDir));
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideLeftRight(quadDir.getCounterClockWise(), minWidth))
                        .apply(Modifiers.cutSideUpDown(maxWidth))
                        .export(quadMap.get(quadDir));

                QuadModifier mod = QuadModifier.of(quad);
                for (Direction side : Direction.values())
                {
                    if (side.getAxis() != quadDir.getAxis() && !orientation.isSideOpen(side))
                    {
                        mod.apply(Modifiers.cutSide(side, maxWidth, maxWidth));
                    }
                }
                mod.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
            }
            else if (orientation.isSideOpen(quadDir.getOpposite()))
            {
                QuadModifier mod = QuadModifier.of(quad);
                for (Direction side : Direction.values())
                {
                    if (side.getAxis() == quadDir.getAxis()) continue;

                    if (orientation.isSideOpen(side.getOpposite()))
                    {
                        mod.apply(Modifiers.cutSide(side, minWidth, minWidth));
                    }
                    else if (!orientation.isSideOpen(side))
                    {
                        mod.apply(Modifiers.cutSide(side, maxWidth, maxWidth));
                    }
                }
                mod.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
            }
            else
            {
                QuadModifier modOne = QuadModifier.of(quad);
                QuadModifier modTwo = QuadModifier.of(quad);
                for (Direction side : Direction.values())
                {
                    if (side.getAxis() == quadDir.getAxis()) continue;

                    if (side != orientation.getPrimaryDir())
                    {
                        modOne.apply(Modifiers.cutSide(side, maxWidth, maxWidth));
                    }
                    if (side != orientation.getSecondaryDir())
                    {
                        float len = side == orientation.getSecondaryDir().getOpposite() ? minWidth : maxWidth;
                        modTwo.apply(Modifiers.cutSide(side, len, len));
                    }
                }
                modOne.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
                modTwo.apply(Modifiers.setPosition(minWidth)).export(quadMap.get(null));
            }
        }
    }

    @Override
    public boolean transformAllQuads()
    {
        return true;
    }
}

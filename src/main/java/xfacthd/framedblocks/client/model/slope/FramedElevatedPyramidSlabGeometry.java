package xfacthd.framedblocks.client.model.slope;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.property.PillarConnection;

public class FramedElevatedPyramidSlabGeometry extends FramedPyramidGeometry
{
    public FramedElevatedPyramidSlabGeometry(GeometryFactory.Context ctx)
    {
        super(ctx);
    }

    @Override
    protected void buildBody(QuadMap quadMap, BakedQuad quad, Direction quadDir)
    {
        if (Utils.isY(facing))
        {
            boolean up = facing == Direction.UP;
            if (!ySlope && quadDir.getAxis() != facing.getAxis())
            {
                QuadModifier.of(quad)
                        .applyIf(Modifiers.cutSideUpDown(!up, slopeHeight), hasPillar)
                        .apply(Modifiers.cutSideUpDown(up, .5F))
                        .apply(Modifiers.cutSideLeftRight(false, up ? .5F : 1.5F, up ? 1.5F : .5F))
                        .apply(Modifiers.cutSideLeftRight(true, up ? .5F : 1.5F, up ? 1.5F : .5F))
                        .apply(Modifiers.makeVerticalSlope(up, 45))
                        .apply(Modifiers.offset(quadDir, .5F))
                        .export(quadMap.get(null));
            }
            else if (ySlope && quadDir == facing)
            {
                for (Direction dir : Direction.Plane.HORIZONTAL)
                {
                    boolean northeast = dir == Direction.NORTH || dir == Direction.EAST;
                    float angle = up ? -45 : 45;
                    if (northeast) { angle *= -1F; }
                    QuadModifier.of(quad)
                            .applyIf(Modifiers.cutTopBottom(dir, slopeHeight), hasPillar)
                            .apply(Modifiers.cutTopBottom(dir.getOpposite(), .5F))
                            .apply(Modifiers.cutTopBottom(dir.getCounterClockWise(), .5F, 1.5F))
                            .apply(Modifiers.cutTopBottom(dir.getClockWise(), 1.5F, .5F))
                            .apply(Modifiers.rotateCentered(dir.getClockWise().getAxis(), angle, true))
                            .apply(Modifiers.offset(facing.getOpposite(), .5F))
                            .export(quadMap.get(null));
                }
            }
            if (quadDir.getAxis() != facing.getAxis())
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideUpDown(!up, .5F))
                        .export(quadMap.get(quadDir));
            }
        }
        else
        {
            if (!ySlope && quadDir.getAxis() == facing.getAxis())
            {
                QuadModifier.of(quad)
                        .applyIf(Modifiers.cutSideUpDown(true, slopeHeight), hasPillar)
                        .apply(Modifiers.cutSideUpDown(false, .5F))
                        .apply(Modifiers.cutSideLeftRight(facing.getClockWise(), 1.5F, .5F))
                        .apply(Modifiers.cutSideLeftRight(facing.getCounterClockWise(), 1.5F, .5F))
                        .apply(Modifiers.makeVerticalSlope(true, 45))
                        .apply(Modifiers.offset(Direction.UP, .5F))
                        .export(quadMap.get(null));

                QuadModifier.of(quad)
                        .applyIf(Modifiers.cutSideUpDown(false, slopeHeight), hasPillar)
                        .apply(Modifiers.cutSideUpDown(true, .5F))
                        .apply(Modifiers.cutSideLeftRight(facing.getClockWise(), .5F, 1.5F))
                        .apply(Modifiers.cutSideLeftRight(facing.getCounterClockWise(), .5F, 1.5F))
                        .apply(Modifiers.makeVerticalSlope(false, 45))
                        .apply(Modifiers.offset(Direction.DOWN, .5F))
                        .export(quadMap.get(null));
            }
            else if (ySlope && Utils.isY(quadDir))
            {
                boolean up = quadDir == Direction.UP;

                float angle = up ? 45 : -45;
                if (facing == Direction.NORTH || facing == Direction.EAST)
                {
                    angle *= -1F;
                }

                Vector3f origin = facing.getOpposite().step().max(ZERO);
                if (up)
                {
                    origin.add(0, 1, 0);
                }

                QuadModifier.of(quad)
                        .applyIf(Modifiers.cutTopBottom(facing, slopeHeight), hasPillar)
                        .apply(Modifiers.cutTopBottom(facing.getOpposite(), .5F))
                        .apply(Modifiers.cutTopBottom(facing.getCounterClockWise(), .5F, 1.5F))
                        .apply(Modifiers.cutTopBottom(facing.getClockWise(), 1.5F, .5F))
                        .apply(Modifiers.rotate(facing.getClockWise().getAxis(), origin, angle, true))
                        .apply(Modifiers.offset(quadDir, .5F))
                        .export(quadMap.get(null));
            }
            else if (quadDir.getAxis() == facing.getClockWise().getAxis())
            {
                boolean right = quadDir == facing.getClockWise();
                QuadModifier.of(quad)
                        .applyIf(Modifiers.cutSideLeftRight(facing, slopeHeight), hasPillar)
                        .apply(Modifiers.cutSideLeftRight(facing.getOpposite(), .5F))
                        .apply(Modifiers.cutSideUpDown(true, right ? 1.5F : .5F, right ? .5F : 1.5F))
                        .apply(Modifiers.cutSideUpDown(false, right ? 1.5F : .5F, right ? .5F : 1.5F))
                        .apply(Modifiers.makeHorizontalSlope(!right, 45))
                        .apply(Modifiers.offset(quadDir, .5F))
                        .export(quadMap.get(null));
            }
            if (Utils.isY(quadDir))
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.cutTopBottom(facing, .5F))
                        .export(quadMap.get(quadDir));
            }
            else if (quadDir.getAxis() != facing.getAxis())
            {
                QuadModifier.of(quad)
                        .apply(Modifiers.cutSideLeftRight(facing, .5F))
                        .export(quadMap.get(quadDir));
            }
        }
    }

    @Override
    protected float computeSlopeHeight(PillarConnection pillar)
    {
        return switch (pillar)
        {
            case NONE -> 1F;
            case POST -> 14F/16F;
            case PILLAR -> 12F/16F;
        };
    }

    @Override
    protected float computePillarHeight(PillarConnection pillar)
    {
        return switch (pillar)
        {
            case NONE -> 0F;
            case POST -> 2F/16F;
            case PILLAR -> 4F/16F;
        };
    }
}

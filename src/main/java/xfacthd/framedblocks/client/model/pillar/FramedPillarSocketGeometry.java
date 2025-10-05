package xfacthd.framedblocks.client.model.pillar;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.util.Utils;

public class FramedPillarSocketGeometry extends Geometry
{
    private final Direction facing;

    public FramedPillarSocketGeometry(GeometryFactory.Context ctx)
    {
        this.facing = ctx.state().getValue(BlockStateProperties.FACING);
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        Direction.Axis quadAxis = quadDir.getAxis();

        if (quadDir == facing.getOpposite())
        {
            boolean y = Utils.isY(quadDir);
            QuadModifier.of(quad)
                    .apply(y ? Modifiers.cutTopBottom(.25F, .25F, .75F, .75F) : Modifiers.cutSide(.25F, .25F, .75F, .75F))
                    .export(quadMap.get(quadDir));

            Utils.forAllDirections(false, dir ->
            {
                if (dir.getAxis() == facing.getAxis()) return;

                boolean perp = y ? Utils.isZ(dir) : Utils.isY(dir);
                QuadModifier.of(quad)
                        .apply(Modifiers.cut(dir, .25F))
                        .applyIf(Modifiers.cut(dir.getClockWise(quadAxis), .75F), perp)
                        .applyIf(Modifiers.cut(dir.getCounterClockWise(quadAxis), .75F), perp)
                        .apply(Modifiers.setPosition(.5F))
                        .export(quadMap.get(null));
            });
        }
        else if (quadDir != facing)
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(facing.getOpposite(), .5F))
                    .export(quadMap.get(quadDir));

            QuadModifier.of(quad)
                    .apply(Modifiers.cut(facing, .5F))
                    .apply(Modifiers.cut(facing.getClockWise(quadAxis), .75F))
                    .apply(Modifiers.cut(facing.getCounterClockWise(quadAxis), .75F))
                    .apply(Modifiers.setPosition(.75F))
                    .export(quadMap.get(null));
        }
    }
}

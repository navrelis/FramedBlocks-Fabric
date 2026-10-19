package xfacthd.framedblocks.client.model.interactive;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;

/**
 * One half of the flag of the Framed Banner, rendered by the
 * {@link xfacthd.framedblocks.client.render.block.FramedBannerRenderer}. The camo cube is cut down to a 1/16 thick
 * slab whose front faces the given direction, moved down by one (top half) or two (bottom half) blocks and finally
 * rotated back into the south-facing orientation, the renderer applies the actual orientation of the banner
 */
public final class FramedBannerFlagGeometry extends Geometry
{
    private static final float DEPTH = 1F / 16F;

    private final Direction dir;
    private final boolean top;

    public FramedBannerFlagGeometry(GeometryFactory.Context ctx, boolean top)
    {
        this.dir = ctx.state().getValue(FramedProperties.FACING_HOR);
        this.top = top;
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if ((top && quadDir == Direction.DOWN) || (!top && quadDir == Direction.UP))
        {
            return;
        }

        QuadModifier modifier = QuadModifier.of(quad);
        if (quadDir == dir)
        {
            modifier.apply(Modifiers.setPosition(DEPTH));
        }
        else if (quadDir != dir.getOpposite())
        {
            modifier.apply(Modifiers.cut(dir, DEPTH));
        }

        modifier.apply(Modifiers.offset(Direction.DOWN, top ? 1F : 2F))
                .apply(Modifiers.rotateCentered(Direction.Axis.Y, dir.toYRot(), false))
                .export(quadMap.get(null));
    }

    @Override
    public boolean useSolidNoCamoModel()
    {
        return true;
    }
}

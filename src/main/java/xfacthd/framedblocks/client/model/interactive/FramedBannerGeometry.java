package xfacthd.framedblocks.client.model.interactive;

import net.minecraft.client.renderer.block.model.BakedQuad;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;

/**
 * Static block model of the standing and wall banner. The banner is drawn entirely by the
 * {@link xfacthd.framedblocks.client.render.block.FramedBannerRenderer}: the pole and bar are vanilla banner
 * model parts and the camo flag sways, so the static model produces no quads
 */
public final class FramedBannerGeometry extends Geometry
{
    @SuppressWarnings("unused")
    public FramedBannerGeometry(GeometryFactory.Context ctx) { }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad) { }
}

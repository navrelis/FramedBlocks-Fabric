package xfacthd.framedblocks.client.model.rect;

import net.minecraft.client.renderer.block.model.BakedQuad;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.common.data.shapes.rect.*;

import java.util.List;

/**
 * Geometry for the window frame, table, chair, stool, corbel and wall shelf. The model consists of a union of boxes
 * which are the same boxes that make up the collision and selection shapes
 */
public class FramedRectGeometry extends Geometry
{
    private final List<PxBox> boxes;

    private FramedRectGeometry(List<PxBox> boxes)
    {
        this.boxes = boxes;
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        PxBoxQuads.emit(quadMap, quad, boxes);
    }

    public static FramedRectGeometry windowFrame(GeometryFactory.Context ctx)
    {
        return new FramedRectGeometry(WindowFrameShapes.boxes(ctx.state()));
    }

    public static FramedRectGeometry table(GeometryFactory.Context ctx)
    {
        return new FramedRectGeometry(TableShapes.boxes(ctx.state()));
    }

    public static FramedRectGeometry chair(GeometryFactory.Context ctx)
    {
        return new FramedRectGeometry(SeatShapes.chair(ctx.state().getValue(FramedProperties.FACING_HOR)));
    }

    public static FramedRectGeometry stool(GeometryFactory.Context ctx)
    {
        return new FramedRectGeometry(SeatShapes.stool());
    }

    public static FramedRectGeometry corbel(GeometryFactory.Context ctx)
    {
        return new FramedRectGeometry(CorbelShapes.boxes(
                ctx.state().getValue(FramedProperties.FACING_HOR),
                ctx.state().getValue(FramedProperties.TOP)
        ));
    }

    public static FramedRectGeometry wallShelf(GeometryFactory.Context ctx)
    {
        return new FramedRectGeometry(WallShelfShapes.boxes(
                ctx.state().getValue(FramedProperties.FACING_HOR),
                ctx.state().getValue(FramedProperties.TOP)
        ));
    }
}

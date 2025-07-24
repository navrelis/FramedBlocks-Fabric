package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.util.Utils;

public class FramedMiniCubeGeometry extends Geometry
{
    private static final Vector3f ORIGIN_BOTTOM = new Vector3f(.5F, 0, .5F);
    private static final Vector3f ORIGIN_TOP = new Vector3f(.5F, 1, .5F);

    private final float rotAngle;
    private final Direction bottomFace;
    private final Vector3f origin;

    public FramedMiniCubeGeometry(GeometryFactory.Context ctx)
    {
        int rot = ctx.state().getValue(BlockStateProperties.ROTATION_16);
        this.rotAngle = (4 - (rot % 4)) * 22.5F;
        boolean top = ctx.state().getValue(FramedProperties.TOP);
        this.bottomFace = top ? Direction.UP : Direction.DOWN;
        this.origin = top ? ORIGIN_TOP : ORIGIN_BOTTOM;
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        QuadModifier.of(quad)
                .apply(Modifiers.scaleFace(.5F, origin))
                .applyIf(Modifiers.setPosition(.5F), quadDir == bottomFace.getOpposite())
                .applyIf(Modifiers.setPosition(.75F), !Utils.isY(quadDir))
                .apply(Modifiers.rotate(Direction.Axis.Y, origin, rotAngle, false))
                .export(quadMap.get(quadDir == bottomFace ? quadDir : null));
    }
}

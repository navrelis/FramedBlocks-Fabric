package xfacthd.framedblocks.client.model.cube;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.model.data.QuadMap;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.quad.Modifiers;
import xfacthd.framedblocks.api.model.quad.QuadModifier;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.model.wrapping.itemmodel.ItemModelInfo;
import xfacthd.framedblocks.api.model.wrapping.itemmodel.TranslatedItemModelInfo;

public class FramedLayeredCubeGeometry extends Geometry
{
    private static final ItemModelInfo ITEM_MODEL_INFO = TranslatedItemModelInfo.hand(0F, .8F, 0F);
    private static final float LAYER_HEIGHT = 1F/8F;

    private final Direction facing;
    private final float height;

    public FramedLayeredCubeGeometry(GeometryFactory.Context ctx)
    {
        this.facing = ctx.state().getValue(BlockStateProperties.FACING);
        this.height = ctx.state().getValue(BlockStateProperties.LAYERS) * LAYER_HEIGHT;
    }

    @Override
    public void transformQuad(QuadMap quadMap, BakedQuad quad)
    {
        Direction quadDir = quad.getDirection();
        if (quadDir == facing)
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.setPosition(height))
                    .export(quadMap.get(null));
        }
        else
        {
            QuadModifier.of(quad)
                    .apply(Modifiers.cut(facing, height))
                    .export(quadMap.get(quadDir));
        }
    }

    @Override
    public boolean useBaseModel()
    {
        return true;
    }

    @Override
    public ItemModelInfo getItemModelInfo()
    {
        return ITEM_MODEL_INFO;
    }
}

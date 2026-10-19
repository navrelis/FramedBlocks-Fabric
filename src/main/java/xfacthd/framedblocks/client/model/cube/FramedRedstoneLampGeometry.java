package xfacthd.framedblocks.client.model.cube;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;

public class FramedRedstoneLampGeometry extends FramedGlowingCubeGeometry
{
    public FramedRedstoneLampGeometry(GeometryFactory.Context ctx)
    {
        super(ctx, ctx.state().getValue(BlockStateProperties.LIT));
    }
}

package xfacthd.framedblocks.common.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.common.data.property.PillarConnection;

public interface IPillarLikeBlock
{
    PillarConnection getPillarConnection(BlockState state, Direction side);
}

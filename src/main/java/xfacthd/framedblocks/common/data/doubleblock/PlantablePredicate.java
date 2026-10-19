package xfacthd.framedblocks.common.data.doubleblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.platform.util.TriState;

public interface PlantablePredicate
{
    TriState test(FramedDoubleBlockEntity be, BlockGetter level, Direction side, BlockState plant);
}

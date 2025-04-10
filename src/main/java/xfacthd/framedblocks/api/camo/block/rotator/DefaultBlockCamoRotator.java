package xfacthd.framedblocks.api.camo.block.rotator;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.util.Utils;

final class DefaultBlockCamoRotator implements BlockCamoRotator
{
    @Override
    public boolean canRotate(BlockState state)
    {
        return Utils.getRotatableProperty(state) != null;
    }

    @Override
    @Nullable
    public BlockState rotate(BlockState state)
    {
        Property<?> prop = Utils.getRotatableProperty(state);
        return prop != null ? state.cycle(prop) : null;
    }
}

package xfacthd.framedblocks.api.camo.block.rotator;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class SimpleBlockCamoRotator implements BlockCamoRotator
{
    private final Property<?> property;

    public SimpleBlockCamoRotator(Property<?> property)
    {
        this.property = property;
    }

    @Override
    public boolean canRotate(BlockState state)
    {
        return true;
    }

    @Override
    public BlockState rotate(BlockState state)
    {
        return state.cycle(property);
    }
}

package xfacthd.framedblocks.common.data.blueprint;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import xfacthd.framedblocks.api.blueprint.BlueprintCopyBehaviour;
import xfacthd.framedblocks.common.data.PropertyHolder;

import java.util.List;

public final class TubeCopyBehaviour implements BlueprintCopyBehaviour
{
    @Override
    public List<Property<?>> getPropertiesToCopy(BlockState state)
    {
        return List.of(PropertyHolder.THICK);
    }
}

package xfacthd.framedblocks.api.camo.block;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.api.camo.block.rotator.BlockCamoRotator;

public abstract class AbstractBlockCamoContainer<T extends AbstractBlockCamoContainer<T>> extends CamoContainer<BlockCamoContent, T>
{
    protected AbstractBlockCamoContainer(BlockState state)
    {
        super(new BlockCamoContent(state));
    }

    public final BlockState getState()
    {
        return content.getState();
    }

    @Override
    public boolean canRotateCamo()
    {
        BlockState state = content.getState();
        return BlockCamoRotator.of(state.getBlock()).canRotate(state);
    }

    @Override
    @Nullable
    public T rotateCamo()
    {
        BlockState state = content.getState();
        BlockState newState = BlockCamoRotator.of(state.getBlock()).rotate(state);
        return newState != null ? copyWithState(newState) : null;
    }

    /**
     * {@return a copy of this camo container with the camo state replaced by the given state}
     * To be used when a mod does a similar action to {@link #rotateCamo()} through external means
     * such as custom item interactions.
     */
    @SuppressWarnings("unchecked")
    public final T copyWithState(BlockState state)
    {
        return getFactory().copyContainerWithState((T) this, state);
    }

    @Override
    public abstract AbstractBlockCamoContainerFactory<T> getFactory();
}

package xfacthd.framedblocks.platform.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public final class ModelDataHelper
{
    /**
     * {@return the {@link ModelData} provided as render data by the block entity at the given position or
     * {@link ModelData#EMPTY} if there is none} Safe to be called from chunk meshing threads
     */
    public static ModelData get(BlockGetter level, BlockPos pos)
    {
        if (level.getBlockEntityRenderData(pos) instanceof ModelData data)
        {
            return data;
        }
        return ModelData.EMPTY;
    }



    private ModelDataHelper() { }
}

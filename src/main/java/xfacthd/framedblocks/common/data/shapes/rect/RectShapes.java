package xfacthd.framedblocks.common.data.shapes.rect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import xfacthd.framedblocks.api.shapes.ShapeProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared helper to build a {@link ShapeProvider} from a state-dependent list of {@link PxBox}es
 */
final class RectShapes
{
    static ShapeProvider generate(ImmutableList<BlockState> states, Function<BlockState, List<PxBox>> boxFactory)
    {
        Map<List<PxBox>, VoxelShape> cache = new HashMap<>();
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
        for (BlockState state : states)
        {
            builder.put(state, cache.computeIfAbsent(boxFactory.apply(state), PxBox::toShape));
        }
        return ShapeProvider.of(builder.build());
    }



    private RectShapes() { }
}

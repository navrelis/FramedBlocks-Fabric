package xfacthd.framedblocks.gametest.cmdtests.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import xfacthd.framedblocks.client.modelwrapping.ModelWrappingHandler;
import xfacthd.framedblocks.client.modelwrapping.ModelWrappingManager;

import java.util.stream.Collectors;

public final class ModelStateInfo
{
    public static int getVisitedStateCount(Block block)
    {
        return ModelWrappingManager.getHandler(block).getVisitedStateCount();
    }

    @SuppressWarnings("deprecation")
    public static String printIgnoredProperties(Block block)
    {
        ModelWrappingHandler wrapper = ModelWrappingManager.getHandler(block);
        return wrapper.getStateMerger()
                .getHandledProperties(block.builtInRegistryHolder())
                .stream()
                .filter(block.defaultBlockState()::hasProperty)
                .map(Property::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }



    private ModelStateInfo() { }
}

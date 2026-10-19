package xfacthd.framedblocks.gametest.cmdtests.client;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Block;
import xfacthd.framedblocks.api.util.ClientUtils;

import java.util.List;

/**
 * Client-only part of the state info dump, the properties which the models ignore are only known to the client
 */
public final class ModelStateInfo
{
    private static final List<Property<?>> IGNORED_PROPERTIES = ClientUtils.IGNORE_DEFAULT_LOCK;

    public static int getVisitedStateCount(Block block, int stateCount)
    {
        for (Property<?> prop : IGNORED_PROPERTIES)
        {
            if (block.defaultBlockState().hasProperty(prop))
            {
                stateCount /= prop.getPossibleValues().size();
            }
        }
        return stateCount;
    }



    private ModelStateInfo() { }
}

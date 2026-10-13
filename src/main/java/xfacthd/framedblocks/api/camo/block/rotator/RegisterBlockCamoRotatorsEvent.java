package xfacthd.framedblocks.api.camo.block.rotator;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;

public final class RegisterBlockCamoRotatorsEvent
{
    private final BiConsumer<Block, BlockCamoRotator> registrar;

    @ApiStatus.Internal
    public RegisterBlockCamoRotatorsEvent(BiConsumer<Block, BlockCamoRotator> registrar)
    {
        this.registrar = registrar;
    }

    public void register(Block block, BlockCamoRotator rotator)
    {
        registrar.accept(block, rotator);
    }
}

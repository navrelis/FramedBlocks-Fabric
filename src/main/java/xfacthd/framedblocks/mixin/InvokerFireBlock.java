package xfacthd.framedblocks.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface InvokerFireBlock
{
    @Invoker("getBurnOdds")
    int framedblocks$callGetBurnOdds(BlockState state);

    @Invoker("getIgniteOdds")
    int framedblocks$callGetIgniteOdds(BlockState state);
}

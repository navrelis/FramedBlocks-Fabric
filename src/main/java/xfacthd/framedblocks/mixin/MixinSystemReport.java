package xfacthd.framedblocks.mixin;

import net.minecraft.SystemReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.common.util.ServerConfig;

@Mixin(SystemReport.class)
public abstract class MixinSystemReport
{
    @Shadow
    public abstract void setDetail(String name, String value);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void framedblocks$addBlockEntityWarning(CallbackInfo ci)
    {
        if (FramedBlocks.isInitialized() && ServerConfig.allowBlockEntities)
        {
            setDetail("FramedBlocks BlockEntity Warning", FramedBlocks.getBlockEntityWarning());
        }
    }
}

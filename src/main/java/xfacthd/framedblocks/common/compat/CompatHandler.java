package xfacthd.framedblocks.common.compat;

import xfacthd.framedblocks.common.compat.emi.EmiCompat;
import xfacthd.framedblocks.common.compat.jei.JeiCompat;
import xfacthd.framedblocks.common.compat.rei.ReiCompat;
import xfacthd.framedblocks.common.compat.supplementaries.SupplementariesCompat;

public final class CompatHandler
{
    public static void init()
    {
        // TODO(E3): CreateCompat.init();
        EmiCompat.init();
        // TODO(E3): FlywheelCompat.init();
        JeiCompat.init();
        ReiCompat.init();
        SupplementariesCompat.init();
    }

    public static void commonSetup()
    {
        // TODO(E3): CreateCompat.commonSetup();
    }



    private CompatHandler() { }
}

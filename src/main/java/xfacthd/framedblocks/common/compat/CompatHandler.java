package xfacthd.framedblocks.common.compat;

import xfacthd.framedblocks.common.compat.create.CreateCompat;
import xfacthd.framedblocks.common.compat.emi.EmiCompat;
import xfacthd.framedblocks.common.compat.flywheel.FlywheelCompat;
import xfacthd.framedblocks.common.compat.jei.JeiCompat;
import xfacthd.framedblocks.common.compat.rei.ReiCompat;
import xfacthd.framedblocks.common.compat.supplementaries.SupplementariesCompat;

public final class CompatHandler
{
    public static void init()
    {
        CreateCompat.init();
        EmiCompat.init();
        FlywheelCompat.init();
        JeiCompat.init();
        ReiCompat.init();
        SupplementariesCompat.init();
    }

    public static void commonSetup()
    {
        CreateCompat.commonSetup();
    }



    private CompatHandler() { }
}

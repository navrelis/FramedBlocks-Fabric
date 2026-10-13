package xfacthd.framedblocks.common.compat;

import xfacthd.framedblocks.common.compat.amendments.AmendmentsCompat;
import xfacthd.framedblocks.common.compat.atlasviewer.AtlasViewerCompat;
import xfacthd.framedblocks.common.compat.diagonalblocks.DiagonalBlocksCompat;
import xfacthd.framedblocks.common.compat.searchables.SearchablesCompat;

public final class CompatHandler
{
    public static void init()
    {
        AmendmentsCompat.init();
        AtlasViewerCompat.init();
        DiagonalBlocksCompat.init();
        SearchablesCompat.init();
    }

    public static void commonSetup()
    {
        DiagonalBlocksCompat.commonSetup();
    }



    private CompatHandler() { }
}

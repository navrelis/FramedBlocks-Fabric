package xfacthd.framedblocks.gametest.selftest.client;

import net.minecraft.client.Minecraft;
import xfacthd.framedblocks.gametest.selftest.SelfTest;

public final class SelfTestClient
{
    public static void onClientStarted(@SuppressWarnings("unused") Minecraft minecraft)
    {
        SelfTest.runCommonSelfTest((reporter, blocks) ->
        {
            ClientBlockEffectsPresence.checkClientEffectsPresent(reporter, blocks);
            SpecialShapeRendererPresence.checkSpecialShapePresent(reporter, blocks);
        });
    }



    private SelfTestClient() { }
}

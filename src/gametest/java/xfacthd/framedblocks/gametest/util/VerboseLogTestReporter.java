package xfacthd.framedblocks.gametest.util;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.LogTestReporter;

/**
 * Logs the stack trace of failed game tests in addition to the message logged by the vanilla reporter
 */
public class VerboseLogTestReporter extends LogTestReporter
{
    @Override
    public void onTestFailed(GameTestInfo info)
    {
        super.onTestFailed(info);

        if (info.getError() != null)
        {
            info.getError().printStackTrace();
        }
    }
}

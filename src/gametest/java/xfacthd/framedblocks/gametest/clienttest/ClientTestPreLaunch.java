package xfacthd.framedblocks.gametest.clienttest;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Attaches the log capture as early as possible so that warnings emitted during mod loading are recorded too
 */
public final class ClientTestPreLaunch implements PreLaunchEntrypoint
{
    @Override
    public void onPreLaunch()
    {
        if (ClientTestConfig.isEnabled())
        {
            LogCapture.attach();
        }
    }
}

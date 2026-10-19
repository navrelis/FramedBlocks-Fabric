package xfacthd.framedblocks.gametest.util;

public interface TestRunnable extends Runnable
{
    default int getDuration()
    {
        return 1;
    }
}

package xfacthd.framedblocks.gametest.util;

public record TestDelay(int delay) implements TestRunnable
{
    @Override
    public void run() { }

    @Override
    public int getDuration()
    {
        return delay;
    }
}

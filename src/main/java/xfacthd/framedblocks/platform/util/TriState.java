package xfacthd.framedblocks.platform.util;

public enum TriState
{
    TRUE,
    DEFAULT,
    FALSE;

    public boolean isTrue()
    {
        return this == TRUE;
    }

    public boolean isFalse()
    {
        return this == FALSE;
    }

    public boolean isDefault()
    {
        return this == DEFAULT;
    }

    public net.fabricmc.fabric.api.util.TriState toFabric()
    {
        return switch (this)
        {
            case TRUE -> net.fabricmc.fabric.api.util.TriState.TRUE;
            case DEFAULT -> net.fabricmc.fabric.api.util.TriState.DEFAULT;
            case FALSE -> net.fabricmc.fabric.api.util.TriState.FALSE;
        };
    }

    public static TriState of(net.fabricmc.fabric.api.util.TriState state)
    {
        return switch (state)
        {
            case TRUE -> TRUE;
            case DEFAULT -> DEFAULT;
            case FALSE -> FALSE;
        };
    }
}

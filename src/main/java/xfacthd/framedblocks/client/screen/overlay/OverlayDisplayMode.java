package xfacthd.framedblocks.client.screen.overlay;

public enum OverlayDisplayMode
{
    HIDDEN,
    ICON,
    DETAILED;

    public OverlayDisplayMode constrain(OverlayDisplayMode other)
    {
        return ordinal() < other.ordinal() ? this : other;
    }
}

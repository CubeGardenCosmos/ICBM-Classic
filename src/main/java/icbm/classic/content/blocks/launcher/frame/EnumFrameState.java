package icbm.classic.content.blocks.launcher.frame;

import net.minecraft.util.StringRepresentable;

/** The vertical position of a launcher frame in a connected launch column. */
public enum EnumFrameState implements StringRepresentable
{
    TOP,
    MIDDLE,
    BOTTOM;

    @Override
    public String getSerializedName()
    {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

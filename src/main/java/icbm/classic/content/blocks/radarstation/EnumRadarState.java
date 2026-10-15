package icbm.classic.content.blocks.radarstation;

import net.minecraft.util.StringRepresentable;

public enum EnumRadarState implements StringRepresentable
{
    ON, OFF, WARNING, DANGER;

    public static EnumRadarState get(int index)
    {
        return index >= 0 && index < values().length ? values()[index] : OFF;
    }

    @Override
    public String getSerializedName()
    {
        return name().toLowerCase();
    }
}

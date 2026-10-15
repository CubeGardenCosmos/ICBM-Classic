package icbm.classic.content.blocks.radarstation.data;

public enum RadarDotType
{
    MARKER, DETECTED, HOSTILE, INCOMING;

    public static RadarDotType get(int index)
    {
        return index >= 0 && index < values().length ? values()[index] : DETECTED;
    }
}

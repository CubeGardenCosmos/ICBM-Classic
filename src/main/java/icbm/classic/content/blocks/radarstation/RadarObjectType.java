package icbm.classic.content.blocks.radarstation;

public enum RadarObjectType
{
    THREAT, THREAT_IMPACT, OTHER;

    public static RadarObjectType get(int index)
    {
        return switch (index)
        {
            case 0 -> THREAT;
            case 1 -> THREAT_IMPACT;
            default -> OTHER;
        };
    }
}

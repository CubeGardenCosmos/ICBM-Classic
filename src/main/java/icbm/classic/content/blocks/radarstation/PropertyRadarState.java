package icbm.classic.content.blocks.radarstation;

import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class PropertyRadarState
{
    public static final EnumProperty<EnumRadarState> RADAR_STATE = EnumProperty.create("type", EnumRadarState.class);

    private PropertyRadarState() {}
}

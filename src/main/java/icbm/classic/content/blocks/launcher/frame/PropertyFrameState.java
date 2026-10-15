package icbm.classic.content.blocks.launcher.frame;

import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Block-state property for the rendered vertical frame segment. */
public final class PropertyFrameState
{
    public static final EnumProperty<EnumFrameState> FRAME_STATE = EnumProperty.create("type", EnumFrameState.class);

    private PropertyFrameState() {}
}

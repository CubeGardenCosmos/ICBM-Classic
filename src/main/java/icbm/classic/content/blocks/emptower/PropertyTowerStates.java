package icbm.classic.content.blocks.emptower;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** The 1.12 tower variants, represented as a 26.3 block-state property. */
public final class PropertyTowerStates
{
    public static final EnumProperty<EnumTowerTypes> TOWER_MODELS = EnumProperty.create("type", EnumTowerTypes.class);

    private PropertyTowerStates() {}

    public enum EnumTowerTypes implements StringRepresentable
    {
        BASE, COIL, ELECTRIC, SPIN;

        @Override
        public String getSerializedName()
        {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}

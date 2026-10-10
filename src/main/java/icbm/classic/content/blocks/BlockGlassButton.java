package icbm.classic.content.blocks;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Glass button. 1.12 used {@code super(false)} (the stone button behaviour, not activated by arrows) and
 * overrode the click sounds to the stone button clicks; {@link BlockSetType#STONE} already carries that
 * behaviour, so only the glass break/step sound has to come from the properties.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class BlockGlassButton extends ButtonBlock
{
    public BlockGlassButton(BlockBehaviour.Properties properties)
    {
        // 1.12: super(false) + tickRate 20 (stone button)
        super(BlockSetType.STONE, 20, properties);
    }
}

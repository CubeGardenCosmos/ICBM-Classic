package icbm.classic.content.blocks;

import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Glass pressure plate. 1.12 used {@code Sensitivity.EVERYTHING} (dropped items trigger it, not only
 * living entities). 26.3 moved the sensitivity into {@link BlockSetType}; the wood types are the
 * "everything" ones, so a wood block set is used with a glass sound in the properties.
 * <p>
 * Difference to 1.12: the plate click feedback follows the block set type and is therefore the wood click
 * in 26.3 (1.12 played the stone plate click for every pressure plate). Frequency and triggering behaviour
 * are unchanged.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class BlockGlassPressurePlate extends PressurePlateBlock
{
    public BlockGlassPressurePlate(BlockBehaviour.Properties properties)
    {
        // 1.12: super(Material.GLASS, Sensitivity.EVERYTHING)
        super(BlockSetType.OAK, properties);
    }
}

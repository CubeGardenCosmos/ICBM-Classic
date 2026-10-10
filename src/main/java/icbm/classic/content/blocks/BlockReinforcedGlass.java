package icbm.classic.content.blocks;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Reinforced glass. 1.12 was a {@code Block} with {@code Material.GLASS}, {@code isOpaqueCube=false} and
 * {@code isFullCube=false}; 26.3 expresses that with {@link TransparentBlock} plus {@code noOcclusion}
 * (the caller sets it on the properties so the id can be baked in first).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class BlockReinforcedGlass extends TransparentBlock
{
    public BlockReinforcedGlass(BlockBehaviour.Properties properties)
    {
        super(properties);
    }
}

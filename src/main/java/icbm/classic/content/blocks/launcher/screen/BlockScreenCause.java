package icbm.classic.content.blocks.launcher.screen;

import icbm.classic.ICBMConstants;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Action cause describing a missile launch initiated by a launcher screen. */
public class BlockScreenCause extends CausedByBlock
{
    public static final Identifier REG_NAME = ICBMConstants.id("block.screen");

    public BlockScreenCause() {}

    public BlockScreenCause(Level level, BlockPos pos, BlockState state)
    {
        super(level, pos, state);
    }

    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }
}

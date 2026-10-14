package icbm.classic.lib.explosive.reg;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.BlockActivateFunction;
import icbm.classic.api.data.WorldPosIntSupplier;
import icbm.classic.api.data.WorldTickFunction;
import icbm.classic.api.reg.content.IExBlockRegistry;
import icbm.classic.content.reg.ModBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Content type for explosive blocks. Holds the per explosive fuse time, fuse tick effect and block
 * activation callback.
 * <p>
 * 1.12 kept a second set of maps keyed by the numeric explosive id; with numeric ids gone (ADR 0002)
 * the name keyed maps are the only ones.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
@Deprecated
public class ExBlockContentReg extends ExplosiveContentRegistry implements IExBlockRegistry
{
    private final Map<Identifier, WorldPosIntSupplier> fuseSetSupplier = new HashMap<>();
    private final Map<Identifier, WorldTickFunction> fuseTickCallback = new HashMap<>();
    private final Map<Identifier, BlockActivateFunction> blockActivationCallback = new HashMap<>();

    public ExBlockContentReg()
    {
        super(ICBMClassicAPI.EX_BLOCK);
    }

    @Override
    public ItemStack getDeviceStack(Identifier regName)
    {
        // 1.12: new ItemStack(BlockReg.blockExplosive, 1, ICBMClassicHelpers.getExplosive(regName, false).getRegistryID())
        // 26.3 items are flat (ADR 0002), so the block of the explosive is the device stack. 1.12 answered the
        // item for every metadata value because one block held them all; the explosives without a block form
        // (rot, mutation) have no device stack any more.
        final Block block = ModBlocks.EXPLOSIVE_BLOCKS.get(regName);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    @Override
    public void setFuseSupplier(Identifier exName, WorldPosIntSupplier fuseTimer)
    {
        fuseSetSupplier.put(exName, fuseTimer);
    }

    @Override
    public void setFuseTickListener(Identifier exName, WorldTickFunction function)
    {
        fuseTickCallback.put(exName, function);
    }

    @Override
    public void setActivationListener(Identifier exName, BlockActivateFunction function)
    {
        blockActivationCallback.put(exName, function);
    }

    /** See {@link IExBlockRegistry#getActivationListener(Identifier)}, read by {@code ExplosiveBlock}. */
    @Nullable
    @Override
    public BlockActivateFunction getActivationListener(Identifier exName)
    {
        return blockActivationCallback.get(exName);
    }

    @Override
    public void tickFuse(Level world, double posX, double posY, double posZ, int ticksExisted, Identifier explosiveName)
    {
        final WorldTickFunction function = fuseTickCallback.get(explosiveName);
        if (function != null)
        {
            function.onTick(world, posX, posY, posZ, ticksExisted);
        }
    }

    @Override
    public int getFuseTime(Level world, double posX, double posY, double posZ, Identifier explosiveName)
    {
        final WorldPosIntSupplier function = fuseSetSupplier.get(explosiveName);
        if (function != null)
        {
            return function.get(world, posX, posY, posZ);
        }
        return 100;
    }
}

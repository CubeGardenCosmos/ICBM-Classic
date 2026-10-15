package icbm.classic.lib.energy;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.base.SimpleEnergyItem;

/** A portable energy store backed by Team Reborn Energy's per-stack component. */
public final class BatteryItem extends Item implements SimpleEnergyItem
{
    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INSERT = 10_000L;
    public static final long MAX_EXTRACT = 1_000_000L;

    public BatteryItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack)
    {
        return CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack)
    {
        return MAX_INSERT;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack)
    {
        return MAX_EXTRACT;
    }

    public static team.reborn.energy.api.EnergyStorage createStorage(ContainerItemContext context)
    {
        return SimpleEnergyItem.createStorage(context, CAPACITY, MAX_INSERT, MAX_EXTRACT);
    }
}

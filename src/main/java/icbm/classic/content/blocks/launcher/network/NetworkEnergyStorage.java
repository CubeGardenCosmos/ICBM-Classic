package icbm.classic.content.blocks.launcher.network;

import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.lib.energy.IcbmEnergyBuffer;

/** Internal network-level allocation over launcher buffers; S6.03 adds the external energy API. */
public final class NetworkEnergyStorage
{
    private final LauncherNetwork network;

    public NetworkEnergyStorage(LauncherNetwork network)
    {
        this.network = network;
    }

    public long receive(long amount, boolean simulate)
    {
        if (amount <= 0)
        {
            return 0;
        }
        int remainingBuffers = 0;
        for (LauncherNode node : network.getComponents())
        {
            if (node.getSelf() instanceof TileLauncherBase)
            {
                remainingBuffers++;
            }
        }

        long received = 0;
        for (LauncherNode node : network.getComponents())
        {
            if (node.getSelf() instanceof TileLauncherBase base)
            {
                final long remaining = amount - received;
                final long share = Math.min(remaining, remaining / remainingBuffers + 1);
                received += base.energyStorage.receive(share, simulate);
                remainingBuffers--;
                if (received >= amount)
                {
                    break;
                }
            }
        }
        return received;
    }

    public long getStored()
    {
        long stored = 0;
        for (LauncherNode node : network.getComponents())
        {
            if (node.getSelf() instanceof TileLauncherBase base)
            {
                stored += base.energyStorage.getStored();
            }
        }
        return stored;
    }

    public long getCapacity()
    {
        long capacity = 0;
        for (LauncherNode node : network.getComponents())
        {
            if (node.getSelf() instanceof TileLauncherBase base)
            {
                capacity += base.energyStorage.getCapacity();
            }
        }
        return capacity;
    }
}

package icbm.classic.lib.energy;

import icbm.classic.config.ConfigMain;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.Container;
import team.reborn.energy.api.EnergyStorage;

/** Moves energy from one inventory slot into a machine's internal buffer. */
public final class EnergyItemSlotHelper
{
    private EnergyItemSlotHelper() {}

    public static long dischargeInto(Container container, int slotIndex, IcbmEnergyBuffer buffer)
    {
        if (!ConfigMain.REQUIRES_POWER || slotIndex < 0 || slotIndex >= container.getContainerSize()) return 0;

        final long space = buffer.getCapacity() - buffer.getStored();
        final long acceptable = buffer.receive(space, true);
        if (acceptable <= 0) return 0;

        final var slotStorage = ContainerStorage.of(container, null).getSlot(slotIndex);
        final EnergyStorage itemStorage = EnergyStorage.ITEM.find(
                container.getItem(slotIndex), ContainerItemContext.ofSingleSlot(slotStorage));
        if (itemStorage == null) return 0;

        try (Transaction transaction = Transaction.openOuter())
        {
            final long extracted = itemStorage.extract(acceptable, transaction);
            if (extracted <= 0) return 0;
            final long accepted = buffer.receive(extracted, false);
            if (accepted <= 0) return 0;
            transaction.commit();
            return accepted;
        }
    }
}

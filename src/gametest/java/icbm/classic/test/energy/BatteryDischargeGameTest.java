package icbm.classic.test.energy;

import icbm.classic.config.ConfigMain;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import icbm.classic.prefab.tile.BlockICBM;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.base.SimpleEnergyItem;

public class BatteryDischargeGameTest
{
    @GameTest(maxTicks = 20)
    public void batteryDischargesIntoMachineOnlyWhenPowerIsRequired(GameTestHelper helper)
    {
        final boolean previousRequiresPower = ConfigMain.REQUIRES_POWER;
        try
        {
            ConfigMain.REQUIRES_POWER = false;
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.LAUNCHER_BASE.defaultBlockState()
                    .setValue(BlockICBM.ROTATION_PROP, Direction.UP));
            final TileLauncherBase machine = (TileLauncherBase) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            helper.assertTrue(machine != null, "launcher base block entity must be present");
            machine.energyStorage.setStored(0);
            final long storedBeforeNoPower = machine.energyStorage.getStored();

            final ItemStack battery = new ItemStack(ModItems.BATTERY);
            SimpleEnergyItem.setStoredEnergyUnchecked(battery, 5_000L);
            machine.inventory.setItem(1, battery);

            machine.tickPower();
            helper.assertTrue(machine.energyStorage.getStored() == storedBeforeNoPower,
                    "machine buffer must be unchanged when REQUIRES_POWER is false");
            helper.assertTrue(SimpleEnergyItem.getStoredEnergyUnchecked(battery) == 5_000L,
                    "battery energy must be unchanged when REQUIRES_POWER is false");

            ConfigMain.REQUIRES_POWER = true;
            machine.tickPower();
            helper.assertTrue(machine.energyStorage.getStored() > 0,
                    "machine buffer must increase after battery discharge");
            helper.assertTrue(SimpleEnergyItem.getStoredEnergyUnchecked(battery) < 5_000L,
                    "battery must lose energy after discharge");
            helper.assertTrue(machine.energyStorage.getStored()
                            + SimpleEnergyItem.getStoredEnergyUnchecked(battery) == 5_000L,
                    "discharged energy must be conserved");
            helper.succeed();
        }
        finally
        {
            ConfigMain.REQUIRES_POWER = previousRequiresPower;
        }
    }
}

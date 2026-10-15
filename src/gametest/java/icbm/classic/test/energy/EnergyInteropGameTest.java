package icbm.classic.test.energy;

import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import icbm.classic.prefab.tile.BlockICBM;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyItem;

public class EnergyInteropGameTest {
    @GameTest(maxTicks = 20)
    public void externalEnergyAndBatteryPowerMachine(GameTestHelper helper) {
        final boolean previousRequiresPower = ConfigMain.REQUIRES_POWER;
        final int previousCapacity = ConfigLauncher.POWER_CAPACITY;
        ConfigMain.REQUIRES_POWER = true;
        ConfigLauncher.POWER_CAPACITY = 20_000;
        try {
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.LAUNCHER_BASE.defaultBlockState()
                    .setValue(BlockICBM.ROTATION_PROP, Direction.UP));
            final BlockPos absolutePos = helper.absolutePos(pos);
            final TileLauncherBase machine = (TileLauncherBase) helper.getLevel().getBlockEntity(absolutePos);
            helper.assertTrue(machine != null, "launcher base block entity must be present");
            machine.energyStorage.setStored(0);

            final EnergyStorage exposed = EnergyStorage.SIDED.find(helper.getLevel(), absolutePos, Direction.UP);
            helper.assertTrue(exposed != null, "machine must expose sided energy storage");
            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(exposed.insert(500, transaction) == 500,
                        "external storage lookup must accept 500 FE");
                transaction.commit();
            }
            helper.assertTrue(machine.energyStorage.getStored() == 500,
                    "external insertion must increase the machine buffer by 500 FE");
            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(exposed.extract(500, transaction) == 0,
                        "machine exposed storage must reject extraction");
            }

            final ItemStack battery = new ItemStack(ModItems.BATTERY);
            SimpleEnergyItem.setStoredEnergyUnchecked(battery, 5_000L);
            machine.inventory.setItem(1, battery);
            machine.tickPower();
            final long batteryStored = SimpleEnergyItem.getStoredEnergyUnchecked(battery);
            helper.assertTrue(machine.energyStorage.getStored() > 500,
                    "machine buffer must increase after battery-slot discharge");
            helper.assertTrue(batteryStored < 5_000L,
                    "battery must lose energy after battery-slot discharge");
            helper.assertTrue(machine.energyStorage.getStored() + batteryStored == 5_500L,
                    "energy from external insertion and battery discharge must be conserved");
            helper.succeed();
        } finally {
            ConfigMain.REQUIRES_POWER = previousRequiresPower;
            ConfigLauncher.POWER_CAPACITY = previousCapacity;
        }
    }
}

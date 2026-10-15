package icbm.classic.test.energy;

import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.reg.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import team.reborn.energy.api.EnergyStorage;

public class LauncherNetworkEnergyGameTest {
    @GameTest(maxTicks = 20)
    public void connectorDistributesEnergyAcrossLauncherNetwork(GameTestHelper helper) {
        final int previousCapacity = ConfigLauncher.POWER_CAPACITY;
        ConfigLauncher.POWER_CAPACITY = 1_200;
        try {
            final BlockPos connectorPos = new BlockPos(1, 1, 1);
            helper.setBlock(connectorPos, ModBlocks.LAUNCHER_CONNECTOR);
            helper.setBlock(connectorPos.east(), ModBlocks.LAUNCHER_FRAME);
            helper.setBlock(connectorPos.east(2), ModBlocks.LAUNCHER_BASE.defaultBlockState()
                    .setValue(icbm.classic.prefab.tile.BlockICBM.ROTATION_PROP, Direction.UP));
            helper.setBlock(connectorPos.east(3), ModBlocks.LAUNCHER_FRAME);
            helper.setBlock(connectorPos.east(4), ModBlocks.LAUNCHER_BASE.defaultBlockState()
                    .setValue(icbm.classic.prefab.tile.BlockICBM.ROTATION_PROP, Direction.UP));
            helper.setBlock(connectorPos.east(5), ModBlocks.LAUNCHER_FRAME);
            helper.setBlock(connectorPos.east(6), ModBlocks.LAUNCHER_BASE.defaultBlockState()
                    .setValue(icbm.classic.prefab.tile.BlockICBM.ROTATION_PROP, Direction.UP));

            final BlockPos absoluteConnector = helper.absolutePos(connectorPos);
            final EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), absoluteConnector, Direction.UP);
            helper.assertTrue(storage != null, "connector must expose its launcher network energy storage");
            helper.assertTrue(storage.getCapacity() == 3_600, "network capacity must aggregate three 1200 FE launcher bases");

            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(storage.insert(3_000, transaction) == 3_000, "network must accept 3000 FE");
                transaction.commit();
            }

            for (int x : new int[]{2, 4, 6}) {
                final TileLauncherBase base = (TileLauncherBase) helper.getLevel().getBlockEntity(
                        helper.absolutePos(connectorPos.east(x)));
                helper.assertTrue(base != null, "launcher base block entity must exist");
                final long stored = base.energyStorage.getStored();
                helper.assertTrue(Math.abs(stored - 1_000) <= 1,
                        "each launcher base must receive about 1000 FE, received " + stored);
            }

            try (Transaction transaction = Transaction.openOuter()) {
                helper.assertTrue(storage.insert(3_000, transaction) == 600,
                        "insert must return only the 600 FE remaining network capacity");
                transaction.commit();
            }
            helper.assertTrue(storage.getAmount() == storage.getCapacity(), "network must be full after the capacity-limited insert");
            helper.succeed();
        } finally {
            ConfigLauncher.POWER_CAPACITY = previousCapacity;
        }
    }
}

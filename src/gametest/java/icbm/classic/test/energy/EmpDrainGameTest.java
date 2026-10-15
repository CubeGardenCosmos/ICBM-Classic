package icbm.classic.test.energy;

import icbm.classic.config.ConfigMain;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.reg.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public class EmpDrainGameTest {
    @GameTest(maxTicks = 20, padding = 8)
    public void activationDrainsMachineBufferAndWorksWithoutPower(GameTestHelper helper) {
        final boolean previousRequiresPower = ConfigMain.REQUIRES_POWER;
        try {
            ConfigMain.REQUIRES_POWER = true;
            final BlockPos localTowerPos = new BlockPos(2, 1, 2);
            final BlockPos localMachinePos = localTowerPos.east();
            helper.setBlock(localTowerPos, ModBlocks.EMP_TOWER);
            helper.setBlock(localMachinePos, ModBlocks.LAUNCHER_BASE);
            final BlockPos towerPos = helper.absolutePos(localTowerPos);
            final BlockPos machinePos = helper.absolutePos(localMachinePos);
            final TileEMPTower tower = (TileEMPTower) helper.getLevel().getBlockEntity(towerPos);
            final TileLauncherBase machine = (TileLauncherBase) helper.getLevel().getBlockEntity(machinePos);
            helper.assertTrue(tower != null, "EMP tower block entity must be present");
            helper.assertTrue(machine != null, "launcher base block entity must be present");
            tower.setRange(3);
            tower.energyStorage.setStored(tower.energyStorage.getCapacity());
            machine.energyStorage.setStored(500);

            helper.assertTrue(tower.fire(null), "powered EMP tower activation must succeed");
            helper.assertTrue(machine.energyStorage.getStored() == 0,
                    "EMP activation must directly drain the insert-only machine buffer");

            ConfigMain.REQUIRES_POWER = false;
            final BlockPos freeTowerLocal = new BlockPos(8, 1, 2);
            helper.setBlock(freeTowerLocal, ModBlocks.EMP_TOWER);
            final TileEMPTower freeTower = (TileEMPTower) helper.getLevel().getBlockEntity(helper.absolutePos(freeTowerLocal));
            helper.assertTrue(freeTower != null, "second EMP tower block entity must be present");
            helper.assertTrue(freeTower.fire(null), "EMP activation must still succeed when power is disabled");
            helper.succeed();
        } finally {
            ConfigMain.REQUIRES_POWER = previousRequiresPower;
        }
    }
}

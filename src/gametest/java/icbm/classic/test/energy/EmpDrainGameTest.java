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

    /**
     * Guards the spherical filter of the drain scan: machines inside the sphere lose energy, machines outside
     * (laterally as well as vertically) keep it. The vertical machine also pins the 3D shape of the radius.
     */
    @GameTest(maxTicks = 20, padding = 8)
    public void activationDrainsOnlyMachinesInsideRadius(GameTestHelper helper) {
        final boolean previousRequiresPower = ConfigMain.REQUIRES_POWER;
        try {
            ConfigMain.REQUIRES_POWER = true;
            final BlockPos localTowerPos = new BlockPos(2, 1, 2);
            final BlockPos localInsidePos = new BlockPos(3, 1, 2);
            final BlockPos localOutsideLateralPos = new BlockPos(7, 1, 2);
            final BlockPos localOutsideVerticalPos = new BlockPos(3, 5, 2);
            helper.setBlock(localTowerPos, ModBlocks.EMP_TOWER);
            helper.setBlock(localInsidePos, ModBlocks.LAUNCHER_BASE);
            helper.setBlock(localOutsideLateralPos, ModBlocks.LAUNCHER_BASE);
            helper.setBlock(localOutsideVerticalPos, ModBlocks.LAUNCHER_BASE);
            final TileEMPTower tower = (TileEMPTower) helper.getLevel().getBlockEntity(helper.absolutePos(localTowerPos));
            final TileLauncherBase inside = (TileLauncherBase) helper.getLevel().getBlockEntity(helper.absolutePos(localInsidePos));
            final TileLauncherBase outsideLateral = (TileLauncherBase) helper.getLevel().getBlockEntity(helper.absolutePos(localOutsideLateralPos));
            final TileLauncherBase outsideVertical = (TileLauncherBase) helper.getLevel().getBlockEntity(helper.absolutePos(localOutsideVerticalPos));
            helper.assertTrue(tower != null, "EMP tower block entity must be present");
            helper.assertTrue(inside != null && outsideLateral != null && outsideVertical != null,
                    "all launcher base block entities must be present");
            tower.setRange(3);
            tower.energyStorage.setStored(tower.energyStorage.getCapacity());
            inside.energyStorage.setStored(500);
            outsideLateral.energyStorage.setStored(500);
            outsideVertical.energyStorage.setStored(500);

            helper.assertTrue(tower.fire(null), "charged EMP tower activation must succeed");
            helper.assertTrue(inside.energyStorage.getStored() == 0,
                    "machine inside the drain radius must be drained");
            helper.assertTrue(outsideLateral.energyStorage.getStored() == 500,
                    "machine outside the drain radius must keep its energy");
            helper.assertTrue(outsideVertical.energyStorage.getStored() == 500,
                    "machine above the drain radius must keep its energy");
            helper.succeed();
        } finally {
            ConfigMain.REQUIRES_POWER = previousRequiresPower;
        }
    }
}
